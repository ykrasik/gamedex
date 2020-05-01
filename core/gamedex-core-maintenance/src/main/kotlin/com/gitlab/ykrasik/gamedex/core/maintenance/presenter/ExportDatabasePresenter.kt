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
import kotlinx.coroutines.launch
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
        private var exportDatabaseDirectory by view.exportDatabaseDirectory
        private var exportDatabaseFolderIsValid by view.exportDatabaseFolderIsValid
        private var shouldExportLibrary by view.shouldExportLibrary
        private var shouldExportProviderAccounts by view.shouldExportProviderAccounts
        private var shouldExportFilters by view.shouldExportFilters

        init {
            view.exportDatabaseDirectory.forEach { onDirectoryChanged() }
            view.shouldExportLibrary.forEach { setCanAccept() }
            view.shouldExportProviderAccounts.forEach { setCanAccept() }
            view.shouldExportFilters.forEach { setCanAccept() }
            view.browseActions.forEach { onBrowse() }
            view.acceptActions.forEach { onAccept() }
            view.cancelActions.forEach { onCancel() }
        }

        override suspend fun onShown() {
            exportDatabaseDirectory = ""
            shouldExportLibrary = true
            shouldExportProviderAccounts = true
            shouldExportFilters = true
            launch {
                onBrowse()
            }
        }

        private fun onDirectoryChanged() {
            exportDatabaseFolderIsValid = Try {
                check(exportDatabaseDirectory.isNotBlank()) { "Please enter a path to a directory!" }
                check(exportDatabaseDirectory.file.isDirectory) { "Directory doesn't exist!" }
            }
            setCanAccept()
        }

        private fun onBrowse() {
            val dir = view.selectExportDatabaseDirectory(settingsRepo.exportDbDirectory)
            if (dir != null) {
                exportDatabaseDirectory = dir.absolutePath
            }
            onDirectoryChanged()
        }

        private fun setCanAccept() {
            view.canAccept *= exportDatabaseFolderIsValid and Try {
                check(shouldExportLibrary || shouldExportProviderAccounts || shouldExportFilters) {
                    "Must export something!"
                }
            }
        }

        private suspend fun onAccept() {
            view.canAccept.assert()
            hideView()

            val params = ImportExportParams(
                library = shouldExportLibrary,
                providerAccounts = shouldExportProviderAccounts,
                filters = shouldExportFilters
            )
            val dir = exportDatabaseDirectory.file
            taskService.execute(maintenanceService.exportDatabase(dir, params))
            settingsRepo.exportDbDirectory = dir
            view.openDirectory(dir)
        }

        private fun onCancel() {
            hideView()
        }

        private fun hideView() = eventBus.requestHideView(view)
    }
}