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
import com.gitlab.ykrasik.gamedex.util.time
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 16/09/2018
 * Time: 14:41
 */
abstract class JsonStorage<K, V : Any>(protected val basePath: String, private val klass: KClass<V>) : Storage<K, V> {
    companion object {
        @JvmStatic
        protected val log = logger()
    }

    override fun set(id: K, value: V) {
        val file = fileFor(id)
        log.trace("[$file] Writing: $value")
        serialize(file, value)
    }

    override fun get(id: K): V? {
        val file = fileFor(id)
        return if (file.isFile) {
            log.info("[$file] Reading...")
            log.time({ "[$file] Reading... Done: $it" }) {
                deserialize(file)
            }
        } else {
            null
        }
    }

    override fun getAll() = streamFiles { stream ->
        log.info("[$basePath] Reading all files...")
        log.time({ "[$basePath] Reading all files... Done: $it"}) {
            stream.mapNotNull { file ->
                val id = parseId(file.nameWithoutExtension)
                val value = deserialize(file)
                value?.let { id to it }
            }.toMap()
        }
    } ?: emptyMap()

    protected abstract fun parseId(fileName: String): K
    private fun fileFor(id: K) = File("$basePath/$id.json")

    private fun serialize(file: File, value: V) {
        file.parentFile.mkdirs()
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, value)
    }

    private fun deserialize(file: File) =
        try {
            objectMapper.readValue(file, klass.java)
        } catch (e: Exception) {
            log.error("[$file] Error!", e)
            null
        }

    protected inline fun <T> streamFiles(f: (Sequence<File>) -> T?): T? {
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

class IntIdJsonStorage<V : Any>(basePath: String, klass: KClass<V>) : JsonStorage<Int, V>(basePath, klass) {
    // Initialize nextId as the max existing id.
    private val nextId = AtomicInteger(streamFiles { stream -> stream.map { it.nameWithoutExtension.toInt() }.max() } ?: 0)

    init {
        log.trace("[$basePath] Initialized, next id: $nextId")
    }

    override fun add(value: V): Int {
        val id = nextId()
        set(id, value)
        return id
    }

    private fun nextId() = nextId.incrementAndGet()

    override fun parseId(fileName: String) = fileName.toInt()
}

class StringIdJsonStorage<V : Any>(basePath: String, klass: KClass<V>) : JsonStorage<String, V>(basePath, klass) {
    override fun add(value: V): String {
        throw UnsupportedOperationException("StringId Storage does not support key generation!")
    }

    override fun parseId(fileName: String) = fileName
}

interface JsonStorageFactory<K> {
    operator fun <V : Any> invoke(basePath: String, klass: KClass<V>): Storage<K, V>
}

object IntIdJsonStorageFactory : JsonStorageFactory<Int> {
    override fun <V : Any> invoke(basePath: String, klass: KClass<V>): IntIdJsonStorage<V> =
        IntIdJsonStorage(basePath, klass)
}

object StringIdJsonStorageFactory : JsonStorageFactory<String> {
    override fun <V : Any> invoke(basePath: String, klass: KClass<V>): StringIdJsonStorage<V> =
        StringIdJsonStorage(basePath, klass)
}