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
val Path.children: List<Path> get() = usingDirectoryStream(DirectoryStream.Filter<Path> { true }) { it.toList() }

val Path.childDirectories: List<Path> get() = usingDirectoryStream { it.toList() }
fun Path.firstChildDirectories(amount: Int): List<Path> = usingDirectoryStream { it.take(amount) }

val Path.hasChildDirectories: Boolean get() = usingDirectoryStream { !it.none() }
val Path.hasChildFiles: Boolean get() = usingFileStream { !it.none() }

val Path.isDirectory: Boolean get() = Files.isDirectory(this)
val Path.isFile: Boolean get() = Files.isRegularFile(this)

val Path.exists: Boolean get() = Files.exists(this)
fun Path.existsOrNull(): Path? = if (this.exists) this else null

fun String.toPath(): Path = Paths.get(this)

private fun <T> Path.usingDirectoryStream(consumer: (DirectoryStream<Path>) -> T): T {
    return usingDirectoryStream(DirectoryStream.Filter { it.isDirectory }, consumer)
}

private fun <T> Path.usingFileStream(consumer: (DirectoryStream<Path>) -> T): T {
    return usingDirectoryStream(DirectoryStream.Filter { it.isFile }, consumer)
}

private fun <T> Path.usingDirectoryStream(filter: DirectoryStream.Filter<Path>,
                                          consumer: (DirectoryStream<Path>) -> T): T {
    return Files.newDirectoryStream(this, filter).use { consumer(it) }
}