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

package com.gitlab.ykrasik.gamedex.core.api.util

import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ConflatedChannel
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import kotlin.reflect.KProperty

/**
 * User: ykrasik
 * Date: 02/04/2018
 * Time: 11:15
 */
interface BroadcastReceiveChannel<out T> {
    fun subscribe(): SubscriptionReceiveChannel<T>
}

class BroadcastEventChannel<T>(capacity: Int = 10) : BroadcastReceiveChannel<T> {
    private val channel = BroadcastChannel<T>(capacity)

    override fun subscribe() = channel.openSubscription()

    suspend fun send(element: T) = channel.send(element)
    fun offer(element: T) = channel.offer(element)

    fun close() = channel.close()
}

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