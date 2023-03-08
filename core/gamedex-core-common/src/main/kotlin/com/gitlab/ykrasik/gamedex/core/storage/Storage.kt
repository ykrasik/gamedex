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

package com.gitlab.ykrasik.gamedex.core.storage

/**
 * User: ykrasik
 * Date: 16/09/2018
 * Time: 09:39
 */
interface Storage<K, V> {
    fun add(value: V): K

    @Throws(IllegalStateException::class)
    fun insert(key: K, value: V)

    @Throws(IllegalStateException::class)
    fun update(key: K, value: V)

    operator fun set(key: K, value: V)

    operator fun get(key: K): V?
    fun getAll(): Map<K, V>

    fun delete(key: K): Boolean
    fun delete(keys: Iterable<K>)
    fun clear()

    fun ids(): Iterable<K>

    fun sizeTaken(key: K): Long

    fun sizes(): Map<K, Long> = ids().map { it to sizeTaken(it) }.toMap()
}

inline fun <K, V> Storage<K, V>.getOrPut(key: K, default: () -> V): V =
    get(key) ?: default().apply { set(key, this) }