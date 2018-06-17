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

package com.gitlab.ykrasik.gamedex.core.game.delete

import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.app.api.game.DeleteGameView
import com.gitlab.ykrasik.gamedex.app.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.core.Presentation
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.api.game.GameService
import com.gitlab.ykrasik.gamedex.util.deleteWithChildren
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 02/05/2018
 * Time: 10:46
 */
@Singleton
class DeleteGamePresenter @Inject constructor(
    private val taskRunner: TaskRunner,
    private val gameService: GameService,
    private val viewManager: ViewManager
) : Presenter<DeleteGameView> {
    override fun present(view: DeleteGameView) = object : Presentation() {
        init {
            view.acceptActions.actionOnUi { onAccept() }
            view.cancelActions.actionOnUi { onCancel() }
        }

        override fun onShow() {
            view.fromFileSystem = false
        }

        private suspend fun onAccept() {
            if (view.fromFileSystem) {
                withContext(CommonPool) {
                    view.game.path.deleteWithChildren()
                }
            }

            taskRunner.runTask(gameService.delete(view.game))

            close()
        }

        private fun onCancel() {
            close()
        }

        private fun close() = viewManager.closeDeleteGameView(view)
    }
}