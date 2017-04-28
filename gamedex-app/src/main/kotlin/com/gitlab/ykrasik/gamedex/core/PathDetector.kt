package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.util.logger
import java.io.File

/**
 * User: ykrasik
 * Date: 15/10/2016
 * Time: 13:16
 *
 * Will recursively scan any directory containing only directories.
 * Will not recurse into a directory containing files.
 *
 * This is a simple algorithm that doesn't check file extensions, if it proves to be too simple (stupid), update this
 * class to only consider file extensions.
 */
class PathDetector(private val excludedPaths: Set<File>) {
    private val log by logger()
    private val newPaths = mutableListOf<File>()

    fun detectNewPaths(path: File): List<File> {
        detectNewPathsRec(path)
        return newPaths
    }

    private fun detectNewPathsRec(path: File) {
        if (!path.exists()) {
            log.warn("Path doesn't exist: $path")
            return
        }
        if (path.isExcluded) return

        val children = path.listFiles().filter { !it.isHidden }
        val shouldScanRecursively = shouldScanRecursively(children)
        if (shouldScanRecursively) {
            children.forEach { detectNewPathsRec(it) }
        } else {
            if (!path.isExcluded) {
                log.debug("[$path] is a new path!")
                newPaths += path
            }
        }
    }

    // Scan children recursively if all children are directories.
    private fun shouldScanRecursively(children: List<File>): Boolean = children.isNotEmpty() && children.all(File::isDirectory)

    private val File.isExcluded get() = this in excludedPaths
}