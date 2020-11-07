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

package com.gitlab.ykrasik.gamedex.core.provider.presenter

import com.gitlab.ykrasik.gamedex.app.api.provider.BulkUpdateGamesView
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.provider.BulkUpdateGamesFilterRepository
import com.gitlab.ykrasik.gamedex.core.provider.UpdateGameService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.core.view.Presenter
import com.gitlab.ykrasik.gamedex.core.view.ViewSession
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/05/2018
 * Time: 13:08
 */
@Singleton
class BulkUpdateGamesPresenter @Inject constructor(
    private val repo: BulkUpdateGamesFilterRepository,
    private val updateGameService: UpdateGameService,
    private val taskService: TaskService,
    private val eventBus: EventBus,
) : Presenter<BulkUpdateGamesView> {
    override fun present(view: BulkUpdateGamesView) = object : ViewSession() {
        init {
            // Set view filter from repo each time view is shown or hidden
            view.bulkUpdateGamesFilter *= repo.bulkUpdateGamesFilter.combine(isShowing) { filter, _ -> filter } withDebugName "bulkUpdateGamesFilter"
            view::canAccept *= view.bulkUpdateGamesFilterValidatedValue.allValues().map { it.isValid }
            view::acceptActions.forEach { onAccept() }
            view::cancelActions.forEach { onCancel() }
        }

        private suspend fun onAccept() {
            view.canAccept.assert()

            val filter = view.bulkUpdateGamesFilterValidatedValue.v.value
            repo.update(filter)

            hideView()

            taskService.execute(updateGameService.bulkUpdateGames(filter))
        }

        private fun onCancel() {
            hideView()
        }

        private fun hideView() = eventBus.requestHideView(view)
    }
}