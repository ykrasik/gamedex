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

fun File.sizeTaken() = FileSize(walkBottomUp().fold(0L) { acc, f -> if (f.isFile) acc + f.length() else acc })

data class FileSize(val bytes: Long) : Comparable<FileSize> {
    val humanReadable by lazy {
        val unit = 1024
        if (bytes < unit) return@lazy "$bytes B"

        val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
        val pre = ("KMGTPE")[exp - 1]
        String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
    }

    override fun compareTo(other: FileSize) = bytes.compareTo(other.bytes)

    //    companion object {
//        operator fun invoke(humanReadable: String): FileSize {
//            var returnValue: Long = -1
//            val patt = Pattern.compile("([\\d.]+)([GMK]B)", Pattern.CASE_INSENSITIVE)
//            val matcher = patt.matcher(filesize)
//            val powerMap = HashMap<String, Int>()
//            powerMap.put("GB", 3)
//            powerMap.put("MB", 2)
//            powerMap.put("KB", 1)
//            if (matcher.find()) {
//                val number = matcher.group(1)
//                val pow = powerMap[matcher.group(2).toUpperCase()]
//                var bytes = BigDecimal(number)
//                bytes = bytes.multiply(BigDecimal.valueOf(1024).pow(pow))
//                returnValue = bytes.toLong()
//            }
//            return returnValue
//        }
//    }
}