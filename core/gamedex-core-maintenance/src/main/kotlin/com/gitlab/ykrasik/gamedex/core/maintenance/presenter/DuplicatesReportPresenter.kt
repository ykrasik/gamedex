/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.app.api.util.MultiChannel
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.game.GameEvent
import com.gitlab.ykrasik.gamedex.core.maintenance.MaintenanceService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.util.setAll
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
        private val isDirtyChannel = MultiChannel.conflated(true)
        private var isDirty by isDirtyChannel

        init {
            eventBus.forEach<GameEvent> { isDirty = true }

            view.searchText.debounce().forEach { onSearchTextChanged(it) }
            view.hideViewActions.forEach { hideView() }

            isDirtyChannel.forEach { isDirty ->
                if (isDirty && isShowing) {
                    detectDuplicates()
                    this.isDirty = false
                }
            }
        }

        private suspend fun detectDuplicates() {
            val duplicates = taskService.execute(maintenanceService.detectDuplicates())
            view.duplicates.setAll(duplicates)
        }

        override suspend fun onShown() {
            // Send the existing 'isDirty' value to the channel again, to cause the consumer to re-run
            isDirty = isDirty
        }

        private fun onSearchTextChanged(searchText: String) {
            if (searchText.isNotBlank()) {
                view.matchingGame *= view.duplicates.asSequence().map { it.game }.firstOrNull { it.matchesSearchQuery(searchText) }
            }
        }

        // TODO: Do I need the better search capabilities of searchService?
        private fun Game.matchesSearchQuery(query: String) =
            query.isEmpty() || query.split(" ").all { word -> name.contains(word, ignoreCase = true) }

        private fun hideView() = eventBus.requestHideView(view)
    }
}