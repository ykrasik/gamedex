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

package com.gitlab.ykrasik.gamedex.core.game.presenter.delete

import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.app.api.game.DeleteGameView
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.file.FileSystemService
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
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
    private val viewManager: ViewManager
) : Presenter<DeleteGameView> {
    override fun present(view: DeleteGameView) = object : ViewSession() {
        init {
            view.acceptActions.forEach { onAccept() }
            view.cancelActions.forEach { onCancel() }
        }

        override fun onShow() {
            view.fromFileSystem = false
        }

        private suspend fun onAccept() {
            if (view.fromFileSystem) {
                fileSystemService.delete(view.game.path)
            }

            taskService.execute(gameService.delete(view.game))

            close()
        }

        private fun onCancel() {
            close()
        }

        private fun close() = viewManager.closeDeleteGameView(view)
    }
}