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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 20/10/2018
 * Time: 09:07
 */
interface EventBus {
    fun <E : CoreEvent> flowOf(eventClass: KClass<E>): Flow<E>

    suspend fun <E : CoreEvent> emit(event: E)
}

inline fun <reified E : CoreEvent> EventBus.flowOf(): Flow<E> = flowOf(E::class)

suspend inline fun <reified E : CoreEvent> EventBus.awaitEvent(crossinline predicate: (E) -> Boolean = { true }) =
    flowOf<E>().filter { predicate(it) }.first()

interface CoreEvent