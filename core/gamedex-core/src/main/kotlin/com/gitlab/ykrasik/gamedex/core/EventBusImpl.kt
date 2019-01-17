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

package com.gitlab.ykrasik.gamedex.core

import kotlinx.coroutines.*
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
    private var handlers = emptyMap<KClass<out CoreEvent>, List<EventHandler>>()

    @Suppress("UNCHECKED_CAST")
    override fun <E : CoreEvent> on(event: KClass<E>, context: CoroutineContext, handler: suspend (E) -> Unit): EventSubscription {
        val eventHandler = EventHandler(context, handler as (suspend (CoreEvent) -> Unit))
        handlers += event to (handlersFor(event) + eventHandler)
        return object : EventSubscription {
            override fun cancel() {
                handlers += event to (handlersFor(event) - eventHandler)
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

    override suspend fun <E : CoreEvent> awaitEvent(event: KClass<E>, predicate: (E) -> Boolean): E {
        val result = CompletableDeferred<E>()
        val subscription = on(event) {
            if (predicate(it)) {
                result.complete(it)
            }
        }
        return result.await().apply { subscription.cancel() }
    }

    private fun handlersFor(event: KClass<out CoreEvent>) = handlers.getOrDefault(event, emptyList())

    private data class EventHandler(
        val context: CoroutineContext,
        val handler: suspend (CoreEvent) -> Unit
    )
}