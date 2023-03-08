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

package com.gitlab.ykrasik.gamedex.core.maintenance.presenter

import com.gitlab.ykrasik.gamedex.app.api.maintenance.CleanupDatabaseView
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.maintenance.MaintenanceService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.core.view.Presenter
import com.gitlab.ykrasik.gamedex.core.view.ViewSession
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/05/2018
 * Time: 13:17
 */
@Singleton
class CleanupDatabasePresenter @Inject constructor(
    private val maintenanceService: MaintenanceService,
    private val taskService: TaskService,
    private val eventBus: EventBus,
) : Presenter<CleanupDatabaseView> {
    override fun present(view: CleanupDatabaseView) = object : ViewSession() {
        init {
            this::isShowing.forEach {
                if (it) {
                    view.movedGamesToFix /= view.cleanupData.v.movedGames
                }
            }

            view.isDeleteLibrariesAndGames *= isShowing withDebugName "isDeleteLibrariesAndGames"
            view.isDeleteImages *= isShowing withDebugName "isDeleteImages"
            view.isDeleteFileCache *= isShowing withDebugName "isDeleteFileCache"

            view::acceptActions.forEach { onAccept() }
            view::cancelActions.forEach { onCancel() }
        }

        private suspend fun onAccept() {
            view.canAccept.assert()

            hideView()

            val cleanupData = view.cleanupData.v
            val cleanupDataFix = cleanupData.copy(
                movedGames = view.movedGamesToFix.v,
                missingLibraries = if (view.isDeleteLibrariesAndGames.v) cleanupData.missingLibraries else emptyList(),
                missingGames = if (view.isDeleteLibrariesAndGames.v) cleanupData.missingGames else emptyList(),
                staleImages = if (view.isDeleteImages.v) cleanupData.staleImages else emptyMap(),
                staleFileTrees = if (view.isDeleteFileCache.v) cleanupData.staleFileTrees else emptyMap()
            )
            if (!cleanupDataFix.isEmpty) {
                taskService.execute(maintenanceService.fixCleanupData(cleanupDataFix))
            }
        }

        private fun onCancel() {
            hideView()
        }

        private fun hideView() = eventBus.requestHideView(view)
    }
}