package com.github.ykrasik.gamedex.common

import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * User: ykrasik
 * Date: 02/10/2016
 * Time: 19:52
 */

val Path.childDirectories: List<Path> get() = usingDirectoryStream { it.toList() }
fun Path.firstChildDirectories(amount: Int): List<Path> = usingDirectoryStream { it.take(amount) }

val Path.hasChildDirectories: Boolean get() = usingDirectoryStream { !it.none() }
val Path.hasChildFiles: Boolean get() = usingFileStream { !it.none() }

private fun <T> Path.usingDirectoryStream(consumer: (DirectoryStream<Path>) -> T): T {
    return usingDirectoryStream(DirectoryStream.Filter { Files.isDirectory(it) }, consumer)
}

private fun <T> Path.usingFileStream(consumer: (DirectoryStream<Path>) -> T): T {
    return usingDirectoryStream(DirectoryStream.Filter { Files.isRegularFile(it) }, consumer)
}

private fun <T> Path.usingDirectoryStream(filter: DirectoryStream.Filter<Path>,
                                          consumer: (DirectoryStream<Path>) -> T): T {
    return Files.newDirectoryStream(this, filter).use { consumer(it) }
}

fun String.toPath(): Path = Paths.get(this)