/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.SelectClause1
import kotlinx.coroutines.selects.whileSelect
import java.io.Closeable
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KProperty

/**
 * User: ykrasik
 * Date: 02/04/2018
 * Time: 11:15
 */
interface MultiReceiveChannel<T> : Closeable {
    fun subscribe(): ReceiveChannel<T>

    fun subscribe(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        f: suspend (T) -> Unit
    ): ReceiveChannel<T>

    fun peek(): T = subscribe().consume { poll()!! }

    operator fun getValue(thisRef: Any, property: KProperty<*>) = peek()

    fun <R> map(transform: suspend (T) -> R): MultiReceiveChannel<R>

    fun <R> flatMap(transform: suspend (T) -> ReceiveChannel<R>): MultiReceiveChannel<R>

    fun <R> combineLatest(other: MultiReceiveChannel<R>): MultiReceiveChannel<Pair<T, R>>

    fun filter(filter: suspend (T) -> Boolean): MultiReceiveChannel<T>

    fun distinctUntilChanged(equals: (T, T) -> Boolean = { t1, t2 -> t1 == t2 }): MultiReceiveChannel<T>

    fun drop(amount: Int): MultiReceiveChannel<T>
}

val <T> MultiChannel<T>.onReceive: SelectClause1<T> get() = subscribe().onReceive

class MultiChannel<T>(
    private val capacity: Int = 32,
    override val coroutineContext: CoroutineContext = Dispatchers.Default + Job()
) : MultiReceiveChannel<T>, CoroutineScope {

    private val channel = BroadcastChannel<T>(capacity)

    override fun subscribe() = channel.openSubscription()

    override fun subscribe(context: CoroutineContext, start: CoroutineStart, f: suspend (T) -> Unit): ReceiveChannel<T> {
        val channel = subscribe()
        launch(context, start) {
            channel.consumeEach {
                f(it)
            }
        }
        return channel
    }

    suspend fun send(element: T) = channel.send(element)
    fun offer(element: T) = channel.offer(element)

    override fun close() {
        channel.close()
        coroutineContext.cancel()
    }

    private fun <R> newChannel() = MultiChannel<R>(capacity, coroutineContext)

    override fun <R> map(transform: suspend (T) -> R): MultiReceiveChannel<R> {
        val channel = newChannel<R>()
        subscribe {
            channel.send(transform(it))
        }
        return channel
    }

    override fun <R> flatMap(transform: suspend (T) -> ReceiveChannel<R>): MultiReceiveChannel<R> {
        val channel = newChannel<R>()
        var flatMapped: ReceiveChannel<R>? = null
        subscribe {
            flatMapped?.cancel()
            flatMapped = transform(it)
            launch {
                flatMapped!!.consumeEach {
                    channel.send(it)
                }
            }
        }
        return channel
    }

    override fun <R> combineLatest(other: MultiReceiveChannel<R>): MultiReceiveChannel<Pair<T, R>> {
        val channel = MultiChannel.conflated<Pair<T, R>>()

        var latestA: T? = null
        var latestB: R? = null

        this.subscribe { a ->
            latestA = a
            if (latestA != null && latestB != null) {
                channel.send(latestA!! to latestB!!)
            }
        }

        other.subscribe { b ->
            latestB = b
            if (latestA != null && latestB != null) {
                channel.send(latestA!! to latestB!!)
            }
        }

        return channel
    }

    override fun filter(filter: suspend (T) -> Boolean): MultiReceiveChannel<T> {
        val channel = newChannel<T>()
        subscribe {
            if (filter(it)) {
                channel.send(it)
            }
        }
        return channel
    }

    override fun distinctUntilChanged(equals: (T, T) -> Boolean): MultiReceiveChannel<T> {
        var last: T? = null
        return filter { value ->
            val lastValue = last
            val keep = lastValue == null || !equals(value, lastValue)
            last = value
            keep
        }
    }

    override fun drop(amount: Int): MultiReceiveChannel<T> {
        val channel = newChannel<T>()
        var dropped = 0
        subscribe {
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
        fun <T> conflated(): MultiChannel<T> = MultiChannel(Channel.CONFLATED)
        fun <T> conflated(initial: T): MultiChannel<T> = conflated<T>().apply { offer(initial) }
        inline fun <T> conflated(initial: () -> T): MultiChannel<T> = conflated(initial())
    }
}

fun <T> ReceiveChannel<T>.bind(channel: SendChannel<T>, context: CoroutineContext = Dispatchers.Default) = GlobalScope.launch(context) {
    consumeEach {
        channel.send(it)
    }
}

fun <T> channel(): MultiChannel<T> = MultiChannel(capacity = 32)
fun <T> conflatedChannel(initial: T) = Channel<T>(Channel.CONFLATED).apply { offer(initial) }

fun <A, B> ReceiveChannel<A>.combineLatest(
    other: ReceiveChannel<B>,
    context: CoroutineContext = Dispatchers.Default
): ReceiveChannel<Pair<A, B>> {
    val channel = Channel<Pair<A, B>>(capacity = 0)
    val sourceA: ReceiveChannel<A> = this@combineLatest
    val sourceB: ReceiveChannel<B> = other

    var latestA: A? = null
    var latestB: B? = null

    GlobalScope.launch(context, CoroutineStart.UNDISPATCHED) {
        sourceA.consumeEach { a ->
            latestA = a
            if (latestA != null && latestB != null) {
                channel.send(latestA!! to latestB!!)
            }
        }
    }

    GlobalScope.launch(context, CoroutineStart.UNDISPATCHED) {
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

fun <T> ReceiveChannel<T>.bufferUntilTimeout(millis: Long = 200): ReceiveChannel<List<T>> = Channel<List<T>>(capacity = 32).also { channel ->
    GlobalScope.launch {
        var buffer = emptyList<T>()
        whileSelect {
            onTimeout(millis) {
                channel.offer(buffer)
                buffer = emptyList()
                buffer += receive()
                true
            }
            onReceive {
                buffer += it
                true
            }
        }
    }
}

fun <T> ReceiveChannel<T>.debounce(millis: Long = 200, scope: CoroutineScope = GlobalScope): ReceiveChannel<T> = Channel<T>(Channel.CONFLATED).also { channel ->
    scope.launch {
        var value = receive()
        whileSelect {
            onTimeout(millis) {
                channel.offer(value)
                value = receive()
                true
            }
            onReceive {
                value = it
                true
            }
        }
    }
}