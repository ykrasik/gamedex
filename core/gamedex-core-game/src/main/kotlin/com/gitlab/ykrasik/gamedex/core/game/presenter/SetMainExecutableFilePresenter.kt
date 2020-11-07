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

package com.gitlab.ykrasik.gamedex.core.game.presenter

import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanSetMainExecutableFile
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.core.view.Presenter
import com.gitlab.ykrasik.gamedex.core.view.ViewSession
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 01/05/2020
 * Time: 15:43
 */
@Singleton
class SetMainExecutableFilePresenter @Inject constructor(
    private val gameService: GameService,
    private val taskService: TaskService,
) : Presenter<ViewCanSetMainExecutableFile> {
    override fun present(view: ViewCanSetMainExecutableFile) = object : ViewSession() {
        init {
            view::setMainExecutableFileActions.forEach { (game, absoluteExecutablePath) ->
                val relativeExecutablePath = if (absoluteExecutablePath != null) {
                    check(!absoluteExecutablePath.isDirectory) { "Main Executable must not be a directory!" }
                    checkNotNull(absoluteExecutablePath.relativeToOrNull(game.path)?.toString()) { "Main Executable must be under the game's path!" }
                } else {
                    null
                }
                val updatedGame = game.rawGame.copy(userData = game.userData.copy(mainExecutablePath = relativeExecutablePath))
                taskService.execute(gameService.replace(game, updatedGame))
            }
        }
    }
}