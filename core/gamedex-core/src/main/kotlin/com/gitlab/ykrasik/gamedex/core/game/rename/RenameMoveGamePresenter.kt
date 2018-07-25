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

package com.gitlab.ykrasik.gamedex.core.game.rename

import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.app.api.game.RenameMoveGameView
import com.gitlab.ykrasik.gamedex.app.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.core.Presentation
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.api.game.GameService
import com.gitlab.ykrasik.gamedex.core.common.CommonData
import com.gitlab.ykrasik.gamedex.util.logger
import com.gitlab.ykrasik.gamedex.util.toFile
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.withContext
import java.io.File
import java.nio.file.Files
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
    private val commonData: CommonData,
    private val gameService: GameService,
    private val taskRunner: TaskRunner,
    private val viewManager: ViewManager
) : Presenter<RenameMoveGameView> {
    override fun present(view: RenameMoveGameView) = object : Presentation() {
        private val log = logger()

        init {
            commonData.realLibraries.bindTo(view.possibleLibraries)
            view.libraryChanges.subscribeOnUi { validate() }
            view.pathChanges.subscribeOnUi { validate() }
            view.nameChanges.subscribeOnUi { validate() }

            view.selectDirectoryActions.actionOnUi { onSelectDirectory() }
            view.browseToGameActions.actionOnUi { onBrowseToGame() }
            view.acceptActions.actionOnUi { onAccept() }
            view.cancelActions.actionOnUi { onCancel() }
        }

        override fun onShow() {
            val game = view.game
            view.library = game.library
            view.path = game.rawGame.metadata.path.toFile().let { it.parentFile?.path ?: "" }
            view.name = view.initialName ?: game.rawGame.metadata.path.toFile().name
            view.nameValidationError = null
        }

        private fun onSelectDirectory() {
            val initialDirectory = view.library.path.resolve(view.path).let { dir ->
                if (dir.exists()) dir else view.library.path
            }
            val newPath = view.selectDirectory(initialDirectory)
            if (newPath != null) {
                view.path = newPath.relativeTo(view.library.path).path
            }
        }

        private fun onBrowseToGame() = view.browseTo(view.game.path)

        private fun validate() {
            view.nameValidationError = run {
                try {
                    val basePath = view.library.path.resolve(view.path).normalize()
                    val validBasePath = basePath.startsWith(view.library.path) &&
                        commonData.realLibraries.filter { !view.library.path.startsWith(it.path) }.none { basePath.startsWith(it.path) }
                    if (!validBasePath) {
                        return@run "Path is not in library '${view.library.name}'!"
                    }

                    if (view.name.isBlank()) {
                        return@run "Empty name!"
                    }

                    val validName = !view.name.contains(File.separatorChar) && try {
                        Paths.get(basePath.resolve(view.name).toURI())
                        true
                    } catch (e: Exception) {
                        false
                    }
                    if (!validName) {
                        return@run "Invalid name!"
                    }

                    val file = basePath.resolve(view.name)
                    if (file.exists()) {
                        // Windows is case insensitive.
                        if (file.path == view.game.path.path || !file.path.equals(view.game.path.path, ignoreCase = true)) {
                            return@run "Already exists!"
                        }
                    }
                    null
                } catch (e: Exception) {
                    e.message
                }
            }
        }

        private suspend fun onAccept() {
            check(view.nameValidationError == null) { "Cannot accept invalid state!" }
            withContext(CommonPool) {
                val library = view.library
                val game = view.game
                val path = view.path
                val name = view.name
                val newPath = path.toFile().resolve(name)
                val fullPath = library.path.resolve(newPath)
                log.info("Renaming/Moving: ${game.path} -> $fullPath")

                val parent = fullPath.parentFile
                if (parent != library.path && !parent.exists()) {
                    parent.mkdirs()
                }
                if (!game.path.renameTo(fullPath)) {
                    // File.renameTo is case sensitive, but can fail (doesn't cover all move variants).
                    // If it does, retry with Files.move, which is platform-independent (but also case insensitive)
                    // and throws an exception if it fails.
                    Files.move(game.path.toPath(), fullPath.toPath())
                }

                taskRunner.runTask(gameService.replace(game, game.rawGame.withMetadata { it.copy(libraryId = library.id, path = newPath.toString()) }))
            }

            close()
        }

        private fun onCancel() {
            close()
        }

        private fun close() = viewManager.closeRenameMoveGameView(view)
    }
}