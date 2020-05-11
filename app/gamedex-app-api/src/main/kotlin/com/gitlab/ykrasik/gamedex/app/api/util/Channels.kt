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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

fun <T> broadcastFlow() = BroadcastFlow<T>(BroadcastChannel(2))

class BroadcastFlow<T> private constructor(
    private val channel: BroadcastChannel<T>,
    private val flow: Flow<T>
) : BroadcastChannel<T> by channel, Flow<T> by flow {
    companion object {
        operator fun <T> invoke(channel: BroadcastChannel<T>) = BroadcastFlow(channel, channel.asFlow())
    }
}

inline fun <F, T, R> F.writeTo(flow: MutableStateFlow<R>, crossinline f: (T) -> R) where F : Flow<T>, F : CoroutineScope = apply {
    launch(Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) {
        collect {
            flow.value = f(it)
        }
    }
}

inline fun <F, T, R> F.writeFrom(flow: Flow<R>, crossinline f: (R) -> T) where F : MutableStateFlow<T>, F : CoroutineScope = apply {
    launch(Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) {
        flow.collect {
            this@apply.value = f(it)
        }
    }
}