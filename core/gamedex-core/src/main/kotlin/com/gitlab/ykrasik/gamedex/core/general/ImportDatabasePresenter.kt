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

package com.gitlab.ykrasik.gamedex.core.general

import com.gitlab.ykrasik.gamedex.app.api.general.ImportDatabaseView
import com.gitlab.ykrasik.gamedex.app.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.core.Presentation
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/05/2018
 * Time: 13:23
 */
@Singleton
class ImportDatabasePresenter @Inject constructor(
    private val databaseActionsService: DatabaseActionsService,
    private val taskRunner: TaskRunner,
    private val settingsService: SettingsService
) : Presenter<ImportDatabaseView> {
    override fun present(view: ImportDatabaseView) = object : Presentation() {
        init {
            view.importDatabaseActions.actionOnUi { importDatabase() }
        }

        private suspend fun importDatabase() {
            val file = view.selectDatabaseImportFile(settingsService.general.exportDbDirectory) ?: return
            if (view.confirmImportDatabase()) {
                // Drop any filters we may currently have - they may be incorrect for the new database (point to non-existing libraries).
                settingsService.game.modify { copy(platformSettings = emptyMap()) }
                taskRunner.runTask(databaseActionsService.importDatabase(file))
            }
        }
    }
}