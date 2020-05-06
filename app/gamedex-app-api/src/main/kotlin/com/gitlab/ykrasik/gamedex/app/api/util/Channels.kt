/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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
interface MultiReadChannel<T> : Closeable {
    fun subscribe(): ReceiveChannel<T>

    fun subscribe(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.UNDISPATCHED,
        f: suspend (T) -> Unit
    ): ReceiveChannel<T>

    fun <R> map(transform: suspend (T) -> R): MultiReadChannel<R>
    fun <R> mapTo(channel: MultiWriteChannel<R>, transform: suspend (T) -> R): ReceiveChannel<T>

    fun <R> flatMap(transform: suspend (T) -> ReceiveChannel<R>): MultiReadChannel<R>

    fun <T2, R> combineLatest(channel: MultiReadChannel<T2>, f: suspend (T, T2) -> R): MultiReadChannel<R>
    fun <T2, T3, R> combineLatest(channel2: MultiReadChannel<T2>, channel3: MultiReadChannel<T3>, f: suspend (T, T2, T3) -> R): MultiReadChannel<R>
    fun <T2, T3, T4, R> combineLatest(
        channel2: MultiReadChannel<T2>,
        channel3: MultiReadChannel<T3>,
        channel4: MultiReadChannel<T4>,
        f: suspend (T, T2, T3, T4) -> R
    ): MultiReadChannel<R>

    fun <T2, T3, T4, T5, R> combineLatest(
        channel2: MultiReadChannel<T2>,
        channel3: MultiReadChannel<T3>,
        channel4: MultiReadChannel<T4>,
        channel5: MultiReadChannel<T5>,
        f: suspend (T, T2, T3, T4, T5) -> R
    ): MultiReadChannel<R>

    fun filter(filter: suspend (T) -> Boolean): MultiReadChannel<T>

    fun distinctUntilChanged(equals: (T, T) -> Boolean = { t1, t2 -> t1 == t2 }): MultiReadChannel<T>

    fun drop(amount: Int): MultiReadChannel<T>

    val isClosed: Boolean
}

/**
 * A conflated [MultiReadChannel]. Always has a value.
 */
interface StatefulMultiReadChannel<T> : MultiReadChannel<T> {
    val value: T

    fun peek(): T? = subscribe().consume { poll() }

    // Re-send the current value again, useful to trigger listeners that need to act on new values, without there being a new value.
    fun resend()

    operator fun getValue(thisRef: Any, property: KProperty<*>): T = peek()!!

    override fun <R> map(transform: suspend (T) -> R): StatefulMultiReadChannel<R>
    override fun <R> flatMap(transform: suspend (T) -> ReceiveChannel<R>): StatefulMultiReadChannel<R>
    override fun <T2, R> combineLatest(channel: MultiReadChannel<T2>, f: suspend (T, T2) -> R): StatefulMultiReadChannel<R>
    override fun <T2, T3, R> combineLatest(channel2: MultiReadChannel<T2>, channel3: MultiReadChannel<T3>, f: suspend (T, T2, T3) -> R): StatefulMultiReadChannel<R>
    override fun <T2, T3, T4, R> combineLatest(
        channel2: MultiReadChannel<T2>,
        channel3: MultiReadChannel<T3>,
        channel4: MultiReadChannel<T4>,
        f: suspend (T, T2, T3, T4) -> R
    ): StatefulMultiReadChannel<R>

    override fun <T2, T3, T4, T5, R> combineLatest(
        channel2: MultiReadChannel<T2>,
        channel3: MultiReadChannel<T3>,
        channel4: MultiReadChannel<T4>,
        channel5: MultiReadChannel<T5>,
        f: suspend (T, T2, T3, T4, T5) -> R
    ): StatefulMultiReadChannel<R>

    override fun filter(filter: suspend (T) -> Boolean): StatefulMultiReadChannel<T>
    override fun distinctUntilChanged(equals: (T, T) -> Boolean): StatefulMultiReadChannel<T>
    override fun drop(amount: Int): StatefulMultiReadChannel<T>
}

interface MultiWriteChannel<T> : MultiReadChannel<T> {
    suspend fun send(element: T)
    fun offer(element: T): Boolean

    override fun <R> map(transform: suspend (T) -> R): MultiWriteChannel<R>
    override fun <R> flatMap(transform: suspend (T) -> ReceiveChannel<R>): MultiWriteChannel<R>
    override fun <T2, R> combineLatest(channel: MultiReadChannel<T2>, f: suspend (T, T2) -> R): MultiWriteChannel<R>
    override fun <T2, T3, R> combineLatest(channel2: MultiReadChannel<T2>, channel3: MultiReadChannel<T3>, f: suspend (T, T2, T3) -> R): MultiWriteChannel<R>
    override fun <T2, T3, T4, R> combineLatest(
        channel2: MultiReadChannel<T2>,
        channel3: MultiReadChannel<T3>,
        channel4: MultiReadChannel<T4>,
        f: suspend (T, T2, T3, T4) -> R
    ): MultiWriteChannel<R>

    override fun <T2, T3, T4, T5, R> combineLatest(
        channel2: MultiReadChannel<T2>,
        channel3: MultiReadChannel<T3>,
        channel4: MultiReadChannel<T4>,
        channel5: MultiReadChannel<T5>,
        f: suspend (T, T2, T3, T4, T5) -> R
    ): MultiWriteChannel<R>

    override fun filter(filter: suspend (T) -> Boolean): MultiWriteChannel<T>
    override fun distinctUntilChanged(equals: (T, T) -> Boolean): MultiWriteChannel<T>
    override fun drop(amount: Int): MultiWriteChannel<T>
}

sealed class MultiChannel<T>(
    capacity: Int = 32,
    override val coroutineContext: CoroutineContext = Dispatchers.Default + Job()   // FIXME: I think this is not needed at all.
) : MultiWriteChannel<T>, CoroutineScope {
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

    override suspend fun send(element: T) = channel.send(element)
    override fun offer(element: T) = channel.offer(element)

    override fun close() {
        channel.close()
        coroutineContext.cancel()
    }

    override val isClosed get() = channel.isClosedForSend

    protected abstract fun <R> newChannel(): MultiChannel<R>

    override fun <R> map(transform: suspend (T) -> R) =
        newChannel<R>().also { channel -> mapTo(channel, transform) }

    override fun <R> mapTo(channel: MultiWriteChannel<R>, transform: suspend (T) -> R) =
        subscribe { channel.send(transform(it)) }

    override fun <R> flatMap(transform: suspend (T) -> ReceiveChannel<R>): MultiChannel<R> {
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

    override fun <T2, R> combineLatest(channel: MultiReadChannel<T2>, f: suspend (T, T2) -> R): MultiChannel<R> =
        combineLatestInternal<T2, Nothing, Nothing, Nothing, R>(channel) { v1, v2, _, _, _ -> f(v1, v2) }

    override fun <T2, T3, R> combineLatest(channel2: MultiReadChannel<T2>, channel3: MultiReadChannel<T3>, f: suspend (T, T2, T3) -> R): MultiChannel<R> =
        combineLatestInternal<T2, T3, Nothing, Nothing, R>(channel2, channel3) { v1, v2, v3, _, _ -> f(v1, v2, v3!!) }

    override fun <T2, T3, T4, R> combineLatest(
        channel2: MultiReadChannel<T2>,
        channel3: MultiReadChannel<T3>,
        channel4: MultiReadChannel<T4>,
        f: suspend (T, T2, T3, T4) -> R
    ): MultiChannel<R> = combineLatestInternal<T2, T3, T4, Nothing, R>(channel2, channel3, channel4) { v1, v2, v3, v4, _ -> f(v1, v2, v3!!, v4!!) }

    override fun <T2, T3, T4, T5, R> combineLatest(
        channel2: MultiReadChannel<T2>,
        channel3: MultiReadChannel<T3>,
        channel4: MultiReadChannel<T4>,
        channel5: MultiReadChannel<T5>,
        f: suspend (T, T2, T3, T4, T5) -> R
    ): MultiChannel<R> = combineLatestInternal(channel2, channel3, channel4, channel5) { v1, v2, v3, v4, v5 -> f(v1, v2, v3!!, v4!!, v5!!) }

    private fun <T2, T3, T4, T5, R> combineLatestInternal(
        channel2: MultiReadChannel<T2>,
        channel3: MultiReadChannel<T3>? = null,
        channel4: MultiReadChannel<T4>? = null,
        channel5: MultiReadChannel<T5>? = null,
        f: suspend (T, T2, T3?, T4?, T5?) -> R
    ): MultiChannel<R> {
        val channel = newChannel<R>()

        var latest1: T? = null
        var latest2: T2? = null
        var latest3: T3? = null
        var latest4: T4? = null
        var latest5: T5? = null

        suspend fun sendLatest() {
            if (latest1 != null && latest2 != null &&
                (latest3 != null || channel3 == null) &&
                (latest4 != null || channel4 == null) &&
                (latest5 != null || channel5 == null)
            ) {
                channel.send(f(latest1!!, latest2!!, latest3, latest4, latest5))
            }
        }

        this.subscribe { v1 ->
            latest1 = v1
            sendLatest()
        }

        channel2.subscribe { v2 ->
            latest2 = v2
            sendLatest()
        }

        channel3?.subscribe { v3 ->
            latest3 = v3
            sendLatest()
        }

        channel4?.subscribe { v4 ->
            latest4 = v4
            sendLatest()
        }

        channel5?.subscribe { v5 ->
            latest5 = v5
            sendLatest()
        }

        return channel
    }

    override fun filter(filter: suspend (T) -> Boolean): MultiChannel<T> {
        val channel = newChannel<T>()
        subscribe {
            if (filter(it)) {
                channel.send(it)
            }
        }
        return channel
    }

    override fun distinctUntilChanged(equals: (T, T) -> Boolean): MultiChannel<T> {
        var last: T? = null
        return filter { value ->
            val lastValue = last
            val keep = lastValue == null || !equals(value, lastValue)
            last = value
            keep
        }
    }

    override fun drop(amount: Int): MultiChannel<T> {
        val channel = newChannel<T>()
        var count = 0
        subscribe {
            count += 1
            if (count > amount) {
                channel.send(it)
            }
        }
        return channel
    }
}

class ConflatedMultiChannel<T>(override val coroutineContext: CoroutineContext = Dispatchers.Default + Job()) :
    MultiChannel<T>(Channel.CONFLATED, coroutineContext), StatefulChannel<T> {

    override fun <R> newChannel() = ConflatedMultiChannel<R>(coroutineContext)

    override var value by this

    override fun resend() {
        value = value
    }

    override fun <R> map(transform: suspend (T) -> R) = super.map(transform) as ConflatedMultiChannel<R>
    override fun <R> flatMap(transform: suspend (T) -> ReceiveChannel<R>) = super.flatMap(transform) as ConflatedMultiChannel<R>
    override fun <T2, R> combineLatest(channel: MultiReadChannel<T2>, f: suspend (T, T2) -> R) = super.combineLatest(channel, f) as ConflatedMultiChannel<R>
    override fun <T2, T3, R> combineLatest(channel2: MultiReadChannel<T2>, channel3: MultiReadChannel<T3>, f: suspend (T, T2, T3) -> R) =
        super.combineLatest(channel2, channel3, f) as ConflatedMultiChannel<R>

    override fun <T2, T3, T4, R> combineLatest(
        channel2: MultiReadChannel<T2>,
        channel3: MultiReadChannel<T3>,
        channel4: MultiReadChannel<T4>,
        f: suspend (T, T2, T3, T4) -> R
    ) = super.combineLatest(channel2, channel3, channel4, f) as ConflatedMultiChannel<R>

    override fun <T2, T3, T4, T5, R> combineLatest(
        channel2: MultiReadChannel<T2>,
        channel3: MultiReadChannel<T3>,
        channel4: MultiReadChannel<T4>,
        channel5: MultiReadChannel<T5>,
        f: suspend (T, T2, T3, T4, T5) -> R
    ) = super.combineLatest(channel2, channel3, channel4, channel5, f) as ConflatedMultiChannel<R>

    override fun filter(filter: suspend (T) -> Boolean) = super.filter(filter) as ConflatedMultiChannel<T>
    override fun distinctUntilChanged(equals: (T, T) -> Boolean) = super.distinctUntilChanged(equals) as ConflatedMultiChannel<T>
    override fun drop(amount: Int) = super.drop(amount) as ConflatedMultiChannel<T>

    companion object {
        operator fun <T> invoke(initial: T): ConflatedMultiChannel<T> = ConflatedMultiChannel<T>().apply { offer(initial) }
    }
}

class BufferedMultiChannel<T>(private val capacity: Int = 32, coroutineContext: CoroutineContext = Dispatchers.Default + Job()) :
    MultiChannel<T>(capacity, coroutineContext) {

    override fun <R> newChannel() = BufferedMultiChannel<R>(capacity, coroutineContext)

//    override fun <R> map(transform: suspend (T) -> R) = super.map(transform) as BufferedMultiChannel<R>
//    override fun <R> flatMap(transform: suspend (T) -> ReceiveChannel<R>) = super.flatMap(transform) as BufferedMultiChannel<R>
//    override fun <T2> combineLatest(channel: MultiReadChannel<T2>) = super.combineLatest(channel) as BufferedMultiChannel<Pair<T, T2>>
//    override fun <T2, T3> combineLatest(channel2: MultiReadChannel<T2>, channel3: MultiReadChannel<T3>) = super.combineLatest(channel2, channel3) as BufferedMultiChannel<Triple<T, T2, T3>>
//    override fun filter(filter: suspend (T) -> Boolean) = super.filter(filter) as BufferedMultiChannel<T>
//    override fun distinctUntilChanged(equals: (T, T) -> Boolean) = super.distinctUntilChanged(equals) as BufferedMultiChannel<T>
//    override fun drop(amount: Int) = super.drop(amount) as BufferedMultiChannel<T>
}

val <T> MultiChannel<T>.onReceive: SelectClause1<T> get() = subscribe().onReceive

fun <T> ReceiveChannel<T>.bind(channel: SendChannel<T>, context: CoroutineContext = Dispatchers.Default) = GlobalScope.launch(context) {
    consumeEach {
        channel.send(it)
    }
}

fun <T> channel(): BufferedMultiChannel<T> = BufferedMultiChannel()
fun <T> conflatedChannel(): ConflatedMultiChannel<T> = ConflatedMultiChannel()
fun <T> conflatedChannel(initial: T): ConflatedMultiChannel<T> = ConflatedMultiChannel(initial)

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