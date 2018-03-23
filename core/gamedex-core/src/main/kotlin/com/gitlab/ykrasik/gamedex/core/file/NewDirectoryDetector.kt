/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 * http://www.apache.org/licenses/LICENSE-2.0                               *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ****************************************************************************/

package com.gitlab.ykrasik.gamedex.core.file

import com.gitlab.ykrasik.gamedex.util.logger
import java.io.File

/**
 * User: ykrasik
 * Date: 15/10/2016
 * Time: 13:16
 */
interface NewDirectoryDetector {
    // TODO: Make this an observable.
    fun detectNewDirectories(dir: File, excludedDirectories: Set<File>): List<File>
}

/**
 * Will recursively scan any directory containing only directories.
 * Will not recurse into a directory containing files.
 *
 * This is a simple algorithm that doesn't check file extensions, if it proves to be too simple (stupid), update this
 * class to only consider file extensions.
 */
class SimpleNewDirectoryDetector: NewDirectoryDetector {
    private val log = logger()

    override fun detectNewDirectories(dir: File, excludedDirectories: Set<File>): List<File> {
        val context = NewDirectoryContext(excludedDirectories)
        context.detectNewDirectories(dir)
        return context.newDirectories
    }

    private inner class NewDirectoryContext(private val excludedDirectories: Set<File>) {
        val newDirectories = mutableListOf<File>()

        fun detectNewDirectories(dir: File) {
            if (!dir.isDirectory) {
                log.warn("Directory doesn't exist: [$dir]")
                throw DirectoryDoesNotExistException(dir)
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

    class DirectoryDoesNotExistException(dir: File) : RuntimeException("Directory doesn't exist: '$dir'")
}