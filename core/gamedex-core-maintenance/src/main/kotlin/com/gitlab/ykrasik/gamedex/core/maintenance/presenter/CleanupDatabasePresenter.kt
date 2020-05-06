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

import com.gitlab.ykrasik.gamedex.app.api.maintenance.CleanupDatabaseView
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.maintenance.MaintenanceService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.util.Try
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
    private val eventBus: EventBus
) : Presenter<CleanupDatabaseView> {
    override fun present(view: CleanupDatabaseView) = object : ViewSession() {
        init {
            view.staleData.mapTo(view.librariesAndGames.canDelete) { staleData ->
                Try { check(staleData.libraries.isNotEmpty() || staleData.games.isNotEmpty()) { "No stale libraries or games to delete!" } }
            }
            view.librariesAndGames.canDelete.mapTo(view.librariesAndGames.shouldDelete) { it.isSuccess }

            view.staleData.mapTo(view.images.canDelete) { staleData ->
                Try { check(staleData.images.isNotEmpty()) { "No stale images to delete!" } }
            }
            view.images.canDelete.mapTo(view.images.shouldDelete) { it.isSuccess }

            view.staleData.mapTo(view.fileCache.canDelete) { staleData ->
                Try { check(staleData.fileTrees.isNotEmpty()) { "No stale images to delete!" } }
            }
            view.fileCache.canDelete.mapTo(view.fileCache.shouldDelete) { it.isSuccess }

            view.librariesAndGames.shouldDelete.combineLatest(view.images.shouldDelete, view.fileCache.shouldDelete) { deleteLibrariesAndGames, deleteImages, deleteFileCache ->
                view.canAccept *= Try {
                    if (deleteLibrariesAndGames) view.librariesAndGames.canDelete.assert()
                    if (deleteImages) view.images.canDelete.assert()
                    if (deleteFileCache) view.fileCache.canDelete.assert()
                    check(deleteLibrariesAndGames || deleteImages || deleteFileCache) { "Select stale data to delete!" }
                }
            }

            view.acceptActions.forEach { onAccept() }
            view.cancelActions.forEach { onCancel() }
        }

        private suspend fun onAccept() {
            hideView()

            val staleData = view.staleData.value
            val staleDataToDelete = staleData.copy(
                libraries = if (view.librariesAndGames.shouldDelete.value) staleData.libraries else emptyList(),
                games = if (view.librariesAndGames.shouldDelete.value) staleData.games else emptyList(),
                images = if (view.images.shouldDelete.value) staleData.images else emptyMap(),
                fileTrees = if (view.fileCache.shouldDelete.value) staleData.fileTrees else emptyMap()
            )
            taskService.execute(maintenanceService.deleteStaleData(staleDataToDelete))
        }

        private fun onCancel() {
            hideView()
        }

        private fun hideView() = eventBus.requestHideView(view)
    }
}