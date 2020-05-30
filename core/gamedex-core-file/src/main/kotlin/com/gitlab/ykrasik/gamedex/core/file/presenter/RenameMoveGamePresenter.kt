/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.core.file.FileSystemService
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.core.task.task
import com.gitlab.ykrasik.gamedex.core.view.Presenter
import com.gitlab.ykrasik.gamedex.core.view.ViewSession
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.gitlab.ykrasik.gamedex.util.Try
import com.gitlab.ykrasik.gamedex.util.file
import com.gitlab.ykrasik.gamedex.util.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
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
        val game by view.game
        var targetPath by view.targetPath
        var targetPathLibrary by view.targetPathLibrary
        val loading = MutableStateFlow(false)

        init {
            this::isShowing.forEach {
                if (it) {
                    val initialName = view.initialName.v
                    view.targetPath /= (initialName?.let { game.path.parentFile.resolve(it) } ?: game.path).path
                }
            }

            view::canAccept *= combine(loading, view.targetPathIsValid) { loading, targetPathIsValid ->
                if (loading) IsValid.invalid("Loading...") else targetPathIsValid
            }

            view.targetPath.allValues().forEach(debugName = "onTargetPathChanged") { validateAndDetectLibrary(it) }

            view::browseActions.forEach { onBrowse() }
            view::acceptActions.forEach { onAccept() }
            view::cancelActions.forEach { onCancel() }
        }

        private fun onBrowse() {
            val initialDirectory = when {
                targetPath.file.isDirectory -> targetPath.file
                targetPathLibrary.id != Library.Null.id -> targetPathLibrary.path
                else -> game.library.path
            }.takeIf { it.isDirectory } ?: File(".")
            val selectedDirectory = view.browse(initialDirectory)
            if (selectedDirectory != null) {
                targetPath = selectedDirectory.path
            }
        }

        private suspend fun validateAndDetectLibrary(path: String) = withContext(Dispatchers.IO) {
            loading /= true
            view.targetPathIsValid /= Try {
                check(!path.isBlank()) { "Path is required!" }
                try {
                    Paths.get(path)
                } catch (e: Exception) {
                    error("Invalid path: ${e.message}")
                }

                // Detect library
                view.targetPathLibrary.value = checkNotNull(libraryService.libraries
                    .asSequence()
                    .mapNotNull { library -> matchPath(library, path) }
                    .maxBy { it.numElements }
                    ?.library
                ) { "Path doesn't belong to any library!" }

                check(game.path.exists()) { "Source path doesn't exist!" }

                check(path != game.path.path) { "Path already exists!" }
                if (path.file.exists()) {
                    // Windows is case insensitive and will report that the file already exists, even if there is a case difference.
                    check(path.file.canonicalFile.path.equals(game.path.canonicalFile.path, ignoreCase = true)) { "Path already exists!" }
                }
            }
            loading /= false
        }

        private fun matchPath(library: Library, path: String): LibraryMatch? {
            val libraryElements = library.path.canonicalPath.split(File.separatorChar)
            val pathElements = path.file.canonicalPath.split(File.separatorChar)
            libraryElements.forEachIndexed { i, element ->
                val pathElement = pathElements.getOrNull(i) ?: return null
                if (pathElement != element) return null
            }
            return LibraryMatch(library, numElements = libraryElements.size)
        }

        private suspend fun onAccept() {
            view.canAccept.assert()

            log.info("Renaming/Moving: ${game.path} -> $targetPath")

            loading /= true
            try {
                taskService.execute(task("Moving ${game.path}...") {
                    fileSystemService.move(from = game.path, to = targetPath.file)
                    executeSubTask(gameService.replace(game, game.rawGame.withMetadata { it.copy(libraryId = targetPathLibrary.id, path = targetPath) }))
                })
                hideView()
            } catch (e: Exception) {
                log.error("Error", e)
                view.onError(e.message!!, "Error!", e)
                validateAndDetectLibrary(targetPath)
            } finally {
                loading /= false
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