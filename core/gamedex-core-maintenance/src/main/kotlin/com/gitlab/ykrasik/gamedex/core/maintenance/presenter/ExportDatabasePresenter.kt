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

import com.gitlab.ykrasik.gamedex.app.api.maintenance.ExportDatabaseView
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.maintenance.MaintenanceService
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.util.Try
import com.gitlab.ykrasik.gamedex.util.defaultTimeZone
import com.gitlab.ykrasik.gamedex.util.file
import com.gitlab.ykrasik.gamedex.util.now
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
    private val settingsService: SettingsService,
    private val eventBus: EventBus
) : Presenter<ExportDatabaseView> {
    override fun present(view: ExportDatabaseView) = object : ViewSession() {
        init {
            view.exportDatabaseDirectory.forEach { onDirectoryChanged() }
            view.browseActions.forEach { onBrowse() }
            view.acceptActions.forEach { onAccept() }
            view.cancelActions.forEach { onCancel() }
        }

        override suspend fun onShown() {
            view.exportDatabaseDirectory *= ""
            onBrowse()
        }

        private fun onDirectoryChanged() {
            view.exportDatabaseFolderIsValid *= Try {
                check(view.exportDatabaseDirectory.value.isNotBlank()) { "Please enter a path to a directory!" }
                check(view.exportDatabaseDirectory.value.file.isDirectory) { "Directory doesn't exist!" }
            }
            setCanAccept()
        }

        private suspend fun onBrowse() {
            val dir = view.selectExportDatabaseDirectory(settingsService.general.exportDbDirectory)
            if (dir != null) {
                view.exportDatabaseDirectory *= dir.absolutePath
                onAccept()
            } else {
                onDirectoryChanged()
            }
        }

        private fun setCanAccept() {
            view.canAccept *= view.exportDatabaseFolderIsValid.value
        }

        private suspend fun onAccept() {
            view.canAccept.assert()
            hideView()

            val file = view.exportDatabaseDirectory.value.file.resolve("db ${now.defaultTimeZone.toString("yyyy-MM-dd HH_mm_ss")}.json")
            taskService.execute(maintenanceService.exportDatabase(file))
            settingsService.general.modify { copy(exportDbDirectory = file.parentFile) }
            view.openDirectory(file.parentFile)
        }

        private fun onCancel() {
            hideView()
        }

        private fun hideView() = eventBus.requestHideView(view)
    }
}