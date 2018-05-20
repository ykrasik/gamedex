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

package com.gitlab.ykrasik.gamedex.core.game.common

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.game.common.DeleteGameChoice
import com.gitlab.ykrasik.gamedex.app.api.game.common.DeleteGamePresenter
import com.gitlab.ykrasik.gamedex.app.api.game.common.DeleteGamePresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.game.common.ViewCanDeleteGame
import com.gitlab.ykrasik.gamedex.app.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.core.api.game.GameService
import com.gitlab.ykrasik.gamedex.core.launchOnUi
import com.gitlab.ykrasik.gamedex.util.deleteWithChildren
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 02/05/2018
 * Time: 10:46
 */
@Singleton
class DeleteGamePresenterFactoryImpl @Inject constructor(
    private val taskRunner: TaskRunner,
    private val gameService: GameService
) : DeleteGamePresenterFactory {
    override fun present(view: ViewCanDeleteGame): DeleteGamePresenter = object : DeleteGamePresenter {
        override fun deleteGame(game: Game) = launchOnUi {
            val choice = view.showConfirmDeleteGame(game)
            val (confirm, fromFileSystem) = when (choice) {
                is DeleteGameChoice.Confirm -> Pair(true, choice.fromFileSystem)
                DeleteGameChoice.Cancel -> Pair(false, false)
            }

            if (confirm) {
                if (fromFileSystem) {
                    game.path.deleteWithChildren()
                }

                taskRunner.runTask(gameService.delete(game))
            }
        }
    }
}