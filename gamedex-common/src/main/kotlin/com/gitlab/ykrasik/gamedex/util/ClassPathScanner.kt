package com.gitlab.ykrasik.gamedex.util

import java.net.URI
import java.net.URL
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.*

/**
 * User: ykrasik
 * Date: 07/10/2016
 * Time: 16:23
 */
object ClassPathScanner {
    fun scanPackage(packageName: String, predicate: (String) -> Boolean): List<URL> = scanPath(packageName.replace('.', '/'), predicate)

    fun scanPath(path: String, predicate: (String) -> Boolean): List<URL> {
        val visitor = ClassPathFileVisitor(predicate)
        val resources = Thread.currentThread().contextClassLoader.getResources(path)
        resources.iterator().forEach { scanUrl(it, visitor) }
        return visitor.resources
    }

    private fun scanUrl(url: URL, visitor: ClassPathFileVisitor) {
        val filePath = url.path
        val pathElements = filePath.split("!".toRegex()).dropLastWhile(String::isEmpty)
        if (pathElements.size > 1) {
            // URL points inside a Jar file - create a fileSystem from the jar file itself (the part before the '!'),
            // then navigate the the part after the '!' and walk the fileTree from there.
            FileSystems.newFileSystem(Paths.get(URI.create(pathElements[0])), null).use { fs ->
                val path = fs.getPath(pathElements[1])
                Files.walkFileTree(path, visitor)
            }
        } else {
            // URL points to a directory - just walk the fileTree.
            val path = Paths.get(url.toURI())
            Files.walkFileTree(path, visitor)
        }
    }

    private class ClassPathFileVisitor(private val predicate: (String) -> Boolean) : SimpleFileVisitor<Path>() {
        val resources = mutableListOf<URL>()

        override fun visitFile(path: Path, attrs: BasicFileAttributes): FileVisitResult {
            val fileName = path.fileName.toString()
            if (predicate(fileName)) {
                resources.add(path.toUri().toURL())
            }
            return FileVisitResult.CONTINUE
        }
    }

    private fun <T> Enumeration<T>.iterator(): Iterator<T> = object : Iterator<T> {
        override operator fun hasNext() = this@iterator.hasMoreElements()
        override operator fun next() = this@iterator.nextElement()
    }
}