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

/**
 * User: ykrasik
 * Date: 16/09/2018
 * Time: 14:41
 */
class MemoryCachedStorage<K, V>(private val delegate: Storage<K, V>, lazy: Boolean) : Storage<K, V> {
    private val cache = if (lazy) mutableMapOf() else delegate.getAll().toMutableMap()

    override fun add(value: V): K {
        val key = delegate.add(value)
        cache[key] = value
        return key
    }

    override fun insert(key: K, value: V) {
        delegate.insert(key, value)
        cache[key] = value
    }

    override fun update(key: K, value: V) {
        delegate.update(key, value)
        cache[key] = value
    }

    override fun set(key: K, value: V) {
        delegate[key] = value
        cache[key] = value
    }

    override fun get(key: K): V? {
        val cachedValue = cache[key]
        if (cachedValue != null) return cachedValue

        val value = delegate[key]
        if (value != null) {
            cache[key] = value
        }
        return value
    }

    override fun getAll() = cache

    override fun delete(key: K): Boolean {
        val deleted = delegate.delete(key)
        cache -= key
        return deleted
    }

    override fun delete(keys: Iterable<K>) {
        delegate.delete(keys)
        cache -= keys
    }

    override fun clear() {
        delegate.clear()
        cache.clear()
    }

    override fun ids() = cache.keys

    override fun sizeTaken(key: K) = delegate.sizeTaken(key)

    override fun toString() = "MemoryCachedStorage[$delegate]"
}

fun <K, V> Storage<K, V>.memoryCached(lazy: Boolean = false): Storage<K, V> = MemoryCachedStorage(this, lazy)