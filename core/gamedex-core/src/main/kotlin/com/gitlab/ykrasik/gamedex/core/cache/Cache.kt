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

package com.gitlab.ykrasik.gamedex.core.cache

import com.gitlab.ykrasik.gamedex.util.fromJson
import com.gitlab.ykrasik.gamedex.util.objectMapper
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 11/09/2018
 * Time: 09:47
 */
interface Cache<K, V> {
    operator fun get(key: K): V?
    operator fun set(key: K, value: V)
    fun getOrPut(key: K, default: () -> V): V
}

class PersistedCache<K, V : Any>(
    private val basePath: String,
    private val serDes: CacheSerDes<V>,
    private val cacheKeyProvider: (K) -> String,
    initial: Map<String, V>
) : Cache<K, V> {
    private val cache: MutableMap<String, V> = initial.toMutableMap()

    override fun get(key: K): V? = cache[cacheKeyProvider(key)]
    override fun set(key: K, value: V) {
        val cacheKey = cacheKeyProvider(key)
        cache[cacheKey] = value
        write(cacheKey, value)
    }

    override fun getOrPut(key: K, default: () -> V): V {
        val cacheKey = cacheKeyProvider(key)
        return cache.getOrPut(cacheKey) { write(cacheKey, default()) }
    }

    private fun write(fileName: String, value: V): V = value.apply {
        val data = serDes.serialize(value)
        File(basePath).mkdirs()
        File("$basePath/$fileName").writeBytes(data)
    }

    companion object {
        /**
         * Read all existing entries
         */
        operator fun <K, V : Any> invoke(basePath: String,
                                         serDes: CacheSerDes<V>,
                                         cacheKeyProvider: (K) -> String): PersistedCache<K, V> {
            val initial = (File(basePath).listFiles() ?: emptyArray()).mapNotNull { f ->
                val key = f.name
                try {
                    val value = serDes.deserialize(f.readBytes())
                    key to value
                } catch (e: Exception) {
                    null
                }
            }.toMap()
            return PersistedCache(basePath, serDes, cacheKeyProvider, initial)
        }
    }
}

interface CacheSerDes<V> {
    fun serialize(value: V): ByteArray
    fun deserialize(data: ByteArray): V
}

class JsonCacheSerDes<V : Any>(private val klass: KClass<V>) : CacheSerDes<V> {
    override fun serialize(value: V): ByteArray {
        val os = ByteArrayOutputStream()
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(os, value)
        return os.toByteArray()
    }

    override fun deserialize(data: ByteArray): V = data.fromJson(klass)

    companion object {
        inline operator fun <reified V : Any> invoke() = JsonCacheSerDes(V::class)
    }
}