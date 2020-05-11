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

package com.gitlab.ykrasik.gamedex.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.merge
import javax.inject.Singleton
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 31/10/2018
 * Time: 22:11
 */
@Singleton
class EventBusImpl : EventBus, CoroutineScope {
    override val coroutineContext = Dispatchers.Default

    // TODO: Consider serializing all access to this with a special dispatcher.
    private val channels = mutableMapOf<KClass<out CoreEvent>, BroadcastChannel<out CoreEvent>>()

    override fun <E : CoreEvent> flowOf(eventClass: KClass<E>): Flow<E> {
        return if (eventClass.isSealed) {
            val channels = eventClass.sealedSubclasses.map { it.channel }
            channels.map { it.asFlow() }.merge()
        } else {
            eventClass.channel.asFlow()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private val <E : CoreEvent> KClass<E>.channel: BroadcastChannel<E>
        get() = channels.getOrPut(this) { BroadcastChannel(32) } as BroadcastChannel<E>

    @Suppress("UNCHECKED_CAST")
    override fun <E : CoreEvent> send(event: E) {
        (event::class.channel as BroadcastChannel<E>).offer(event)
    }
}