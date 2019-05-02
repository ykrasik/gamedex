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

import com.gitlab.ykrasik.gamedex.app.api.game.RenameMoveGameView
import com.gitlab.ykrasik.gamedex.core.CommonData
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.file.FileSystemService
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.util.Try
import com.gitlab.ykrasik.gamedex.util.file
import com.gitlab.ykrasik.gamedex.util.logger
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
    private val eventBus: EventBus
) : Presenter<RenameMoveGameView> {
    override fun present(view: RenameMoveGameView) = object : ViewSession() {
        private val log = logger()
        private var library by view.library
        private var path by view.path
        private var name by view.name

        init {
            commonData.contentLibraries.bind(view.possibleLibraries)
            view.library.forEach { validate() }
            view.path.forEach { validate() }
            view.name.forEach { validate() }

            view.selectDirectoryActions.forEach { onSelectDirectory() }
            view.acceptActions.forEach { onAccept() }
            view.cancelActions.forEach { onCancel() }
        }

        override suspend fun onShow() {
            val game = view.game
            library = game.library
            name = view.initialName ?: game.rawGame.metadata.path.file.name
            path = game.rawGame.metadata.path.file.let { it.parentFile?.path ?: "" }
            validate()
        }

        private fun onSelectDirectory() {
            val initialDirectory = library.path.resolve(path).let { dir ->
                if (dir.exists()) dir else library.path
            }
            val newPath = view.selectDirectory(initialDirectory)
            if (newPath != null) {
                path = newPath.relativeTo(library.path).path
            }
        }

        private fun validate() {
            view.nameIsValid *= Try {
                val basePath = library.path.resolve(path).normalize()
                val validBasePath = basePath.startsWith(library.path) &&
                    (commonData.contentLibraries - library).none { basePath.startsWith(it.path) }
                if (!validBasePath) {
                    error("Path is not in library '${library.name}'!")
                }

                if (name.isBlank()) {
                    error("Empty name!")
                }

                check(!name.contains(File.separatorChar)) { "Invalid name!" }
                try {
                    Paths.get(basePath.resolve(name).toURI())
                } catch (_: Exception) {
                    error("Invalid name!")
                }

                val file = basePath.resolve(name)
                if (file.exists()) {
                    // Windows is case insensitive.
                    val gamePath = view.game.path.path
                    if (file.path == gamePath || !file.path.equals(gamePath, ignoreCase = true)) {
                        error("Already exists!")
                    }
                }
            }
            view.canAccept *= view.nameIsValid.value
        }

        private suspend fun onAccept() {
            view.canAccept.assert()
            val game = view.game
            val newPath = path.file.resolve(name)
            val fullPath = library.path.resolve(newPath)
            log.info("Renaming/Moving: ${game.path} -> $fullPath")

            fileSystemService.move(from = game.path, to = fullPath)

            taskService.execute(gameService.replace(game, game.rawGame.withMetadata { it.copy(libraryId = library.id, path = newPath.toString()) }))

            finished()
        }

        private fun onCancel() {
            finished()
        }

        private fun finished() = eventBus.viewFinished(view)
    }
}