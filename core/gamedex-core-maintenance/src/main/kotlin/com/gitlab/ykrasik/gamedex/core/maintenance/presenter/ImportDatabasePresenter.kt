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
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.maintenance.ImportDbContent
import com.gitlab.ykrasik.gamedex.core.maintenance.MaintenanceService
import com.gitlab.ykrasik.gamedex.core.settings.GeneralSettingsRepository
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
        val importDbContent = MutableStateFlow(Try.error<ImportDbContent>(IllegalArgumentException("Empty")))

        init {
            isShowing.forEach(debugName = "onShow") {
                if (it) {
                    view.importDatabaseFile *= ""
                    onBrowse()
                }
            }

            // TODO: importDbContent *= view.importDatabaseFile.allValues().map {... throws compiler exceptions
            view.importDatabaseFile.allValues().forEach(debugName = "onImportDatabaseFileChanged") { importDatabaseFile ->
                importDbContent *= Try {
                    check(importDatabaseFile.isNotBlank()) { "Enter a path to a file!" }
                    check(importDatabaseFile.file.isFile) { "File doesn't exist or is a directory!" }
                    taskService.execute(maintenanceService.readImportDbFile(importDatabaseFile.file))
                }
            }

            view.importDatabaseFileIsValid *= importDbContent.map { it.map { Unit } } withDebugName "importDatabaseFileIsValid"

            view.canImportLibrary *= importDbContent.map { it.map { check(it.db != null) { "No library to import!" } } } withDebugName "canImportLibrary"
            view.shouldImportLibrary *= view.canImportLibrary.map { it.isSuccess } withDebugName "shouldImportLibrary"
            view.shouldImportLibrary.onlyChangesFromView().forEach(debugName = "onShouldImportLibraryChanged") { view.canImportLibrary.assert() }

            view.canImportProviderAccounts *= importDbContent.map { it.map { check(it.accounts != null) { "No accounts to import!" } } } withDebugName "canImportProviderAccounts"
            view.shouldImportProviderAccounts *= view.canImportProviderAccounts.map { it.isSuccess } withDebugName "shouldImportProviderAccounts"
            view.shouldImportProviderAccounts.onlyChangesFromView().forEach(debugName = "onShouldImportProviderAccounts") { view.canImportProviderAccounts.assert() }

            view.canImportFilters *= importDbContent.map { it.map { check(it.filters != null) { "No filters to import!" } } } withDebugName "canImportFilters"
            view.shouldImportFilters *= view.canImportFilters.map { it.isSuccess } withDebugName "shouldImportFilters"
            view.shouldImportFilters.onlyChangesFromView().forEach(debugName = "onShouldImportFilters") { view.canImportFilters.assert() }

            view.canAccept *= combine(
                view.importDatabaseFileIsValid,
                view.shouldImportLibrary.allValues(),
                view.shouldImportProviderAccounts.allValues(),
                view.shouldImportFilters.allValues()
            ) { importDatabaseFileIsValid, shouldImportLibrary, shouldImportProviderAccounts, shouldImportFilters ->
                importDatabaseFileIsValid and IsValid {
                    check(shouldImportLibrary || shouldImportProviderAccounts || shouldImportFilters) { "Must import something!" }
                }
            } withDebugName "canAccept"

            view.browseActions.forEach(debugName = "onBrowse") { onBrowse() }
            view.acceptActions.forEach(debugName = "onAccept") { onAccept() }
            view.cancelActions.forEach(debugName = "onCancel") { onCancel() }
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

            val file = view.importDatabaseFile.v.file
            val content = importDbContent.value.getOrThrow().run {
                copy(
                    db = db?.takeIf { view.shouldImportLibrary.v },
                    accounts = accounts?.takeIf { view.shouldImportProviderAccounts.v },
                    filters = filters?.takeIf { view.shouldImportFilters.v }
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