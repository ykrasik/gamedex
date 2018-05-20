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

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.game.rename.RenameMoveGameChoice
import com.gitlab.ykrasik.gamedex.app.api.game.rename.RenameMoveGamePresenter
import com.gitlab.ykrasik.gamedex.app.api.game.rename.RenameMoveGamePresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.game.rename.ViewCanRenameMoveGame
import com.gitlab.ykrasik.gamedex.app.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.core.api.game.GameService
import com.gitlab.ykrasik.gamedex.core.launchOnUi
import com.gitlab.ykrasik.gamedex.util.logger
import com.gitlab.ykrasik.gamedex.util.toFile
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.withContext
import java.nio.file.Files
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 16/05/2018
 * Time: 00:39
 */
@Singleton
class RenameMoveGamePresenterFactoryImpl @Inject constructor(
    private val taskRunner: TaskRunner,
    private val gameService: GameService
) : RenameMoveGamePresenterFactory {
    private val log = logger()

    override fun present(view: ViewCanRenameMoveGame) = object : RenameMoveGamePresenter {
        override fun renameMove(game: Game, initialName: String?) = launchOnUi {
            val choice = view.showRenameMoveGameView(game, initialName ?: game.path.name) as? RenameMoveGameChoice.Accept
                ?: return@launchOnUi

            withContext(CommonPool) {
                val (library, path, name) = choice
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
        }
    }
}