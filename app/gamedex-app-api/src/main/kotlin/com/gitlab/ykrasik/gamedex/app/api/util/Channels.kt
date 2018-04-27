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

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.reflect.KProperty

/**
 * User: ykrasik
 * Date: 02/04/2018
 * Time: 11:15
 */
interface BroadcastReceiveChannel<out T> {
    fun subscribe(): SubscriptionReceiveChannel<T>

    fun subscribe(context: CoroutineContext = DefaultDispatcher, f: suspend (T) -> Unit): SubscriptionReceiveChannel<T> {
        val subscription = subscribe()
        launch(context) {
            subscription.consumeEach {
                f(it)
            }
        }
        return subscription
    }
}

class BroadcastEventChannel<T>(capacity: Int = 32) : BroadcastReceiveChannel<T> {
    private val channel = BroadcastChannel<T>(capacity)

    override fun subscribe() = channel.openSubscription()

    suspend fun send(element: T) = channel.send(element)
    fun offer(element: T) = channel.offer(element)

    fun close() = channel.close()

    companion object {
        fun <T> conflated(initial: T): BroadcastEventChannel<T> =
            BroadcastEventChannel<T>(Channel.CONFLATED).apply { offer(initial) }
    }
}

fun <T> ReceiveChannel<T>.launchConsumeEach(context: CoroutineContext = CommonPool, f: suspend (T) -> Unit) = launch(context) {
    consumeEach {
        f(it)
    }
}

fun <T> ReceiveChannel<T>.bind(channel: SendChannel<T>, context: CoroutineContext = CommonPool) = launch(context) {
    consumeEach {
        channel.send(it)
    }
}

fun <T> ReceiveChannel<T>.clone(capacity: Int = Channel.UNLIMITED): Pair<ReceiveChannel<T>, ReceiveChannel<T>> {
    val channel1 = Channel<T>(capacity)
    val channel2 = Channel<T>(capacity)
    launch(CommonPool) {
        consumeEach {
            channel1.send(it)
            channel2.send(it)
        }
    }
    return channel1 to channel2
}

inline fun <E, R> SendChannel<E>.produceOnly(block: SendChannel<E>.() -> R): R =
    try {
        block()
    } finally {
        close()
    }

// capacity = 2 to accomodate the closeToken being sent.
fun <T> singleValueChannel(value: T): ReceiveChannel<T> = Channel<T>(capacity = 2).apply { offer(value); close() }

fun <T> conflatedChannel(): ConflatedChannel<T> = ConflatedChannel()
fun <T> conflatedChannel(initial: T): ConflatedChannel<T> = conflatedChannel<T>().apply {
    offer(initial)
}

fun <T> conflatedBroadcastEventChannel(): BroadcastEventChannel<T> = BroadcastEventChannel(Channel.CONFLATED)
fun <T> conflatedBroadcastEventChannel(initial: T) = conflatedBroadcastEventChannel<T>().apply {
    offer(initial)
}

// TODO: Be VERY careful with this, it will only return a value for the first time it's read! Add a global cache? How to clean it?
operator fun <T> ConflatedChannel<T>.getValue(thisRef: Any, property: KProperty<*>) = poll()!!
operator fun <T> ConflatedChannel<T>.setValue(thisRef: Any, property: KProperty<*>, value: T) {
    offer(value)
}
//operator fun <T> ReceiveChannel<T>.getValue(thisRef: Any, property: KProperty<*>) = poll()!!
//operator fun <T> SendChannel<T>.setValue(thisRef: Any, property: KProperty<*>, value: T) {
//    offer(value)
//}