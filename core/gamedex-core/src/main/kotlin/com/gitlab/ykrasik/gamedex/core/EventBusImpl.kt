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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext
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
    private val handlers = mutableMapOf<KClass<out CoreEvent>, MutableList<EventHandler>>()

    @Suppress("UNCHECKED_CAST")
    override fun <E : CoreEvent> on(eventClass: KClass<E>, context: CoroutineContext, handler: suspend (E) -> Unit): EventSubscription {
        return if (eventClass.isSealed) {
            val subscriptions = eventClass.sealedSubclasses.map {
                on(it, context, handler)
            }
            object : EventSubscription {
                override fun cancel() {
                    subscriptions.forEach { it.cancel() }
                }
            }
        } else {
            val eventHandler = EventHandler(context, handler as (suspend (CoreEvent) -> Unit))
            handlersFor(eventClass) += eventHandler
            object : EventSubscription {
                override fun cancel() {
                    handlersFor(eventClass) -= eventHandler
                }
            }
        }
    }

    override fun <E : CoreEvent> send(event: E) = launch(Job()) {
        handlersFor(event::class).forEach { (context, handler) ->
            launch(context) {
                handler(event)
            }
        }
    }

    private fun handlersFor(event: KClass<out CoreEvent>) = handlers.getOrPut(event) { mutableListOf() }

    private data class EventHandler(
        val context: CoroutineContext,
        val handler: suspend (CoreEvent) -> Unit
    )
}