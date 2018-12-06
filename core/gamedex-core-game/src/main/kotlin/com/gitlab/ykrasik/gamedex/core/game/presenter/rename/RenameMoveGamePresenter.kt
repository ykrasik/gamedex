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

package com.gitlab.ykrasik.gamedex.core.game.presenter.rename

import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.app.api.game.RenameMoveGameView
import com.gitlab.ykrasik.gamedex.app.api.util.IsValid
import com.gitlab.ykrasik.gamedex.core.CommonData
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.file.FileSystemService
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.util.logger
import com.gitlab.ykrasik.gamedex.util.toFile
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
    private val commonData: CommonData,
    private val gameService: GameService,
    private val fileSystemService: FileSystemService,
    private val taskService: TaskService,
    private val viewManager: ViewManager
) : Presenter<RenameMoveGameView> {
    override fun present(view: RenameMoveGameView) = object : ViewSession() {
        private val log = logger()

        init {
            commonData.realLibraries.bindTo(view.possibleLibraries)
            view.libraryChanges.forEach { validate() }
            view.pathChanges.forEach { validate() }
            view.nameChanges.forEach { validate() }

            view.selectDirectoryActions.forEach { onSelectDirectory() }
            view.browseToGameActions.forEach { onBrowseToGame() }
            view.acceptActions.forEach { onAccept() }
            view.cancelActions.forEach { onCancel() }
        }

        override fun onShow() {
            val game = view.game
            view.library = game.library
            view.path = game.rawGame.metadata.path.toFile().let { it.parentFile?.path ?: "" }
            view.name = view.initialName ?: game.rawGame.metadata.path.toFile().name
            validate()
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
            view.nameIsValid = IsValid.invoke {
                val basePath = view.library.path.resolve(view.path).normalize()
                val validBasePath = basePath.startsWith(view.library.path) &&
                    (commonData.realLibraries - view.library).none { basePath.startsWith(it.path) }
                if (!validBasePath) {
                    error("Path is not in library '${view.library.name}'!")
                }

                if (view.name.isBlank()) {
                    error("Empty name!")
                }

                check(!view.name.contains(File.separatorChar)) { "Invalid name!" }
                try {
                    Paths.get(basePath.resolve(view.name).toURI())
                } catch (_: Exception) {
                    error("Invalid name!")
                }

                val file = basePath.resolve(view.name)
                if (file.exists()) {
                    // Windows is case insensitive.
                    if (file.path == view.game.path.path || !file.path.equals(view.game.path.path, ignoreCase = true)) {
                        error("Already exists!")
                    }
                }
            }
            view.canAccept = view.nameIsValid
        }

        private suspend fun onAccept() {
            check(view.canAccept.isSuccess) { "Cannot accept invalid state!" }
            val library = view.library
            val game = view.game
            val newPath = view.path.toFile().resolve(view.name)
            val fullPath = library.path.resolve(newPath)
            log.info("Renaming/Moving: ${game.path} -> $fullPath")

            fileSystemService.move(from = game.path, to = fullPath)

            taskService.execute(gameService.replace(game, game.rawGame.withMetadata { it.copy(libraryId = library.id, path = newPath.toString()) }))

            close()
        }

        private fun onCancel() {
            close()
        }

        private fun close() = viewManager.closeRenameMoveGameView(view)
    }
}