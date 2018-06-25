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

import com.gitlab.ykrasik.gamedex.app.api.game.*
import com.gitlab.ykrasik.gamedex.app.api.library.DeleteLibraryView
import com.gitlab.ykrasik.gamedex.app.api.library.EditLibraryView
import com.gitlab.ykrasik.gamedex.app.api.report.EditReportView
import com.gitlab.ykrasik.gamedex.app.api.settings.SettingsView

/**
 * User: ykrasik
 * Date: 21/05/2018
 * Time: 10:29
 *
 * A required implementation by the view layer that allows showing & hiding views.
 */
interface ViewManager {
    val editLibraryView: EditLibraryView
    fun showEditLibraryView(view: EditLibraryView)
    fun showEditLibraryView(f: EditLibraryView.() -> Unit) = mutateAndShow(editLibraryView, f, this::showEditLibraryView)
    fun closeEditLibraryView(view: EditLibraryView)

    val gameDetailsView: GameDetailsView
    fun showGameDetailsView(view: GameDetailsView)
    fun showGameDetailsView(f: GameDetailsView.() -> Unit) = mutateAndShow(gameDetailsView, f, this::showGameDetailsView)
    fun closeGameDetailsView(view: GameDetailsView)

    val deleteLibraryView: DeleteLibraryView
    fun showDeleteLibraryView(view: DeleteLibraryView)
    fun showDeleteLibraryView(f: DeleteLibraryView.() -> Unit) = mutateAndShow(deleteLibraryView, f, this::showDeleteLibraryView)
    fun closeDeleteLibraryView(view: DeleteLibraryView)

    val editGameView: EditGameView
    fun showEditGameView(view: EditGameView)
    fun showEditGameView(f: EditGameView.() -> Unit) = mutateAndShow(editGameView, f, this::showEditGameView)
    fun closeEditGameView(view: EditGameView)

    val deleteGameView: DeleteGameView
    fun showDeleteGameView(view: DeleteGameView)
    fun showDeleteGameView(f: DeleteGameView.() -> Unit) = mutateAndShow(deleteGameView, f, this::showDeleteGameView)
    fun closeDeleteGameView(view: DeleteGameView)

    val renameMoveGameView: RenameMoveGameView
    fun showRenameMoveGameView(view: RenameMoveGameView)
    fun showRenameMoveGameView(f: RenameMoveGameView.() -> Unit) = mutateAndShow(renameMoveGameView, f, this::showRenameMoveGameView)
    fun closeRenameMoveGameView(view: RenameMoveGameView)

    val tagGameView: TagGameView
    fun showTagGameView(view: TagGameView)
    fun showTagGameView(f: TagGameView.() -> Unit) = mutateAndShow(tagGameView, f, this::showTagGameView)
    fun closeTagGameView(view: TagGameView)

    val editReportView: EditReportView
    fun showEditReportView(view: EditReportView)
    fun showEditReportView(f: EditReportView.() -> Unit) = mutateAndShow(editReportView, f, this::showEditReportView)
    fun closeEditReportView(view: EditReportView)
    
    val settingsView: SettingsView
    fun showSettingsView(view: SettingsView)
    fun showSettingsView(f: SettingsView.() -> Unit) = mutateAndShow(settingsView, f, this::showSettingsView)
    fun closeSettingsView(view: SettingsView)
    
    private inline fun <V> mutateAndShow(view: V, mutator: (V) -> Unit, show: (V) -> Unit) {
        mutator(view)
        show(view)
    }
}