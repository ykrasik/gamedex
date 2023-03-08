/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
    private val flows = mutableMapOf<KClass<out CoreEvent>, MutableSharedFlow<CoreEvent>>()

    override fun <E : CoreEvent> flowOf(eventClass: KClass<E>): Flow<E> {
        return if (eventClass.isSealed) {
            val subClassFlows = eventClass.sealedSubclasses.map { it.flow }
            subClassFlows.merge()
        } else {
            eventClass.flow
        }
    }

    @Suppress("UNCHECKED_CAST")
    private val <E : CoreEvent> KClass<out E>.flow: MutableSharedFlow<E>
        get() = flows.getOrPut(this) { MutableSharedFlow() } as MutableSharedFlow<E>

    override suspend fun <E : CoreEvent> emit(event: E) {
        event::class.flow.emit(event)
    }
}