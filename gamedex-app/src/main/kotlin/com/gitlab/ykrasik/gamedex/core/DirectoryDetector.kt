package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.util.logger
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
class NewDirectoryDetector {
    private val log by logger()

    suspend fun detectNewDirectories(dir: File, excludedDirectories: Set<File>): List<File> {
        val context = NewDirectoryContext(excludedDirectories)
        context.detectNewDirectories(dir)
        return context.newDirectories
    }

    private inner class NewDirectoryContext(private val excludedDirectories: Set<File>) {
        val newDirectories = mutableListOf<File>()

        fun detectNewDirectories(dir: File) {
            if (!dir.isDirectory) {
                log.warn("Directory doesn't exist: [$dir]")
                throw DirectoryDoesntExistException(dir)
            }
            if (dir.isExcluded) return

            // TODO: this blocks until all children are resolved, make this cancellable.
            val children = dir.listFiles().filter { !it.isHidden }
            if (shouldScanRecursively(children)) {
                children.forEach { detectNewDirectories(it) }
            } else {
                if (!dir.isExcluded) {
                    log.debug("New directory detected: [$dir]")
                    newDirectories += dir
                }
            }
        }

        // Scan children recursively if all children are directories.
        private fun shouldScanRecursively(children: List<File>): Boolean = children.isNotEmpty() && children.all(File::isDirectory)

        private val File.isExcluded get() = this in excludedDirectories
    }
}

class DirectoryDoesntExistException(dir: File) : RuntimeException("Directory doesn't exist: '$dir'")