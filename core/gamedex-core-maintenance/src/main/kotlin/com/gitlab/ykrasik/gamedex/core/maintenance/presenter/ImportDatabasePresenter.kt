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

import com.gitlab.ykrasik.gamedex.app.api.maintenance.ImportDatabaseView
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.maintenance.ImportDbContent
import com.gitlab.ykrasik.gamedex.core.maintenance.MaintenanceService
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.gitlab.ykrasik.gamedex.util.Try
import com.gitlab.ykrasik.gamedex.util.and
import com.gitlab.ykrasik.gamedex.util.file
import kotlinx.coroutines.launch
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
    private val settingsService: SettingsService,
    private val eventBus: EventBus
) : Presenter<ImportDatabaseView> {
    override fun present(view: ImportDatabaseView) = object : ViewSession() {
        private var importDatabaseFileIsValid by view.importDatabaseFileIsValid
        private var importDatabaseFile by view.importDatabaseFile
        private var canImportLibrary by view.canImportLibrary
        private var shouldImportLibrary by view.shouldImportLibrary
        private var canImportProviderAccounts by view.canImportProviderAccounts
        private var shouldImportProviderAccounts by view.shouldImportProviderAccounts
        private var canImportFilters by view.canImportFilters
        private var shouldImportFilters by view.shouldImportFilters

        private var importDbContent: ImportDbContent? = null

        init {
            view.importDatabaseFile.forEach { onFileChanged() }
            view.shouldImportLibrary.forEach { view.canImportLibrary.assert(); setCanAccept() }
            view.shouldImportProviderAccounts.forEach { view.canImportProviderAccounts.assert(); setCanAccept() }
            view.shouldImportFilters.forEach { view.canImportFilters.assert(); setCanAccept() }
            view.browseActions.forEach { onBrowse() }
            view.acceptActions.forEach { onAccept() }
            view.cancelActions.forEach { onCancel() }
        }

        override suspend fun onShown() {
            importDatabaseFile = ""
            importDbContent = null
            shouldImportLibrary = false
            view.canImportLibrary *= IsValid.valid
            shouldImportProviderAccounts = false
            view.canImportProviderAccounts *= IsValid.valid
            shouldImportFilters = false
            view.canImportFilters *= IsValid.valid
            launch {
                onBrowse()
            }
        }

        private suspend fun onFileChanged() {
            importDatabaseFileIsValid = Try {
                check(importDatabaseFile.isNotBlank()) { "Please enter a path to a file!" }
                check(importDatabaseFile.file.isFile) { "File doesn't exist or is a directory!" }
                importDbContent = taskService.execute(maintenanceService.readImportDbFile(importDatabaseFile.file))
            }
            canImportLibrary = importDatabaseFileIsValid and Try { check(importDbContent!!.db != null) { "No library to import!" } }
            canImportProviderAccounts = importDatabaseFileIsValid and Try { check(importDbContent!!.accounts != null) { "No accounts to import!" } }
            canImportFilters = importDatabaseFileIsValid and Try { check(importDbContent!!.filters != null) { "No filters to import!" } }
            shouldImportLibrary = canImportLibrary.isSuccess
            shouldImportProviderAccounts = canImportProviderAccounts.isSuccess
            shouldImportFilters = canImportFilters.isSuccess

            setCanAccept()
        }

        private suspend fun onBrowse() {
            val file = view.selectImportDatabaseFile(settingsService.general.exportDbDirectory)
            if (file != null) {
                importDatabaseFile = file.absolutePath
            }
            onFileChanged()
        }

        private fun setCanAccept() {
            view.canAccept *= importDatabaseFileIsValid and Try {
                check(shouldImportLibrary || shouldImportProviderAccounts || shouldImportFilters) {
                    "Must import something!"
                }
            }
        }

        private suspend fun onAccept() {
            view.canAccept.assert()
            hideView()

            val file = importDatabaseFile.file
            val content = importDbContent!!.run {
                copy(
                    db = db?.takeIf { shouldImportLibrary },
                    accounts = accounts?.takeIf { shouldImportProviderAccounts },
                    filters = filters?.takeIf { shouldImportFilters }
                )
            }
            taskService.execute(maintenanceService.importDatabase(content))
            settingsService.general.modify { copy(exportDbDirectory = file.parentFile) }
        }

        private fun onCancel() {
            hideView()
        }

        private fun hideView() = eventBus.requestHideView(view)
    }
}