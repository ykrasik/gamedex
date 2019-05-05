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
import com.gitlab.ykrasik.gamedex.app.api.maintenance.FolderNameDiffView
import com.gitlab.ykrasik.gamedex.app.api.util.BroadcastEventChannel
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
 * Date: 27/04/2019
 * Time: 13:34
 */
@Singleton
class FolderNameDiffReportPresenter @Inject constructor(
    private val maintenanceService: MaintenanceService,
    private val taskService: TaskService,
    private val eventBus: EventBus
) : Presenter<FolderNameDiffView> {
    override fun present(view: FolderNameDiffView) = object : ViewSession() {
        private val isDirtyChannel = BroadcastEventChannel.conflated(true)
        private var isDirty by isDirtyChannel

        init {
            eventBus.forEach<GameEvent> { isDirty = true }

            view.searchText.debounce().forEach { onSearchTextChanged(it) }
            view.hideViewActions.forEach { finished() }

            isDirtyChannel.forEach { isDirty ->
                if (isDirty && isShowing) {
                    detectFolderNameDiffs()
                    this.isDirty = false
                }
            }
        }

        private suspend fun detectFolderNameDiffs() {
            val folderNameDiffs = taskService.execute(maintenanceService.detectFolderNameDiffs())
            view.diffs.setAll(folderNameDiffs)
        }

        override suspend fun onShown() {
            // Send the existing 'isDirty' value to the channel again, to cause the consumer to re-run
            isDirty = isDirty
        }

        private fun onSearchTextChanged(searchText: String) {
            if (searchText.isNotBlank()) {
                view.matchingGame *= view.diffs.asSequence().map { it.game }.firstOrNull { it.matchesSearchQuery(searchText) }
            }
        }

        // TODO: Do I need the better search capabilities of searchService?
        private fun Game.matchesSearchQuery(query: String) =
            query.isEmpty() || query.split(" ").all { word -> name.contains(word, ignoreCase = true) }

        private fun finished() = eventBus.viewFinished(view)
    }
}