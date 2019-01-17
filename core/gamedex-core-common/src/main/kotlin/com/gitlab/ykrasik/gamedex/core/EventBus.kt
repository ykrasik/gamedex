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
        event: KClass<E>,
        context: CoroutineContext = EmptyCoroutineContext,
        handler: suspend (E) -> Unit
    ): EventSubscription

    fun <E : CoreEvent> send(event: E): Job

    suspend fun <E : CoreEvent> awaitEvent(event: KClass<E>, predicate: (E) -> Boolean = { true }): E
}

inline fun <reified E : CoreEvent> EventBus.on(
    context: CoroutineContext = EmptyCoroutineContext,
    noinline handler: suspend (E) -> Unit
) = on(E::class, context, handler)

suspend inline fun <reified E : CoreEvent> EventBus.awaitEvent(noinline predicate: (E) -> Boolean = { true }) = awaitEvent(E::class, predicate)

interface EventSubscription {
    fun cancel()
}

interface CoreEvent