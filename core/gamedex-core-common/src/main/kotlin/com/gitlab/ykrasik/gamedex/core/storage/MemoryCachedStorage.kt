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

package com.gitlab.ykrasik.gamedex.core.storage

/**
 * User: ykrasik
 * Date: 16/09/2018
 * Time: 14:41
 */
class MemoryCachedStorage<K, V>(private val delegate: Storage<K, V>) : Storage<K, V> {
    private val cache = delegate.getAll().toMutableMap()

    override fun add(value: V): K {
        val id = delegate.add(value)
        cache[id] = value
        return id
    }

    override fun set(id: K, value: V) {
        delegate[id] = value
        cache[id] = value
    }

    override fun setIfNotExists(id: K, value: V): Boolean {
        val success = delegate.setIfNotExists(id, value)
        if (success) {
            cache[id] = value
        }
        return success
    }

    override fun setOnlyIfExists(id: K, value: V) {
        delegate.setOnlyIfExists(id, value)
        cache[id] = value
    }

    override fun setOnlyIfDoesntExist(id: K, value: V) {
        delegate.setOnlyIfDoesntExist(id, value)
        cache[id] = value
    }

    override fun get(id: K): V? {
        val cachedValue = cache[id]
        if (cachedValue != null) return cachedValue

        val value = delegate[id]
        if (value != null) {
            cache[id] = value
        }
        return value
    }

    override fun getAll() = cache.toMap()

    override fun delete(id: K): Boolean {
        val success = delegate.delete(id)
        if (success) {
            cache -= id
        }
        return success
    }

    override fun deleteOnlyIfExists(id: K) {
        delegate.deleteOnlyIfExists(id)
        cache -= id
    }
}

fun <K, V> Storage<K, V>.memoryCached(): Storage<K, V> = MemoryCachedStorage(this)