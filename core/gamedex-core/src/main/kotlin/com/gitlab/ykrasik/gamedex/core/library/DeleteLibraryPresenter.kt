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

package com.gitlab.ykrasik.gamedex.core.library

import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.app.api.library.DeleteLibraryView
import com.gitlab.ykrasik.gamedex.app.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.core.Presentation
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.api.game.GameService
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/05/2018
 * Time: 13:30
 */
@Singleton
class DeleteLibraryPresenter @Inject constructor(
    private val libraryService: LibraryService,
    private val gameService: GameService,
    private val taskRunner: TaskRunner,
    private val viewManager: ViewManager
) : Presenter<DeleteLibraryView> {
    override fun present(view: DeleteLibraryView) = object : Presentation() {
        init {
            view.acceptActions.forEach { onAccept() }
            view.cancelActions.forEach { onCancel() }
        }

        override fun onShow() {
            view.gamesToBeDeleted.clear()
            view.gamesToBeDeleted += gameService.games.filter { it.library.id == view.library.id }
        }

        private suspend fun onAccept() {
            taskRunner.runTask(libraryService.delete(view.library))

            close()
        }

        private fun onCancel() {
            close()
        }

        private fun close() = viewManager.closeDeleteLibraryView(view)
    }
}