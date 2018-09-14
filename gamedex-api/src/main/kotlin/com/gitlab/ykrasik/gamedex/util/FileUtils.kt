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

package com.gitlab.ykrasik.gamedex.util

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonValue
import java.awt.Desktop
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

fun browse(path: File) = Desktop.getDesktop().open(path)

// FIXME: Make this an inline class when they are available.
@JsonIgnoreProperties("humanReadable")
data class FileSize(@JsonValue val bytes: Long) : Comparable<FileSize> {
    val humanReadable: String get() {
        val unit = 1024
        if (bytes < unit) return "$bytes B"

        val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
        val pre = ("KMGTPE")[exp - 1]
        return String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
    }

    operator fun plus(other: FileSize): FileSize = FileSize(bytes + other.bytes)
    operator fun minus(other: FileSize): FileSize = FileSize(bytes - other.bytes)

    override fun compareTo(other: FileSize) = bytes.compareTo(other.bytes)

    override fun toString() = humanReadable

    companion object {
        private val regex = "([\\d.]+)[\\s]?([KMGTPE]?B)".toRegex(RegexOption.IGNORE_CASE)
        private val powerMap = mapOf("B" to 0, "KB" to 1, "MB" to 2, "GB" to 3, "TB" to 4, "PB" to 5, "EB" to 6)

        operator fun invoke(humanReadable: String): FileSize {
            val result = regex.find(humanReadable) ?: throw IllegalArgumentException("Invalid input: '$humanReadable'")
            val (number, scale) = result.destructured
            val pow = powerMap[scale.toUpperCase()]!!
            val bytes = BigDecimal(number).multiply(BigDecimal.valueOf(1024).pow(pow))
            return FileSize(bytes.toLong())
        }

        val Empty = FileSize(0)
    }
}