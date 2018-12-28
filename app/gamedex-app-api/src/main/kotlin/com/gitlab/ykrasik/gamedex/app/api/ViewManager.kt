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

package com.gitlab.ykrasik.gamedex.app.api

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameDataType
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.app.api.game.*
import com.gitlab.ykrasik.gamedex.app.api.library.DeleteLibraryView
import com.gitlab.ykrasik.gamedex.app.api.library.EditLibraryView
import com.gitlab.ykrasik.gamedex.app.api.maintenance.CleanupDatabaseView
import com.gitlab.ykrasik.gamedex.app.api.maintenance.StaleData
import com.gitlab.ykrasik.gamedex.app.api.report.EditReportView
import com.gitlab.ykrasik.gamedex.app.api.report.Report
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
    fun closeTaskView(view: TaskView)

    fun showEditLibraryView(library: Library?): EditLibraryView
    fun closeEditLibraryView(view: EditLibraryView)

    fun showGameView(game: Game): GameView
    fun closeGameView(view: GameView)

    fun showDeleteLibraryView(library: Library): DeleteLibraryView
    fun closeDeleteLibraryView(view: DeleteLibraryView)

    fun showEditGameView(game: Game, initialType: GameDataType): EditGameView
    fun closeEditGameView(view: EditGameView)

    fun showDeleteGameView(game: Game): DeleteGameView
    fun closeDeleteGameView(view: DeleteGameView)

    fun showRenameMoveGameView(game: Game, initialName: String?): RenameMoveGameView
    fun closeRenameMoveGameView(view: RenameMoveGameView)

    fun showTagGameView(game: Game): TagGameView
    fun closeTagGameView(view: TagGameView)

    fun showEditReportView(report: Report?): EditReportView
    fun closeEditReportView(view: EditReportView)

    fun showRedownloadGamesView(): RedownloadGamesView
    fun closeRedownloadGamesView(view: RedownloadGamesView)

    fun showCleanupDatabaseView(staleData: StaleData): CleanupDatabaseView
    fun closeCleanupDatabaseView(view: CleanupDatabaseView)

    fun showSettingsView(): SettingsView
    fun closeSettingsView(view: SettingsView)
}