/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 * http://www.apache.org/licenses/LICENSE-2.0                               *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ****************************************************************************/

package com.gitlab.ykrasik.gamedex.app.api.util

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.reflect.KProperty

/**
 * User: ykrasik
 * Date: 02/04/2018
 * Time: 11:15
 */
interface BroadcastReceiveChannel<out T> {
    fun subscribe(): ReceiveChannel<T>

    fun subscribe(context: CoroutineContext = Dispatchers.Default, f: suspend (T) -> Unit): ReceiveChannel<T>

    fun peek(): T? = subscribe().let { subscription ->
        subscription.poll().apply { subscription.cancel() }
    }

    operator fun getValue(thisRef: Any, property: KProperty<*>) = peek()!!

    fun <R> map(context: CoroutineContext = Dispatchers.Default, transform: suspend (T) -> R): BroadcastReceiveChannel<R>

    fun <R> flatMap(context: CoroutineContext = Dispatchers.Default, transform: suspend (T) -> ReceiveChannel<R>): BroadcastReceiveChannel<R>
}

class BroadcastEventChannel<T>(capacity: Int = 32) : BroadcastReceiveChannel<T>, CoroutineScope {
    private val job = Job()
    override val coroutineContext = Dispatchers.Default + job

    private val channel = BroadcastChannel<T>(capacity)

    override fun subscribe() = channel.openSubscription()

    override fun subscribe(context: CoroutineContext, f: suspend (T) -> Unit): ReceiveChannel<T> {
        val subscription = subscribe()
        launch(context) {
            subscription.consumeEach {
                f(it)
            }
        }
        return subscription
    }

    suspend fun send(element: T) = channel.send(element)
    fun offer(element: T) = channel.offer(element)

    fun close() {
        channel.close()
        job.cancel()
    }

    override fun <R> map(context: CoroutineContext, transform: suspend (T) -> R): BroadcastEventChannel<R> {
        val channel = BroadcastEventChannel.conflated<R>()
        subscribe(context) {
            channel.send(transform(it))
        }
        return channel
    }

    override fun <R> flatMap(context: CoroutineContext, transform: suspend (T) -> ReceiveChannel<R>): BroadcastEventChannel<R> {
        val channel = BroadcastEventChannel.conflated<R>()
        var flatMapped: ReceiveChannel<R>? = null
        subscribe(context) {
            flatMapped?.cancel()
            flatMapped = transform(it)
            launch(context) {
                flatMapped!!.consumeEach {
                    channel.send(it)
                }
            }
        }
        return channel
    }

    inline fun filter(context: CoroutineContext = Dispatchers.Default, crossinline filter: suspend (T) -> Boolean): BroadcastEventChannel<T> {
        val channel = BroadcastEventChannel.conflated<T>()
        subscribe(context) {
            if (filter(it)) {
                channel.send(it)
            }
        }
        return channel
    }

    fun distinctUntilChanged(context: CoroutineContext = Dispatchers.Default): BroadcastEventChannel<T> {
        var last: T? = null
        return filter(context) {
            val keep = it != last
            last = it
            keep
        }
    }

    fun drop(amount: Int, context: CoroutineContext = Dispatchers.Default): BroadcastEventChannel<T> {
        val channel = BroadcastEventChannel.conflated<T>()
        var dropped = 0
        subscribe(context) {
            dropped += 1
            if (dropped > amount) {
                channel.send(it)
            }
        }
        return channel
    }

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        offer(value)
    }

    companion object {
        fun <T> conflated(): BroadcastEventChannel<T> = BroadcastEventChannel(Channel.CONFLATED)
        fun <T> conflated(initial: T): BroadcastEventChannel<T> = conflated<T>().apply { offer(initial) }
        inline fun <T> conflated(initial: () -> T): BroadcastEventChannel<T> = conflated(initial())
    }
}

fun <T> ReceiveChannel<T>.bind(channel: SendChannel<T>, context: CoroutineContext = Dispatchers.Default) = GlobalScope.launch(context) {
    consumeEach {
        channel.send(it)
    }
}

fun <T> channel(): Channel<T> = Channel(capacity = 32)

fun <T> conflatedChannel(): ConflatedChannel<T> = Channel<T>(capacity = Channel.CONFLATED) as ConflatedChannel<T>

// FIXME: Get rid of this.
operator fun <T> ConflatedChannel<T>.getValue(thisRef: Any, property: KProperty<*>) = poll()!!
operator fun <T> ConflatedChannel<T>.setValue(thisRef: Any, property: KProperty<*>, value: T) {
    offer(value)
}

fun <A, B> ReceiveChannel<A>.combineLatest(
    other: ReceiveChannel<B>,
    context: CoroutineContext = Dispatchers.Default
): ReceiveChannel<Pair<A, B>> {
    val channel = Channel<Pair<A, B>>(capacity = 0)
    val sourceA: ReceiveChannel<A> = this@combineLatest
    val sourceB: ReceiveChannel<B> = other

    var latestA: A? = null
    var latestB: B? = null

    GlobalScope.launch(context) {
        sourceA.consumeEach { a ->
            latestA = a
            if (latestA != null && latestB != null) {
                channel.send(latestA!! to latestB!!)
            }
        }
    }

    GlobalScope.launch(context) {
        sourceB.consumeEach { b ->
            latestB = b
            if (latestA != null && latestB != null) {
                channel.send(latestA!! to latestB!!)
            }
        }
    }

    return channel
}

fun <A, B> BroadcastReceiveChannel<A>.combineLatest(
    other: BroadcastReceiveChannel<B>,
    context: CoroutineContext = Dispatchers.Default
): BroadcastEventChannel<Pair<A, B>> {
    val channel = BroadcastEventChannel.conflated<Pair<A, B>>()
    val sourceA: ReceiveChannel<A> = this@combineLatest.subscribe()
    val sourceB: ReceiveChannel<B> = other.subscribe()

    var latestA: A? = null
    var latestB: B? = null

    GlobalScope.launch(context) {
        sourceA.consumeEach { a ->
            latestA = a
            if (latestA != null && latestB != null) {
                channel.send(latestA!! to latestB!!)
            }
        }
    }

    GlobalScope.launch(context) {
        sourceB.consumeEach { b ->
            latestB = b
            if (latestA != null && latestB != null) {
                channel.send(latestA!! to latestB!!)
            }
        }
    }

    return channel
}

fun <T> ReceiveChannel<T>.distinctUntilChanged(context: CoroutineContext = Dispatchers.Default): ReceiveChannel<T> {
    var last: T? = null
    return filter(context) {
        val keep = it != last
        last = it
        keep
    }
}