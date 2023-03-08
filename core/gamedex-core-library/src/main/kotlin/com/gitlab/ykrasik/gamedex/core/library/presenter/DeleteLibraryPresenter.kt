/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.core.library.presenter

import com.gitlab.ykrasik.gamedex.app.api.library.DeleteLibraryView
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.core.view.Presenter
import com.gitlab.ykrasik.gamedex.core.view.ViewSession
import kotlinx.coroutines.flow.map
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
    private val taskService: TaskService,
    private val eventBus: EventBus,
) : Presenter<DeleteLibraryView> {
    override fun present(view: DeleteLibraryView) = object : ViewSession() {
        init {
            view::gamesToBeDeleted *= view.library.onlyChangesFromView().map { library ->
                gameService.games.filter { it.library.id == library.id }
            }
            view::acceptActions.forEach { onAccept() }
            view::cancelActions.forEach { onCancel() }
        }

        private suspend fun onAccept() {
            taskService.execute(libraryService.delete(view.library.v))

            hideView()
        }

        private fun onCancel() {
            hideView()
        }

        private fun hideView() = eventBus.requestHideView(view)
    }
}