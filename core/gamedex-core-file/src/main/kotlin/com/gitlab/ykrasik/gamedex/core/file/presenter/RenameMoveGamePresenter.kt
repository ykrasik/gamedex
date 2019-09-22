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

package com.gitlab.ykrasik.gamedex.core.file.presenter

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.app.api.game.RenameMoveGameView
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.file.FileSystemService
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.core.task.task
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.gitlab.ykrasik.gamedex.util.file
import com.gitlab.ykrasik.gamedex.util.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 19/05/2018
 * Time: 12:15
 */
@Singleton
class RenameMoveGamePresenter @Inject constructor(
    private val gameService: GameService,
    private val libraryService: LibraryService,
    private val fileSystemService: FileSystemService,
    private val taskService: TaskService,
    private val eventBus: EventBus
) : Presenter<RenameMoveGameView> {
    private val log = logger()

    override fun present(view: RenameMoveGameView) = object : ViewSession() {
        private val game by view.game
        private var newPath by view.newPath
        private var newPathLibrary by view.newPathLibrary

        init {
            view.newPath.forEach { onPathChanged() }

            view.browseActions.forEach { onBrowse() }
            view.acceptActions.forEach { onAccept() }
            view.cancelActions.forEach { onCancel() }
        }

        override suspend fun onShown() {
            newPath = (view.initialName.value?.let { game.path.parentFile.resolve(it) } ?: game.path).path
            onPathChanged()
        }

        private suspend fun onBrowse() {
            val initialDirectory = when {
                newPath.file.isDirectory -> newPath.file
                newPathLibrary.id != Library.Null.id -> newPathLibrary.path
                else -> game.library.path
            }.takeIf { it.isDirectory } ?: File(".")
            val selectedDirectory = view.browse(initialDirectory)
            if (selectedDirectory != null) {
                newPath = selectedDirectory.path
                onPathChanged()
            }
        }

        private suspend fun onPathChanged() {
            validateAndDetectLibrary()
        }

        private suspend fun validateAndDetectLibrary() {
            view.canAccept *= IsValid.invalid("Loading...")
            view.newPathIsValid *= IsValid {
                withContext(Dispatchers.IO) {
                    check(!newPath.isBlank()) { "Path is required!" }
                    try {
                        Paths.get(newPath)
                    } catch (e: Exception) {
                        error("Invalid path: ${e.message}")
                    }

                    // Detect library
                    val library = checkNotNull(libraryService.libraries
                        .asSequence()
                        .mapNotNull { library -> matchPath(library) }
                        .maxBy { it.numElements }
                        ?.library
                    ) { "Path doesn't belong to any library!" }
                    withContext(Dispatchers.Main) {
                        newPathLibrary = library
                    }

                    check(game.path.exists()) { "Source path doesn't exist!" }

                    check(newPath != game.path.path) { "Path already exists!" }
                    if (newPath.file.exists()) {
                        // Windows is case insensitive and will report that the file already exists, even if there is a case difference.
                        check(newPath.file.canonicalFile.path.equals(game.path.canonicalFile.path, ignoreCase = true)) { "Path already exists!" }
                    }
                }
            }
            view.canAccept *= view.newPathIsValid.value
        }

        private fun matchPath(library: Library): LibraryMatch? {
            val libraryElements = library.path.canonicalPath.split(File.separatorChar)
            val pathElements = newPath.file.canonicalPath.split(File.separatorChar)
            libraryElements.forEachIndexed { i, element ->
                val pathElement = pathElements.getOrNull(i) ?: return null
                if (pathElement != element) return null
            }
            return LibraryMatch(library, numElements = libraryElements.size)
        }

        private suspend fun onAccept() {
            view.canAccept.assert()

            log.info("Renaming/Moving: ${game.path} -> $newPath")

            view.canAccept *= IsValid.invalid("Loading...")
            try {
                taskService.execute(task("Moving ${game.path}...") {
                    fileSystemService.move(from = game.path, to = newPath.file)
                    executeSubTask(gameService.replace(game, game.rawGame.withMetadata { it.copy(libraryId = newPathLibrary.id, path = newPath) }))
                })
                hideView()
            } catch (e: Exception) {
                log.error("Error", e)
                view.onError(e.message!!, "Error!", e)
                validateAndDetectLibrary()
            }
        }

        private fun onCancel() {
            hideView()
        }

        private fun hideView() = eventBus.requestHideView(view)
    }

    private data class LibraryMatch(
        val library: Library,
        val numElements: Int
    )
}