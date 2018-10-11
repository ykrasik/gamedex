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

import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.app.api.game.*
import com.gitlab.ykrasik.gamedex.app.api.library.DeleteLibraryView
import com.gitlab.ykrasik.gamedex.app.api.library.EditLibraryView
import com.gitlab.ykrasik.gamedex.app.api.report.EditReportView
import com.gitlab.ykrasik.gamedex.app.api.settings.SettingsView
import com.gitlab.ykrasik.gamedex.app.javafx.game.delete.JavaFxDeleteGameView
import com.gitlab.ykrasik.gamedex.app.javafx.game.edit.JavaFxEditGameView
import com.gitlab.ykrasik.gamedex.app.javafx.game.rename.JavaFxRenameMoveGameView
import com.gitlab.ykrasik.gamedex.app.javafx.game.tag.JavaFxTagGameView
import com.gitlab.ykrasik.gamedex.app.javafx.library.JavaFxDeleteLibraryView
import com.gitlab.ykrasik.gamedex.app.javafx.library.JavaFxEditLibraryView
import com.gitlab.ykrasik.gamedex.app.javafx.report.JavaFxEditReportView
import com.gitlab.ykrasik.gamedex.app.javafx.settings.JavaFxSettingsView
import com.gitlab.ykrasik.gamedex.javafx.game.details.JavaFxViewGameScreen
import tornadofx.View
import tornadofx.vbox
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 21/05/2018
 * Time: 10:37
 */
@Singleton
class JavaFxViewManager : View(), ViewManager {
    override val root = vbox()  // Unused.

    private val mainView: MainView by inject()

    override val gameView: JavaFxViewGameScreen by inject()
    override fun showGameView(view: GameView) = mainView.showGameDetails()
    override fun closeGameView(view: GameView) = mainView.showPreviousScreen()

    override val editLibraryView: JavaFxEditLibraryView by inject()
    override fun showEditLibraryView(view: EditLibraryView) = view.openModal()
    override fun closeEditLibraryView(view: EditLibraryView) = view.close()

    override val deleteLibraryView: JavaFxDeleteLibraryView by inject()
    override fun showDeleteLibraryView(view: DeleteLibraryView) = view.openModal()
    override fun closeDeleteLibraryView(view: DeleteLibraryView) = view.close()

    override val editGameView: JavaFxEditGameView by inject()
    override fun showEditGameView(view: EditGameView) = view.openModal()
    override fun closeEditGameView(view: EditGameView) = view.close()

    override val deleteGameView: JavaFxDeleteGameView by inject()
    override fun showDeleteGameView(view: DeleteGameView) = view.openModal()
    override fun closeDeleteGameView(view: DeleteGameView) = view.close()

    override val renameMoveGameView: JavaFxRenameMoveGameView by inject()
    override fun showRenameMoveGameView(view: RenameMoveGameView) = view.openModal()
    override fun closeRenameMoveGameView(view: RenameMoveGameView) = view.close()

    override val tagGameView: JavaFxTagGameView by inject()
    override fun showTagGameView(view: TagGameView) = view.openModal()
    override fun closeTagGameView(view: TagGameView) = view.close()

    override val editReportView: JavaFxEditReportView by inject()
    override fun showEditReportView(view: EditReportView) = view.openModal()
    override fun closeEditReportView(view: EditReportView) = view.close()

    override val settingsView: JavaFxSettingsView by inject()
    override fun showSettingsView(view: SettingsView) = view.openModal()
    override fun closeSettingsView(view: SettingsView) = view.close()

    private fun Any.openModal() {
        (this as View).openModal()
    }

    private fun Any.close() = (this as View).close()
}