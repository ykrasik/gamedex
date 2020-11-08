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

package com.gitlab.ykrasik.gamedex.app.javafx.maintenance

import com.gitlab.ykrasik.gamedex.app.api.maintenance.*
import com.gitlab.ykrasik.gamedex.app.api.provider.ViewCanBulkUpdateGames
import com.gitlab.ykrasik.gamedex.app.api.provider.ViewCanSyncGamesWithMissingProviders
import com.gitlab.ykrasik.gamedex.app.api.util.broadcastFlow
import com.gitlab.ykrasik.gamedex.app.javafx.JavaFxViewManager
import com.gitlab.ykrasik.gamedex.javafx.control.PopOverMenu
import com.gitlab.ykrasik.gamedex.javafx.control.enableWhen
import com.gitlab.ykrasik.gamedex.javafx.control.popOverSubMenu
import com.gitlab.ykrasik.gamedex.javafx.control.verticalGap
import com.gitlab.ykrasik.gamedex.javafx.mutableStateFlow
import com.gitlab.ykrasik.gamedex.javafx.theme.*
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import com.gitlab.ykrasik.gamedex.util.IsValid
import javafx.geometry.Pos
import tornadofx.hbox
import tornadofx.text
import tornadofx.tooltip
import tornadofx.useMaxWidth

/**
 * User: ykrasik
 * Date: 05/06/2017
 * Time: 14:57
 */
class MaintenanceMenu : PresentableView("Maintenance", Icons.wrench),
    ViewCanExportDatabase,
    ViewCanImportDatabase,
    ClearUserDataView,
    ViewCanCleanupDatabase,
    ViewCanBulkUpdateGames,
    ViewCanSyncGamesWithMissingProviders,
    ViewCanShowDuplicatesReport,
    ViewCanShowFolderNameDiffReport {

    override val importDatabaseActions = broadcastFlow<Unit>()
    override val exportDatabaseActions = broadcastFlow<Unit>()
    override val clearUserDataActions = broadcastFlow<Unit>()
    override val cleanupDatabaseActions = broadcastFlow<Unit>()

    override val canBulkUpdateGames = mutableStateFlow(IsValid.valid, debugName = "canBulkUpdateGames")
    override val bulkUpdateGamesActions = broadcastFlow<Unit>()

    override val canSyncGamesWithMissingProviders = mutableStateFlow(IsValid.valid, debugName = "canSyncGamesWithMissingProviders")
    override val syncGamesWithMissingProvidersActions = broadcastFlow<Unit>()

    override val showDuplicatesReportActions = broadcastFlow<Unit>()
    override val showFolderNameDiffReportActions = broadcastFlow<Unit>()

    private val viewManager: JavaFxViewManager by inject()

    init {
        register()
    }

    override val root = hbox()

    fun init(menu: PopOverMenu) = with(menu) {
        popOverSubMenu("Database", Icons.database) {
            warningButton("Import", Icons.import) {
                useMaxWidth = true
                alignment = Pos.CENTER_LEFT
                action(importDatabaseActions)
            }
            confirmButton("Export", Icons.export) {
                useMaxWidth = true
                alignment = Pos.CENTER_LEFT
                action(exportDatabaseActions)
            }

            verticalGap()

            deleteButton("Cleanup") {
                useMaxWidth = true
                graphic = Icons.databaseCleanup
                alignment = Pos.CENTER_LEFT
                tooltip("Cleanup stale data, like games linked to paths that no longer exist, unused images & file tree cache for deleted games.")
                action(cleanupDatabaseActions)
            }
        }

        popOverSubMenu("Provider", Icons.cloud) {
            infoButton("Bulk Update Games", Icons.download) {
                useMaxWidth = true
                alignment = Pos.CENTER_LEFT
                enableWhen(canBulkUpdateGames)
                action(bulkUpdateGamesActions)
            }
            infoButton("Sync Games with Missing Providers", Icons.sync) {
                useMaxWidth = true
                alignment = Pos.CENTER_LEFT
                enableWhen(canSyncGamesWithMissingProviders)
                action(syncGamesWithMissingProvidersActions)
            }
        }

        popOverSubMenu("Reports", Icons.chart) {
            infoButton("Duplicates", Icons.copy) {
                useMaxWidth = true
                alignment = Pos.CENTER_LEFT
                tooltip("Show a report of different game that share the same provider data.")
                action(showDuplicatesReportActions)
            }
            infoButton("Folder Name Diff", Icons.diff) {
                useMaxWidth = true
                alignment = Pos.CENTER_LEFT
                tooltip("Show a report of games whose folder names do not match the game name according to provider data.")
                action(showFolderNameDiffReportActions)
            }
        }

        verticalGap()

        deleteButton("Clear User Data") {
            useMaxWidth = true
            graphic = Icons.databaseCleanup
            alignment = Pos.CENTER_LEFT
            tooltip("Clear game user data, like tags, excluded providers or custom thumbnails for all games.")
            action(clearUserDataActions)
        }
    }

    override suspend fun confirmClearUserData() = viewManager.showAreYouSureDialog("Clear game user data?", Icons.warning) {
        text("This will remove tags, excluded providers & any custom information entered (like custom names or thumbnails) from all games.") {
            wrappingWidth = 400.0
        }
    }
}