package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.common.util.logger
import java.io.File
import javax.inject.Singleton

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
@Singleton
class PathDetector {
    private val log by logger()

    fun detectNewPaths(path: File, excludedPaths: Set<File>): List<File> {
        val newPaths = mutableListOf<File>()
        detectNewPathsRec(path, excludedPaths, newPaths)
        return newPaths
    }

    private fun detectNewPathsRec(path: File, excludedPaths: Set<File>, newPaths: MutableList<File>) {
        if (!path.exists()) {
            log.warn { "Path doesn't exist: $path" }
            return
        }

        val children = path.listFiles().filter { !it.isHidden }
        val shouldScanRecursively = shouldScanRecursively(children)
        if (shouldScanRecursively) {
            children.forEach { detectNewPathsRec(it, excludedPaths, newPaths) }
        } else {
            if (path !in excludedPaths) {
                log.info { "[$path] is a new path!" }
                newPaths += path
            }
        }
    }

    // Scan children recursively if all children are directories.
    private fun shouldScanRecursively(children: List<File>): Boolean = children.isNotEmpty() && children.all(File::isDirectory)
}