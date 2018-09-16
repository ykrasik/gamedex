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

package com.gitlab.ykrasik.gamedex.core.persistence

import com.gitlab.ykrasik.gamedex.util.logger
import com.gitlab.ykrasik.gamedex.util.objectMapper
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 16/09/2018
 * Time: 09:39
 */
interface Storage<K, V> {
    fun add(value: V): K
    operator fun set(id: K, value: V)
    operator fun get(id: K): V?
    fun getAll(): Map<K, V>

    companion object {
        inline fun <reified V : Any> json(basePath: String): Storage<Int, V> = JsonFileStorage(basePath)
    }
}

class JsonFileStorage<V : Any>(private val basePath: String, private val klass: KClass<V>) : Storage<Int, V> {
    companion object {
        inline operator fun <reified V : Any> invoke(basePath: String): JsonFileStorage<V> =
            JsonFileStorage(basePath, V::class)

        private val log = logger()
    }

    private val nextId = AtomicInteger(maxUsedId())
    
    init {
        log.trace("[$basePath] Initialized, next id: $nextId")
    }

    override fun add(value: V): Int {
        val id = nextId()
        set(id, value)
        return id
    }

    override fun set(id: Int, value: V) {
        val file = fileFor(id)
        log.trace("[$file] Writing: $value")
        serialize(file, value)
    }

    override fun get(id: Int): V? {
        val file = fileFor(id)
        return if (file.isFile) {
            deserialize(file)
        } else {
            null
        }
    }

    override fun getAll() = streamFiles { stream ->
        stream.mapNotNull { file ->
            try {
                val id = file.nameWithoutExtension.toInt()
                val value = deserialize(file)
                id to value
            } catch (e: Exception) {
                null
            }
        }.toMap()
    } ?: emptyMap()

    private fun maxUsedId() = streamFiles { stream -> stream.map { it.nameWithoutExtension.toInt() }.max() } ?: 0

    private fun nextId() = nextId.incrementAndGet()
    private fun fileFor(id: Int) = File("$basePath/$id.json")

    private fun serialize(file: File, value: V) {
        file.parentFile.mkdirs()
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, value)
    }
    private fun deserialize(file: File) = objectMapper.readValue(file, klass.java)

    private inline fun <T> streamFiles(f: (Sequence<File>) -> T?): T? {
        val file = File(basePath)
        return if (file.isDirectory) {
            Files.newDirectoryStream(file.toPath()).use { stream ->
                f(stream.asSequence().map { it.toFile() })
            }
        } else {
            null
        }
    }
}

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
}

fun <K, V> Storage<K, V>.memoryCached(): Storage<K, V> = MemoryCachedStorage(this)

//class KeyMappedStorage<K1, K2, V>(
//    private val delegate: Storage<K2, V>,
//    private val keyMapper: (K1) -> K2
//) : Storage<K1, V> {
//    override fun add(value: V): K1 {
////        val id = delegate.add(value)
////        return keyMapper()
//        TODO("add(): not implemented")
//    }
//
//    override fun set(id: K1, value: V) {
//        delegate[keyMapper(id)] = value
//    }
//
//    override fun get(id: K1): V? =
//        delegate[keyMapper(id)]
//
//    override fun getAll(): Map<K1, V> {
//        TODO("getAll(): not implemented")
//    }
//}