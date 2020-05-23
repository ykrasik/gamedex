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

package com.gitlab.ykrasik.gamedex.core.maintenance.presenter

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.maintenance.DuplicatesView
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.flowOf
import com.gitlab.ykrasik.gamedex.core.game.GameEvent
import com.gitlab.ykrasik.gamedex.core.maintenance.MaintenanceService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 26/04/2019
 * Time: 09:06
 */
@Singleton
class DuplicatesReportPresenter @Inject constructor(
    private val maintenanceService: MaintenanceService,
    private val taskService: TaskService,
    private val eventBus: EventBus
) : Presenter<DuplicatesView> {
    override fun present(view: DuplicatesView) = object : ViewSession() {
        private val isDirty = MutableStateFlow(true)
        private val shouldRun = isDirty and isShowing

        init {
            eventBus.flowOf<GameEvent>().forEach(debugName = "onGameEvent") { isDirty /= true }

            view::matchingGame *= view.searchText.onlyChangesFromView().debounce(100).map { searchText ->
                if (searchText.isNotBlank()) {
                    view.duplicates.asSequence().map { it.game }.firstOrNull { it.matchesSearchQuery(searchText) }
                } else {
                    null
                }
            }
            view::hideViewActions.forEach { hideView() }

            this::shouldRun.forEach {
                if (it) {
                    detectDuplicates()
                    isDirty /= false
                }
            }
        }

        private suspend fun detectDuplicates() {
            view.duplicates /= taskService.execute(maintenanceService.detectDuplicates())
        }

        // TODO: Do I need the better search capabilities of searchService?
        private fun Game.matchesSearchQuery(query: String) =
            query.isEmpty() || query.split(" ").all { word -> name.contains(word, ignoreCase = true) }

        private fun hideView() = eventBus.requestHideView(view)
    }
}