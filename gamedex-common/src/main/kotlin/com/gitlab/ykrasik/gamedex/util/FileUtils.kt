package com.gitlab.ykrasik.gamedex.util

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
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

fun File.sizeTaken() = FileSize(walkBottomUp().fold(0L) { acc, f -> if (f.isFile) acc + f.length() else acc })

// TODO: Why is this needed? Can just use a Long with extensions and factory.
@JsonIgnoreProperties("humanReadable")
data class FileSize(val bytes: Long) : Comparable<FileSize> {
    val humanReadable by lazy {
        val unit = 1024
        if (bytes < unit) return@lazy "$bytes B"

        val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
        val pre = ("KMGTPE")[exp - 1]
        String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
    }

    override fun compareTo(other: FileSize) = bytes.compareTo(other.bytes)

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
    }
}