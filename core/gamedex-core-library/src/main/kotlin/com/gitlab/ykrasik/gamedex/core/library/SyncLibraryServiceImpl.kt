/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.core.library

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.LibraryPath
import com.gitlab.ykrasik.gamedex.core.CommonData
import com.gitlab.ykrasik.gamedex.core.task.task
import com.gitlab.ykrasik.gamedex.util.logger
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 13/11/2018
 * Time: 08:45
 */
@Singleton
class SyncLibraryServiceImpl @Inject constructor(
    private val commonData: CommonData
) : SyncLibraryService {
    private val log = logger()

    override fun detectNewPaths() = task("Detecting new directories...") {
        errorMessage = { it.message!! }
        val newPaths = mutableListOf<LibraryPath>()
        val excludedDirectories = commonData.libraries.map(Library::path).toSet() + commonData.games.map(Game::path)

        fun detectNewPaths(library: Library, dir: File) {
            // The first iteration of this method is called with parent == null
            if (!dir.exists()) {
                log.warn("Path doesn't exist: $dir")
                return
            }
            if (!dir.isDirectory) {
                log.warn("Path isn't a directory: $dir")
                return
            }
            if (dir != library.path && dir in excludedDirectories) return

            val children = dir.listFiles().filter { !it.isHidden }

            // TODO: This is a very simple strategy, consider changing it if it proves too simple.
            // Scan children recursively if all children are directories.
            val shouldScanRecursively = children.isNotEmpty() && children.all(File::isDirectory)
            if (shouldScanRecursively) {
                children.forEach { child ->
                    detectNewPaths(library, child)
                }
            } else {
                if (dir != library.path) {
                    log.debug("New directory detected: [$dir]")
                    newPaths += LibraryPath(library, dir)
                }
            }
        }

        commonData.contentLibraries.forEach { library ->
            detectNewPaths(library, library.path)
        }

        if (newPaths.isEmpty()) {
            successMessage = { "No new games detected." }
        }

        newPaths as List<LibraryPath>
    }
}