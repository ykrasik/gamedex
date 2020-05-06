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

import com.gitlab.ykrasik.gamedex.app.api.maintenance.ImportDatabaseView
import com.gitlab.ykrasik.gamedex.app.api.util.conflatedChannel
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.maintenance.ImportDbContent
import com.gitlab.ykrasik.gamedex.core.maintenance.MaintenanceService
import com.gitlab.ykrasik.gamedex.core.settings.GeneralSettingsRepository
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.util.Try
import com.gitlab.ykrasik.gamedex.util.and
import com.gitlab.ykrasik.gamedex.util.existsOrNull
import com.gitlab.ykrasik.gamedex.util.file
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/05/2018
 * Time: 13:23
 */
@Singleton
class ImportDatabasePresenter @Inject constructor(
    private val maintenanceService: MaintenanceService,
    private val taskService: TaskService,
    private val settingsRepo: GeneralSettingsRepository,
    private val eventBus: EventBus
) : Presenter<ImportDatabaseView> {
    override fun present(view: ImportDatabaseView) = object : ViewSession() {
        private val importDbContent = conflatedChannel<ImportDbContent?>(null)

        init {
            view.importDatabaseFile.mapTo(view.importDatabaseFileIsValid) { importDatabaseFile ->
                Try {
                    check(importDatabaseFile.isNotBlank()) { "Enter a path to a file!" }
                    check(importDatabaseFile.file.isFile) { "File doesn't exist or is a directory!" }
                }
            }
            view.importDatabaseFileIsValid.forEach { importDatabaseFileIsValid ->
                importDbContent *= if (importDatabaseFileIsValid.isSuccess) {
                    taskService.execute(maintenanceService.readImportDbFile(view.importDatabaseFile.value.file))
                } else {
                    null
                }
                view.canImportLibrary *= importDatabaseFileIsValid and Try { check(importDbContent.value!!.db != null) { "No library to import!" } }
                view.shouldImportLibrary *= view.canImportLibrary.value.isSuccess

                view.canImportProviderAccounts *= importDatabaseFileIsValid and Try { check(importDbContent.value!!.accounts != null) { "No accounts to import!" } }
                view.shouldImportProviderAccounts *= view.canImportProviderAccounts.value.isSuccess

                view.canImportFilters *= importDatabaseFileIsValid and Try { check(importDbContent.value!!.filters != null) { "No filters to import!" } }
                view.shouldImportFilters *= view.canImportFilters.value.isSuccess
            }

            view.importDatabaseFileIsValid.combineLatest(
                view.shouldImportLibrary,
                view.shouldImportProviderAccounts,
                view.shouldImportFilters
            ) { importDatabaseFileIsValid, shouldImportLibrary, shouldImportProviderAccounts, shouldImportFilters ->
                if (shouldImportLibrary) view.canImportLibrary.assert()
                if (shouldImportProviderAccounts) view.canImportProviderAccounts.assert()
                if (shouldImportFilters) view.canImportFilters.assert()
                view.canAccept *= importDatabaseFileIsValid and Try {
                    check(shouldImportLibrary || shouldImportProviderAccounts || shouldImportFilters) { "Must import something!" }
                }
            }

            view.browseActions.forEach { onBrowse() }
            view.acceptActions.forEach { onAccept() }
            view.cancelActions.forEach { onCancel() }
        }

        override suspend fun onShown() {
            view.importDatabaseFile *= ""
            onBrowse()
        }

        private fun onBrowse() {
            val file = view.selectImportDatabaseFile(settingsRepo.exportDbDirectory.value.existsOrNull())
            if (file != null) {
                view.importDatabaseFile *= file.absolutePath
            }
        }

        private suspend fun onAccept() {
            view.canAccept.assert()
            hideView()

            val file = view.importDatabaseFile.value.file
            val content = importDbContent.value!!.run {
                copy(
                    db = db?.takeIf { view.shouldImportLibrary.value },
                    accounts = accounts?.takeIf { view.shouldImportProviderAccounts.value },
                    filters = filters?.takeIf { view.shouldImportFilters.value }
                )
            }
            taskService.execute(maintenanceService.importDatabase(content))
            settingsRepo.exportDbDirectory *= file.parentFile
        }

        private fun onCancel() {
            hideView()
        }

        private fun hideView() = eventBus.requestHideView(view)
    }
}