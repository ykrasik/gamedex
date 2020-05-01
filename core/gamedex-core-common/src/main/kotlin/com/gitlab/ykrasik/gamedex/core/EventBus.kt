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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 20/10/2018
 * Time: 09:07
 */
interface EventBus {
    fun <E : CoreEvent> on(
        eventClass: KClass<E>,
        context: CoroutineContext = EmptyCoroutineContext,
        handler: suspend (E) -> Unit
    ): EventSubscription

    fun <E : CoreEvent> send(event: E): Job
}

inline fun <reified E : CoreEvent> EventBus.on(
    context: CoroutineContext = EmptyCoroutineContext,
    noinline handler: suspend (E) -> Unit
) = on(E::class, context, handler)

suspend inline fun <E : CoreEvent> EventBus.awaitEvent(event: KClass<E>, crossinline predicate: (E) -> Boolean = { true }): E {
    val result = CompletableDeferred<E>()
    val subscription = on(event) {
        if (predicate(it)) {
            result.complete(it)
        }
    }
    return result.await().apply { subscription.cancel() }
}

suspend inline fun <reified E : CoreEvent> EventBus.awaitEvent(crossinline predicate: (E) -> Boolean = { true }) = awaitEvent(E::class, predicate)

interface EventSubscription {
    fun cancel()
}

interface CoreEvent