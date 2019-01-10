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

package com.gitlab.ykrasik.gamedex.core

import kotlinx.coroutines.*
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

    private var handlers = emptyMap<KClass<out CoreEvent>, List<suspend (CoreEvent) -> Unit>>()

    @Suppress("UNCHECKED_CAST")
    override fun <E : CoreEvent> on(event: KClass<E>, handler: suspend (E) -> Unit): EventSubscription {
        handlers += event to (handlersFor(event) + handler as (suspend (CoreEvent) -> Unit))
        return object : EventSubscription {
            override fun cancel() {
                handlers += event to (handlersFor(event) - handler)
            }
        }
    }

    override fun <E : CoreEvent> send(event: E) = launch(Job()) {
        handlersFor(event::class).forEach { handler ->
            launch {
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
}