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

package com.gitlab.ykrasik.gamedex.util

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonValue
import java.io.File
import java.io.IOException
import java.math.BigDecimal
import java.net.URI
import java.nio.file.Files

/**
 * User: ykrasik
 * Date: 02/10/2016
 * Time: 19:52
 */
fun String.toFile(): File = File(this)

fun URI.toFile(): File = File(this)

fun File.create() {
    parentFile?.mkdirs()
    createNewFile()
}

fun File.deleteWithChildren() {
    assertExists()
    walkBottomUp().forEach { Files.delete(it.toPath()) }
}

fun File.existsOrNull(): File? = if (exists()) this else null

fun File.assertExists() = existsOrNull() ?: throw IOException("File doesn't exist: $this")

// FIXME: Make this an inline class when they are available.
@JsonIgnoreProperties("humanReadable")
data class FileSize(@JsonValue val bytes: Long) : Comparable<FileSize> {
    val scaled: Pair<Double, Scale>
        get () {
            val unit = 1024
            if (bytes < unit) return bytes.toDouble() to Scale.B

            val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
            val number = bytes / Math.pow(unit.toDouble(), exp.toDouble())
            val scale = Scale.values().drop(1)[exp - 1]
            return number to scale
        }

    val humanReadable: String
        get() {
            val (number, scale) = scaled
            return String.format("%.1f %s", number, scale)
        }

    operator fun plus(other: FileSize): FileSize = FileSize(bytes + other.bytes)
    operator fun minus(other: FileSize): FileSize = FileSize(bytes - other.bytes)

    override fun compareTo(other: FileSize) = bytes.compareTo(other.bytes)

    override fun toString() = humanReadable

    enum class Scale { B, KB, MB, GB, TB }

    companion object {
        private val regex = "([\\d.]+)[\\s]?([KMGT]?B)".toRegex(RegexOption.IGNORE_CASE)

        operator fun invoke(humanReadable: String): FileSize {
            val result = regex.find(humanReadable) ?: throw IllegalArgumentException("Invalid input: '$humanReadable'")
            val (amount, scale) = result.destructured
            return invoke(amount.toDouble(), Scale.valueOf(scale.toUpperCase()))
        }

        operator fun invoke(amount: Number, scale: Scale): FileSize {
            val bytes = BigDecimal(amount.toDouble()).multiply(BigDecimal.valueOf(1024).pow(scale.ordinal))
            return FileSize(bytes.toLong())
        }

        val Empty = FileSize(0)
    }
}