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

package com.gitlab.ykrasik.gamedex.javafx

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameDataType
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.app.api.game.*
import com.gitlab.ykrasik.gamedex.app.api.library.DeleteLibraryView
import com.gitlab.ykrasik.gamedex.app.api.library.EditLibraryView
import com.gitlab.ykrasik.gamedex.app.api.maintenance.CleanupDatabaseView
import com.gitlab.ykrasik.gamedex.app.api.maintenance.StaleData
import com.gitlab.ykrasik.gamedex.app.api.provider.RedownloadGamesView
import com.gitlab.ykrasik.gamedex.app.api.provider.ResyncGamesView
import com.gitlab.ykrasik.gamedex.app.api.provider.SyncGamesView
import com.gitlab.ykrasik.gamedex.app.api.report.EditReportView
import com.gitlab.ykrasik.gamedex.app.api.report.Report
import com.gitlab.ykrasik.gamedex.app.api.settings.SettingsView
import com.gitlab.ykrasik.gamedex.app.api.task.TaskView
import com.gitlab.ykrasik.gamedex.app.javafx.game.delete.JavaFxDeleteGameView
import com.gitlab.ykrasik.gamedex.app.javafx.game.edit.JavaFxEditGameView
import com.gitlab.ykrasik.gamedex.app.javafx.game.rename.JavaFxRenameMoveGameView
import com.gitlab.ykrasik.gamedex.app.javafx.game.tag.JavaFxTagGameView
import com.gitlab.ykrasik.gamedex.app.javafx.library.JavaFxDeleteLibraryView
import com.gitlab.ykrasik.gamedex.app.javafx.library.JavaFxEditLibraryView
import com.gitlab.ykrasik.gamedex.app.javafx.maintenance.JavaFxCleanupDatabaseView
import com.gitlab.ykrasik.gamedex.app.javafx.provider.JavaFxRedownloadGamesView
import com.gitlab.ykrasik.gamedex.app.javafx.provider.JavaFxResyncGamesView
import com.gitlab.ykrasik.gamedex.app.javafx.report.JavaFxEditReportView
import com.gitlab.ykrasik.gamedex.app.javafx.settings.JavaFxSettingsView
import com.gitlab.ykrasik.gamedex.app.javafx.task.JavaFxTaskView
import javafx.stage.StageStyle
import tornadofx.Controller
import tornadofx.View

/**
 * User: ykrasik
 * Date: 21/05/2018
 * Time: 10:37
 */
class JavaFxViewManager : Controller(), ViewManager {

    private val mainView: MainView by inject()

    private val taskView: JavaFxTaskView by inject()

    override fun showTaskView(): TaskView {
        // Nothing to show, the taskView is always shown (actually, everything else is shown INSIDE the task view)
        return taskView
    }

    override fun hide(view: TaskView) {
        // Nothing to do here, the taskView is never hidden.
    }

    override fun showSyncGamesView() = mainView.showSyncGamesView()
    override fun hide(view: SyncGamesView) = mainView.showPreviousScreen()

    private val editLibraryView: JavaFxEditLibraryView by inject()
    override fun showEditLibraryView(library: Library?) = editLibraryView.showModal { this.library = library }
    override fun hide(view: EditLibraryView) = view.close()

    override fun showGameView(game: Game) = mainView.showGameDetails(game)
    override fun hide(view: GameView) = mainView.showPreviousScreen()

    private val deleteLibraryView: JavaFxDeleteLibraryView by inject()
    override fun showDeleteLibraryView(library: Library) = deleteLibraryView.showModal { this.library = library }
    override fun hide(view: DeleteLibraryView) = view.close()

    private val editGameView: JavaFxEditGameView by inject()
    override fun showEditGameView(game: Game, initialType: GameDataType) = editGameView.showModal {
        this.game = game
        this.initialScreen = initialType
    }

    override fun hide(view: EditGameView) = view.close()

    private val deleteGameView: JavaFxDeleteGameView by inject()
    override fun showDeleteGameView(game: Game) = deleteGameView.showModal { this.game = game }
    override fun hide(view: DeleteGameView) = view.close()

    private val renameMoveGameView: JavaFxRenameMoveGameView by inject()
    override fun showRenameMoveGameView(game: Game, initialName: String?) = renameMoveGameView.showModal {
        this.game = game
        this.initialName = initialName
    }
    override fun hide(view: RenameMoveGameView) = view.close()

    private val tagGameView: JavaFxTagGameView by inject()
    override fun showTagGameView(game: Game) = tagGameView.showModal { this.game = game }
    override fun hide(view: TagGameView) = view.close()

    private val editReportView: JavaFxEditReportView by inject()
    override fun showEditReportView(report: Report?) = editReportView.showModal { this.report = report }
    override fun hide(view: EditReportView) = view.close()

    private val settingsView: JavaFxSettingsView by inject()
    override fun showSettingsView() = settingsView.showModal()
    override fun hide(view: SettingsView) = view.close()

    private val redownloadGamesView: JavaFxRedownloadGamesView by inject()
    override fun showRedownloadGamesView() = redownloadGamesView.showModal()
    override fun hide(view: RedownloadGamesView) = view.close()

    private val resyncGamesView: JavaFxResyncGamesView by inject()
    override fun showResyncGamesView() = resyncGamesView.showModal()
    override fun hide(view: ResyncGamesView) = view.close()

    private val cleanupDatabaseView: JavaFxCleanupDatabaseView by inject()
    override fun showCleanupDatabaseView(staleData: StaleData) = cleanupDatabaseView.showModal { this.staleData = staleData }
    override fun hide(view: CleanupDatabaseView) = view.close()

    private fun Any.close() = (this as View).close()

    private inline fun <V : View> V.showModal(f: V.() -> Unit = {}): V = apply {
        f()
        openModal(StageStyle.TRANSPARENT)
    }
}