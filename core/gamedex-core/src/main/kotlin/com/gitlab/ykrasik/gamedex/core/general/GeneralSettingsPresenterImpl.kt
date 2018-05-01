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

import com.gitlab.ykrasik.gamedex.app.api.general.GeneralSettingsPresenter
import com.gitlab.ykrasik.gamedex.app.api.general.GeneralSettingsView
import com.gitlab.ykrasik.gamedex.app.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.core.BasePresenter
import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfigRepository
import com.gitlab.ykrasik.gamedex.util.now
import org.joda.time.DateTimeZone
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 24/04/2018
 * Time: 08:13
 */
@Singleton
class GeneralSettingsPresenterImpl @Inject constructor(
    private val generalSettingsService: GeneralSettingsService,
    userConfigRepository: UserConfigRepository,
    taskRunner: TaskRunner
) : BasePresenter<GeneralSettingsView>(taskRunner), GeneralSettingsPresenter {
    private val generalUserConfig = userConfigRepository[GeneralUserConfig::class]

    override fun initView(view: GeneralSettingsView) {}

    override fun onExportDatabase() = handle {
        val selectedDirectory = view.selectDatabaseExportDirectory(generalUserConfig.exportDbDirectory) ?: return@handle
        generalUserConfig.exportDbDirectory = selectedDirectory
        val timestamp = now.withZone(DateTimeZone.getDefault())
        val timestamptedPath = Paths.get(
            selectedDirectory.toString(),
            timestamp.toString("yyyy-MM-dd"),
            "db_${timestamp.toString("HH_mm_ss")}.json"
        ).toFile()

        generalSettingsService.exportDatabase(timestamptedPath).runTask()
        view.browseDirectory(timestamptedPath.parentFile)
    }

    override fun onImportDatabase() = handle {
        val file = view.selectDatabaseImportFile(generalUserConfig.exportDbDirectory) ?: return@handle
        if (view.confirmImportDatabase()) {
            generalSettingsService.importDatabase(file).runTask()
        }
    }

    override fun onClearUserData() = handle {
        if (view.confirmClearUserData()) {
            generalSettingsService.deleteAllUserData().runTask()
        }
    }

    override fun onCleanupDb() = handle {
        val staleData = generalSettingsService.detectStaleData().runTask()
        if (staleData.isEmpty) return@handle

        if (view.confirmDeleteStaleData(staleData)) {
            // TODO: Create backup before deleting
            generalSettingsService.deleteStaleData(staleData).runTask()
        }
    }
}

