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

package com.gitlab.ykrasik.gamedex.app.javafx.maintenance

import com.gitlab.ykrasik.gamedex.app.api.file.ViewCanOpenFile
import com.gitlab.ykrasik.gamedex.app.api.maintenance.*
import com.gitlab.ykrasik.gamedex.app.api.provider.ViewCanRefetchGames
import com.gitlab.ykrasik.gamedex.app.api.provider.ViewCanResyncGames
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.javafx.areYouSureDialog
import com.gitlab.ykrasik.gamedex.javafx.control.PopOverMenu
import com.gitlab.ykrasik.gamedex.javafx.control.enableWhen
import com.gitlab.ykrasik.gamedex.javafx.control.popOverSubMenu
import com.gitlab.ykrasik.gamedex.javafx.control.verticalGap
import com.gitlab.ykrasik.gamedex.javafx.state
import com.gitlab.ykrasik.gamedex.javafx.theme.*
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableTabView
import com.gitlab.ykrasik.gamedex.util.IsValid
import javafx.geometry.Pos
import tornadofx.*
import java.io.File

/**
 * User: ykrasik
 * Date: 05/06/2017
 * Time: 14:57
 */
class MaintenanceMenu : PresentableTabView("Maintenance", Icons.wrench),
    ExportDatabaseView,
    ImportDatabaseView,
    ClearUserDataView,
    ViewCanCleanupDatabase,
    ViewCanRefetchGames,
    ViewCanResyncGames,
    ViewCanShowDuplicatesReport,
    ViewCanShowFolderNameDiffReport,
    ViewCanOpenFile {

    override val exportDatabaseActions = channel<Unit>()
    override val importDatabaseActions = channel<Unit>()
    override val clearUserDataActions = channel<Unit>()
    override val cleanupDatabaseActions = channel<Unit>()
    override val refetchGamesActions = channel<Unit>()

    override val canResyncGames = state(IsValid.valid)
    override val resyncGamesActions = channel<Unit>()

    override val showDuplicatesReportActions = channel<Unit>()
    override val showFolderNameDiffReportActions = channel<Unit>()

    override val openFileActions = channel<File>()

    init {
        register()
    }

    override val root = hbox()

    fun init(menu: PopOverMenu) = with(menu) {
        popOverSubMenu("Database", Icons.database) {
            confirmButton("Export", Icons.export) {
                useMaxWidth = true
                alignment = Pos.CENTER_LEFT
                action(exportDatabaseActions)
            }
            warningButton("Import", Icons.import) {
                useMaxWidth = true
                alignment = Pos.CENTER_LEFT
                action(importDatabaseActions)
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

        popOverSubMenu("Games", Icons.games) {
            infoButton("Re-Fetch", Icons.download) {
                useMaxWidth = true
                alignment = Pos.CENTER_LEFT
                action(refetchGamesActions)
            }
            infoButton("Re-Sync", Icons.sync) {
                useMaxWidth = true
                alignment = Pos.CENTER_LEFT
                enableWhen(canResyncGames)
                action(resyncGamesActions)
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

    override fun selectDatabaseExportDirectory(initialDirectory: File?) =
        chooseDirectory("Select Database Export Folder...", initialDirectory)

    override fun selectDatabaseImportFile(initialDirectory: File?) =
        chooseFile("Select Database File...", filters = emptyArray()) {
            this@chooseFile.initialDirectory = initialDirectory
        }.firstOrNull()

    override fun browseDirectory(directory: File) {
        // TODO: This kinda sucks. a presenter is telling the view to browse, but the view is delegating to another presenter.
        openFileActions.offer(directory)
    }

    override fun confirmImportDatabase() = areYouSureDialog("The existing database will be lost!")

    override fun confirmClearUserData() = areYouSureDialog("Clear game user data?") {
        text("This will remove tags, excluded providers & any custom information entered (like custom names or thumbnails) from all games.") {
            wrappingWidth = 400.0
        }
    }
}