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
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.maintenance.MaintenanceService
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.util.Try
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
    private val gameService: GameService,
    private val libraryService: LibraryService,
    private val taskService: TaskService,
    private val settingsService: SettingsService,
    private val eventBus: EventBus
) : Presenter<ImportDatabaseView> {
    override fun present(view: ImportDatabaseView) = object : ViewSession() {
        init {
            view.importDatabaseFile.forEach { onFileChanged() }
            view.browseActions.forEach { onBrowse() }
            view.acceptActions.forEach { onAccept() }
            view.cancelActions.forEach { onCancel() }
        }

        override suspend fun onShown() {
            view.importDatabaseFile *= ""
            onBrowse()
        }

        private fun onFileChanged() {
            view.importDatabaseFileIsValid *= Try {
                check(view.importDatabaseFile.value.isNotBlank()) { "Please enter a path to a file!" }
                check(view.importDatabaseFile.value.file.isFile) { "File doesn't exist or is a directory!" }
            }
            setCanAccept()
        }

        private suspend fun onBrowse() {
            val file = view.selectImportDatabaseFile(settingsService.general.exportDbDirectory)
            if (file != null) {
                view.importDatabaseFile *= file.absolutePath
                if (gameService.games.isEmpty() && libraryService.libraries.isEmpty()) {
                    // If the current db is empty, no need to confirm.
                    onAccept()
                }
            }
            onFileChanged()
        }

        private fun setCanAccept() {
            view.canAccept *= view.importDatabaseFileIsValid.value
        }

        private suspend fun onAccept() {
            view.canAccept.assert()
            hideView()

            val file = view.importDatabaseFile.value.file
            taskService.execute(maintenanceService.importDatabase(file))
            settingsService.general.modify { copy(exportDbDirectory = file.parentFile) }
        }

        private fun onCancel() {
            hideView()
        }

        private fun hideView() = eventBus.requestHideView(view)
    }
}