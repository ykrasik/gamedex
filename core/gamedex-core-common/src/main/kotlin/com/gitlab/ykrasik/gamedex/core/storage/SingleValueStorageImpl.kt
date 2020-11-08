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

package com.gitlab.ykrasik.gamedex.core.storage

import com.gitlab.ykrasik.gamedex.util.SingleValueStorage
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 08/11/2020
 * Time: 14:14
 */
class SingleValueStorageImpl<V>(private val storage: Storage<String, V>, private val key: String) : SingleValueStorage<V> {
    override fun get() = storage[key]
    override fun set(v: V) = storage.set(key, v)
    override fun reset() {
        storage.delete(key)
    }

    companion object {
        operator fun <V : Any> invoke(
            basePath: String,
            key: String,
            klass: KClass<V>,
            memoryCached: Boolean = false,
            lazy: Boolean = true,
        ): SingleValueStorage<V> {
            val storage = StringIdJsonStorageFactory(basePath, klass).let { if (memoryCached) it.memoryCached(lazy = lazy) else it }
            return SingleValueStorageImpl(storage, key)
        }

        inline operator fun <reified V : Any> invoke(
            basePath: String,
            key: String,
            memoryCached: Boolean = false,
            lazy: Boolean = true,
        ): SingleValueStorage<V> = invoke(basePath, key, V::class, memoryCached, lazy)
    }
}