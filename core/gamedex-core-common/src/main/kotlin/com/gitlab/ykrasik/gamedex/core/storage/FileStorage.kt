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

package com.gitlab.ykrasik.gamedex.core.storage

import com.gitlab.ykrasik.gamedex.util.file
import com.gitlab.ykrasik.gamedex.util.objectMapper
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 21/09/2018
 * Time: 22:45
 */
class FileStorage<K, V>(
    private val basePath: File,
    private val format: FileStorageFormat<V>,
    private val namingStrategy: FileStorageNamingStrategy<K>,
    private val keyGenerator: FileStorageKeyGenerator<K>?
) : Storage<K, V> {
    init {
        keyGenerator?.init(ids())
    }

    override fun add(value: V): K {
        if (keyGenerator == null) throw UnsupportedOperationException("[$basePath] Storage does not support 'add' operations, only 'set'!")

        val key = keyGenerator.nextKey()
        insert(key, value)
        return key
    }

    override fun insert(key: K, value: V) {
        set(key, value) { file ->
            check(!file.exists()) { "File already exists: $file" }
        }
    }

    override fun update(key: K, value: V) {
        set(key, value) { file ->
            check(file.isFile) { "File doesn't exist: $file" }
        }
    }

    override fun set(key: K, value: V) = set(key, value) { }

    private inline fun set(key: K, value: V, handleFile: (File) -> Unit) {
        val file = fileFor(key)
        handleFile(file)
        file.parentFile.mkdirs()
        format.write(file, value)
    }

    override fun get(key: K): V? {
        val file = fileFor(key)
        return if (file.isFile) {
            format.read(file)
        } else {
            null
        }
    }

    override fun getAll() = streamFiles { stream ->
        stream.map { file ->
            val key = namingStrategy.toKey(file)
            val value = format.read(file)
            key to value
        }.toMap()
    } ?: emptyMap()

    override fun delete(key: K) = fileFor(key).delete()
    override fun delete(keys: Iterable<K>) = keys.forEach { delete(it) }
    override fun clear() {
        delete(ids())
        keyGenerator?.init(emptyList())
    }

    override fun ids() = streamFiles { stream -> stream.map { namingStrategy.toKey(it) }.toList() } ?: emptyList()

    override fun sizeTaken(key: K) = fileFor(key).length()

    private fun fileFor(key: K) = basePath.resolve(namingStrategy.toFileName(key))

    private inline fun <T> streamFiles(f: (Sequence<File>) -> T?): T? {
        return if (basePath.isDirectory) {
            Files.newDirectoryStream(basePath.toPath()).use { stream ->
                f(stream.asSequence().map { it.toFile() })
            }
        } else {
            null
        }
    }

    override fun toString() = basePath.toString()

    companion object {
        inline fun <reified V : Any> json(basePath: String): JsonBuilder<V> = json(basePath, V::class)

        fun <V : Any> json(basePath: String, klass: KClass<V>): JsonBuilder<V> =
            JsonBuilder(basePath.file.normalize(), JsonFileStorageFormat(klass))

        fun binary(basePath: String): BinaryBuilder =
            BinaryBuilder(basePath.file.normalize(), BinaryFileStorageFormat)

        class JsonBuilder<V>(
            private val basePath: File,
            private val format: FileStorageFormat<V>
        ) {
            fun intId(): FileStorage<Int, V> = FileStorage(basePath, format, IntIdFileStorageNamingStrategy("json"), IntIdFileStorageKeyGenerator)
            fun stringId(): FileStorage<String, V> = FileStorage(basePath, format, StringIdFileStorageNamingStrategy("json"), keyGenerator = null)
        }

        class BinaryBuilder(
            private val basePath: File,
            private val format: FileStorageFormat<ByteArray>
        ) {
            fun intId(): FileStorage<Int, ByteArray> = FileStorage(basePath, format, IntIdFileStorageNamingStrategy("dat"), IntIdFileStorageKeyGenerator)
            fun stringId(
                extension: String? = null,
                keyTransform: (String) -> String = { it },
                reverseKeyTransform: (String) -> String = { it }
            ): FileStorage<String, ByteArray> =
                FileStorage(basePath, format, StringIdFileStorageNamingStrategy(extension, keyTransform, reverseKeyTransform), keyGenerator = null)
        }
    }
}

interface FileStorageFormat<V> {
    fun write(file: File, value: V)
    fun read(file: File): V
}

class JsonFileStorageFormat<V : Any>(private val klass: KClass<V>) : FileStorageFormat<V> {
    override fun write(file: File, value: V) = objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, value)
    override fun read(file: File): V = objectMapper.readValue(file, klass.java)
}

object BinaryFileStorageFormat : FileStorageFormat<ByteArray> {
    override fun write(file: File, value: ByteArray) = file.writeBytes(value)
    override fun read(file: File) = file.readBytes()
}

interface FileStorageNamingStrategy<K> {
    fun toKey(file: File): K
    fun toFileName(key: K): String
}

class IntIdFileStorageNamingStrategy(private val extension: String) : FileStorageNamingStrategy<Int> {
    override fun toKey(file: File) = file.nameWithoutExtension.toInt()
    override fun toFileName(key: Int) = "$key.$extension"
}

class StringIdFileStorageNamingStrategy(
    private val extension: String? = null,
    private val keyTransform: (String) -> String = { it },
    private val reverseKeyTransform: (String) -> String = { it }
) : FileStorageNamingStrategy<String> {
    override fun toKey(file: File): String = reverseKeyTransform(if (extension != null) file.nameWithoutExtension else file.name)
    override fun toFileName(key: String) = if (extension != null) "${keyTransform(key)}.$extension" else keyTransform(key)
}

interface FileStorageKeyGenerator<K> {
    fun init(keys: List<K>)
    fun nextKey(): K
}

object IntIdFileStorageKeyGenerator : FileStorageKeyGenerator<Int> {
    private val currentId = AtomicInteger()

    override fun init(keys: List<Int>) {
        currentId.set(keys.max() ?: 0)
    }

    override fun nextKey() = currentId.incrementAndGet()
}

interface JsonStorageFactory<K> {
    operator fun <V : Any> invoke(basePath: String, klass: KClass<V>): Storage<K, V>
}

object IntIdJsonStorageFactory : JsonStorageFactory<Int> {
    override fun <V : Any> invoke(basePath: String, klass: KClass<V>): FileStorage<Int, V> =
        FileStorage.json(basePath, klass).intId()

    inline operator fun <reified V : Any> invoke(basePath: String): FileStorage<Int, V> = invoke(basePath, V::class)
}

object StringIdJsonStorageFactory : JsonStorageFactory<String> {
    override fun <V : Any> invoke(basePath: String, klass: KClass<V>): FileStorage<String, V> =
        FileStorage.json(basePath, klass).stringId()

    inline operator fun <reified V : Any> invoke(basePath: String): FileStorage<String, V> = invoke(basePath, V::class)
}