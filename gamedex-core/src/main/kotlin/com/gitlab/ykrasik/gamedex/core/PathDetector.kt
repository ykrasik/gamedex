package com.gitlab.ykrasik.gamedex.core

import com.github.ykrasik.gamedex.common.logger
import com.gitlab.ykrasik.gamedex.core.ui.model.ExcludedPathRepository
import com.gitlab.ykrasik.gamedex.core.ui.model.GameRepository
import com.gitlab.ykrasik.gamedex.core.ui.model.LibraryRepository
import java.io.File
import javax.inject.Inject
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
class PathDetector @Inject constructor(
    private val gameRepository: GameRepository,
    private val libraryRepository: LibraryRepository,
    private val excludedPathRepository: ExcludedPathRepository
) {
    private val log by logger()

    fun detectNewPaths(path: File): List<File> {
        if (!path.exists()) {
            log.warn { "Path doesn't exist: $path" }
            return emptyList()
        }

        val children = path.listFiles().filter { !it.isHidden }
        val shouldScanRecursively = shouldScanRecursively(children)
        return if (shouldScanRecursively) {
            children.flatMap { detectNewPaths(it) }
        } else {
            if (isPathKnown(path)) {
                emptyList()
            } else {
                listOf(path)
            }
        }
    }

    // Scan children recursively if all children are directories.
    private fun shouldScanRecursively(children: List<File>): Boolean = children.isNotEmpty() && children.all(File::isDirectory)

    fun isPathKnown(path: File): Boolean {
        if (gameRepository.contains(path)) {
            log.debug { "[$path] is an already mapped game." }
            return true
        }
        if (libraryRepository.contains(path)) {
            log.debug { "[$path] is an already mapped library." }
            return true
        }
        if (excludedPathRepository.contains(path)) {
            log.debug { "[$path] is an already excluded path." }
            return true
        }

        log.info { "[$path] is a new path!" }
        return false
    }
}