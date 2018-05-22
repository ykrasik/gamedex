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
import com.gitlab.ykrasik.gamedex.app.api.game.delete.DeleteGamePresenter
import com.gitlab.ykrasik.gamedex.app.api.game.delete.DeleteGamePresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.game.delete.DeleteGameView
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
    override fun present(view: DeleteGameView): DeleteGamePresenter = object : DeleteGamePresenter {
        override fun onGameChanged(game: Game) {
            view.fromFileSystem = false
        }

        override fun onFromFileSystemChanged(fromFileSystem: Boolean) {
        }

        override fun onAccept() = launchOnUi {
            if (view.fromFileSystem) {
                view.game.path.deleteWithChildren()
            }

            taskRunner.runTask(gameService.delete(view.game))

            view.closeView()
        }

        override fun onCancel() {
            view.closeView()
        }
    }
}