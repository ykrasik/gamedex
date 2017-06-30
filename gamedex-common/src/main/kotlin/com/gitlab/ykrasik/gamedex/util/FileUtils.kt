package com.gitlab.ykrasik.gamedex.util

import java.awt.Desktop
import java.io.File
import java.io.IOException
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

fun Long.toHumanReadableFileSize(si: Boolean = true): String {
    val unit = if (si) 1000 else 1024
    if (this < unit) return this.toString() + " B"
    val exp = (Math.log(this.toDouble()) / Math.log(unit.toDouble())).toInt()
    val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1] + if (si) "" else "i"
    return String.format("%.1f %sB", this / Math.pow(unit.toDouble(), exp.toDouble()), pre)
}