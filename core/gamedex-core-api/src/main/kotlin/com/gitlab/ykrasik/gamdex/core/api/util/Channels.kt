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

package com.gitlab.ykrasik.gamdex.core.api.util

import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import kotlinx.coroutines.experimental.launch

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

    fun close() = channel.close()
}

fun <T> conflatedChannel(initial: T? = null) = Channel<T>(Channel.CONFLATED).apply {
    if (initial != null) {
        launch(Unconfined) {
            send(initial)
        }
    }
}