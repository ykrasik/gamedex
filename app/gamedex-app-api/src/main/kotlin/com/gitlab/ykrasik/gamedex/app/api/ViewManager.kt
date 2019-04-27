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

package com.gitlab.ykrasik.gamedex.app.api

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameDataType
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.app.api.common.AboutView
import com.gitlab.ykrasik.gamedex.app.api.game.*
import com.gitlab.ykrasik.gamedex.app.api.library.DeleteLibraryView
import com.gitlab.ykrasik.gamedex.app.api.library.EditLibraryView
import com.gitlab.ykrasik.gamedex.app.api.maintenance.CleanupDatabaseView
import com.gitlab.ykrasik.gamedex.app.api.maintenance.DuplicatesView
import com.gitlab.ykrasik.gamedex.app.api.maintenance.FolderNameDiffView
import com.gitlab.ykrasik.gamedex.app.api.maintenance.StaleData
import com.gitlab.ykrasik.gamedex.app.api.provider.RefetchGamesView
import com.gitlab.ykrasik.gamedex.app.api.provider.ResyncGamesView
import com.gitlab.ykrasik.gamedex.app.api.provider.SyncGamesView
import com.gitlab.ykrasik.gamedex.app.api.report.DeleteReportView
import com.gitlab.ykrasik.gamedex.app.api.report.EditReportView
import com.gitlab.ykrasik.gamedex.app.api.report.Report
import com.gitlab.ykrasik.gamedex.app.api.report.ReportView
import com.gitlab.ykrasik.gamedex.app.api.settings.SettingsView
import com.gitlab.ykrasik.gamedex.app.api.task.TaskView

/**
 * User: ykrasik
 * Date: 21/05/2018
 * Time: 10:29
 *
 * A required implementation by the view layer that allows showing & hiding views.
 */
interface ViewManager {
    fun showTaskView(): TaskView
    fun hide(view: TaskView)

    fun showSyncGamesView(): SyncGamesView
    fun hide(view: SyncGamesView)

    fun showGameDetailsView(game: Game): GameDetailsView
    fun hide(view: GameDetailsView)

    fun showEditLibraryView(library: Library?): EditLibraryView
    fun hide(view: EditLibraryView)

    fun showDeleteLibraryView(library: Library): DeleteLibraryView
    fun hide(view: DeleteLibraryView)

    fun showEditGameView(game: Game, initialType: GameDataType): EditGameView
    fun hide(view: EditGameView)

    fun showDeleteGameView(game: Game): DeleteGameView
    fun hide(view: DeleteGameView)

    fun showRenameMoveGameView(game: Game, initialName: String?): RenameMoveGameView
    fun hide(view: RenameMoveGameView)

    fun showTagGameView(game: Game): TagGameView
    fun hide(view: TagGameView)

    fun showReportView(report: Report): ReportView
    fun hide(view: ReportView)

    fun showEditReportView(report: Report?): EditReportView
    fun hide(view: EditReportView)

    fun showDeleteReportView(report: Report): DeleteReportView
    fun hide(view: DeleteReportView)

    fun showRefetchGamesView(): RefetchGamesView
    fun hide(view: RefetchGamesView)

    fun showResyncGamesView(): ResyncGamesView
    fun hide(view: ResyncGamesView)

    fun showCleanupDatabaseView(staleData: StaleData): CleanupDatabaseView
    fun hide(view: CleanupDatabaseView)

    fun showDuplicatesView(): DuplicatesView
    fun hide(view: DuplicatesView)

    fun showFolderNameDiffView(): FolderNameDiffView
    fun hide(view: FolderNameDiffView)

    fun showSettingsView(): SettingsView
    fun hide(view: SettingsView)

    fun showAboutView(): AboutView
    fun hide(view: AboutView)
}