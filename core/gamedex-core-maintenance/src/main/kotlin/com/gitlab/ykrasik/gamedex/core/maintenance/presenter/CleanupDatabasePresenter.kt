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

package com.gitlab.ykrasik.gamedex.core.maintenance.presenter

import com.gitlab.ykrasik.gamedex.app.api.maintenance.CleanupDatabaseView
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.maintenance.MaintenanceService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.util.IsValid
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
            view.librariesAndGames.shouldDelete.forEach { onLibrariesAndGamesShouldDeleteChanged() }
            view.images.shouldDelete.forEach { onImagesShouldDeleteChanged() }
            view.fileCache.shouldDelete.forEach { onFileCacheShouldDeleteChanged() }
            view.acceptActions.forEach { onAccept() }
            view.cancelActions.forEach { onCancel() }
        }

        override fun onShow() {
            view.librariesAndGames.canDelete *= IsValid {
                check(view.staleData.libraries.isNotEmpty() || view.staleData.games.isNotEmpty()) { "No stale data to delete!" }
            }
            view.librariesAndGames.shouldDelete *= view.librariesAndGames.canDelete.value.isSuccess

            view.images.canDelete *= IsValid {
                check(view.staleData.images.isNotEmpty()) { "No stale images to delete!" }
            }
            view.images.shouldDelete *= view.images.canDelete.value.isSuccess

            view.fileCache.canDelete *= IsValid {
                check(view.staleData.fileStructure.isNotEmpty()) { "No stale file cache to delete!" }
            }
            view.fileCache.shouldDelete *= view.fileCache.canDelete.value.isSuccess

            setCanAccept()
        }

        private fun onLibrariesAndGamesShouldDeleteChanged() {
            check(view.librariesAndGames.canDelete.value.isSuccess) { "Cannot change 'delete libraries & games' value!" }
            setCanAccept()
        }

        private fun onImagesShouldDeleteChanged() {
            check(view.images.canDelete.value.isSuccess) { "Cannot change 'delete images' value!" }
            setCanAccept()
        }

        private fun onFileCacheShouldDeleteChanged() {
            check(view.fileCache.canDelete.value.isSuccess) { "Cannot change 'delete file cache' value!" }
            setCanAccept()
        }

        private fun setCanAccept() {
            view.canAccept *= IsValid {
                check(view.librariesAndGames.shouldDelete.value || view.images.shouldDelete.value || view.fileCache.shouldDelete.value) {
                    "Please select stale data to delete!"
                }
            }
        }

        private suspend fun onAccept() {
            finished()

            val staleData = view.staleData.copy(
                libraries = if (view.librariesAndGames.shouldDelete.value) view.staleData.libraries else emptyList(),
                games = if (view.librariesAndGames.shouldDelete.value) view.staleData.games else emptyList(),
                images = if (view.images.shouldDelete.value) view.staleData.images else emptyMap(),
                fileStructure = if (view.fileCache.shouldDelete.value) view.staleData.fileStructure else emptyMap()
            )
            taskService.execute(maintenanceService.deleteStaleData(staleData))
        }

        private fun onCancel() {
            finished()
        }

        private fun finished() = eventBus.viewFinished(view)
    }
}