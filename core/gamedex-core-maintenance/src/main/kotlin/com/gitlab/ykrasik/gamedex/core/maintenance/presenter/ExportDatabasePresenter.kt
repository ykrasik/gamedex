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

import com.gitlab.ykrasik.gamedex.app.api.maintenance.ExportDatabaseView
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.maintenance.ImportExportParams
import com.gitlab.ykrasik.gamedex.core.maintenance.MaintenanceService
import com.gitlab.ykrasik.gamedex.core.settings.GeneralSettingsRepository
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.util.Try
import com.gitlab.ykrasik.gamedex.util.and
import com.gitlab.ykrasik.gamedex.util.file
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/05/2018
 * Time: 13:21
 */
@Singleton
class ExportDatabasePresenter @Inject constructor(
    private val maintenanceService: MaintenanceService,
    private val taskService: TaskService,
    private val settingsRepo: GeneralSettingsRepository,
    private val eventBus: EventBus
) : Presenter<ExportDatabaseView> {
    override fun present(view: ExportDatabaseView) = object : ViewSession() {
        init {
            view.exportDatabaseDirectory.mapTo(view.exportDatabaseFolderIsValid) { exportDatabaseDirectory ->
                Try {
                    check(exportDatabaseDirectory.isNotBlank()) { "Select export directory!" }
                    check(exportDatabaseDirectory.file.isDirectory) { "Directory doesn't exist!" }
                }
            }

            view.exportDatabaseFolderIsValid.combineLatest(
                view.shouldExportLibrary,
                view.shouldExportProviderAccounts,
                view.shouldExportFilters
            ) { exportDatabaseDirectoryIsValid, shouldExportLibrary, shouldExportProviderAccounts, shouldExportFilters ->
                view.canAccept *= exportDatabaseDirectoryIsValid and Try {
                    check(shouldExportLibrary || shouldExportProviderAccounts || shouldExportFilters) { "Select something to export!" }
                }
            }

            view.browseActions.forEach { onBrowse() }
            view.acceptActions.forEach { onAccept() }
            view.cancelActions.forEach { onCancel() }
        }

        override suspend fun onShown() {
            view.exportDatabaseDirectory *= ""
            view.shouldExportLibrary *= true
            view.shouldExportProviderAccounts *= true
            view.shouldExportFilters *= true
            onBrowse()
        }

        private fun onBrowse() {
            val dir = view.selectExportDatabaseDirectory(settingsRepo.exportDbDirectory.value)
            if (dir != null) {
                view.exportDatabaseDirectory *= dir.absolutePath
            }
        }

        private suspend fun onAccept() {
            view.canAccept.assert()
            hideView()

            val params = ImportExportParams(
                library = view.shouldExportLibrary.value,
                providerAccounts = view.shouldExportProviderAccounts.value,
                filters = view.shouldExportFilters.value
            )
            val dir = view.exportDatabaseDirectory.value.file
            taskService.execute(maintenanceService.exportDatabase(dir, params))
            settingsRepo.exportDbDirectory *= dir
            view.openDirectory(dir)
        }

        private fun onCancel() {
            hideView()
        }

        private fun hideView() = eventBus.requestHideView(view)
    }
}