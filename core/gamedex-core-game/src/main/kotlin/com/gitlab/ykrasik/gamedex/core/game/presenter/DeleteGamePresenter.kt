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

import com.gitlab.ykrasik.gamedex.app.api.game.DeleteGameView
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.file.FileSystemService
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.core.view.Presenter
import com.gitlab.ykrasik.gamedex.core.view.ViewSession
import com.gitlab.ykrasik.gamedex.util.IsValid
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 02/05/2018
 * Time: 10:46
 */
@Singleton
class DeleteGamePresenter @Inject constructor(
    private val taskService: TaskService,
    private val gameService: GameService,
    private val fileSystemService: FileSystemService,
    private val eventBus: EventBus
) : Presenter<DeleteGameView> {
    override fun present(view: DeleteGameView) = object : ViewSession() {
        private val game by view.game

        init {
            this::isShowing.forEach {
                view.fromFileSystem /= false
            }
            // TODO: At the time of writing, view.game.onlyValuesFromView() incorrectly skipped the first changed value
            view::canAccept *= view.game.allValues().combine(view.fromFileSystem.allValues()) { game, fromFileSystem ->
                IsValid {
                    if (fromFileSystem) check(game.path.exists()) { "Path doesn't exist!" }
                }
            }
            view::acceptActions.forEach { onAccept() }
            view::cancelActions.forEach { onCancel() }
        }

        private suspend fun onAccept() {
            view.canAccept.assert()

            if (view.fromFileSystem.v) {
                if (game.path.exists()) {
                    fileSystemService.delete(game.path)
                } else {
                    view.onError(game.path.toString(), title = "File doesn't exist!")
                    return
                }
            }

            taskService.execute(gameService.delete(game))

            hideView()
        }

        private fun onCancel() {
            hideView()
        }

        private fun hideView() = eventBus.requestHideView(view)
    }
}