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

package com.gitlab.ykrasik.gamedex.app.javafx

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.app.api.common.AboutView
import com.gitlab.ykrasik.gamedex.app.api.filter.DeleteFilterView
import com.gitlab.ykrasik.gamedex.app.api.filter.EditFilterView
import com.gitlab.ykrasik.gamedex.app.api.filter.NamedFilter
import com.gitlab.ykrasik.gamedex.app.api.game.*
import com.gitlab.ykrasik.gamedex.app.api.image.ImageGalleryView
import com.gitlab.ykrasik.gamedex.app.api.image.ViewImageParams
import com.gitlab.ykrasik.gamedex.app.api.library.DeleteLibraryView
import com.gitlab.ykrasik.gamedex.app.api.library.EditLibraryView
import com.gitlab.ykrasik.gamedex.app.api.log.LogView
import com.gitlab.ykrasik.gamedex.app.api.maintenance.*
import com.gitlab.ykrasik.gamedex.app.api.provider.BulkUpdateGamesView
import com.gitlab.ykrasik.gamedex.app.api.provider.SyncGamesView
import com.gitlab.ykrasik.gamedex.app.api.provider.SyncGamesWithMissingProvidersView
import com.gitlab.ykrasik.gamedex.app.api.settings.SettingsView
import com.gitlab.ykrasik.gamedex.app.api.task.TaskView
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.api.util.onReceive
import com.gitlab.ykrasik.gamedex.app.api.web.BrowserView
import com.gitlab.ykrasik.gamedex.app.javafx.common.JavaFxAboutView
import com.gitlab.ykrasik.gamedex.app.javafx.common.JavaFxBrowserView
import com.gitlab.ykrasik.gamedex.app.javafx.filter.JavaFxDeleteFilterView
import com.gitlab.ykrasik.gamedex.app.javafx.filter.JavaFxEditFilterView
import com.gitlab.ykrasik.gamedex.app.javafx.game.*
import com.gitlab.ykrasik.gamedex.app.javafx.image.JavaFxImageGalleryView
import com.gitlab.ykrasik.gamedex.app.javafx.library.JavaFxDeleteLibraryView
import com.gitlab.ykrasik.gamedex.app.javafx.library.JavaFxEditLibraryView
import com.gitlab.ykrasik.gamedex.app.javafx.log.JavaFxLogView
import com.gitlab.ykrasik.gamedex.app.javafx.maintenance.JavaFxCleanupDatabaseView
import com.gitlab.ykrasik.gamedex.app.javafx.maintenance.JavaFxExportDatabaseView
import com.gitlab.ykrasik.gamedex.app.javafx.maintenance.JavaFxImportDatabaseView
import com.gitlab.ykrasik.gamedex.app.javafx.provider.JavaFxBulkUpdateGamesView
import com.gitlab.ykrasik.gamedex.app.javafx.provider.JavaFxSyncGamesWithMissingProvidersView
import com.gitlab.ykrasik.gamedex.app.javafx.settings.JavaFxSettingsView
import com.gitlab.ykrasik.gamedex.app.javafx.task.JavaFxTaskView
import com.gitlab.ykrasik.gamedex.javafx.control.OverlayPane
import com.gitlab.ykrasik.gamedex.javafx.screenBounds
import com.gitlab.ykrasik.gamedex.javafx.view.ConfirmationWindow
import javafx.scene.Node
import javafx.scene.layout.VBox
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select
import tornadofx.Controller
import tornadofx.View
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 21/05/2018
 * Time: 10:37
 */
@Singleton
class JavaFxViewManager : Controller(), ViewManager {
    private val mainView: MainView by inject()

    override val externalCloseRequests = channel<Any>()

    private val taskView: JavaFxTaskView by inject()
    override fun showTaskView() = taskView.showOverlay(modal = true)
    override fun hide(view: TaskView) = view.hideOverlay()

    private val gameDetailsView by lazy {
        JavaFxGameDetailsView(
            canClose = true,
            imageFitWidth = screenBounds.width / 3,
            imageFitHeight = screenBounds.height * 2 / 3
        )
    }

    override fun showGameDetailsView(params: ViewGameParams) =
        gameDetailsView.showOverlay(customizeOverlay = gameDetailsView.customizeOverlay) {
            this.gameParams *= params
        }

    override fun hide(view: GameDetailsView) = gameDetailsView.hideOverlay()

    override fun showSyncGamesView() = hideOverlayPane {
        mainView.showSyncGamesView()
    }

    override fun hide(view: SyncGamesView) = restoreOverlayPane {
        mainView.showPreviousScreen()
    }

    private val editLibraryView: JavaFxEditLibraryView by inject()
    override fun showEditLibraryView(library: Library?) = editLibraryView.showOverlay(modal = true) { this.library *= library }
    override fun hide(view: EditLibraryView) = view.hideOverlay()

    private val deleteLibraryView: JavaFxDeleteLibraryView by inject()
    override fun showDeleteLibraryView(library: Library) = deleteLibraryView.showOverlay { this.library *= library }
    override fun hide(view: DeleteLibraryView) = view.hideOverlay()

    private val editGameView: JavaFxEditGameView by inject()
    override fun showEditGameView(params: EditGameParams) = editGameView.showOverlay(modal = true) {
        this.game *= params.game
        this.initialView = params.initialView
    }

    override fun hide(view: EditGameView) = view.hideOverlay()

    private val deleteGameView: JavaFxDeleteGameView by inject()
    override fun showDeleteGameView(game: Game) = deleteGameView.showOverlay { this.game *= game }
    override fun hide(view: DeleteGameView) = view.hideOverlay()

    private val renameMoveGameView: JavaFxRenameMoveGameView by inject()
    override fun showRenameMoveGameView(params: RenameMoveGameParams) = renameMoveGameView.showOverlay {
        this.game *= params.game
        this.initialName *= params.initialSuggestion
    }

    override fun hide(view: RenameMoveGameView) = view.hideOverlay()

    private val tagGameView: JavaFxTagGameView by inject()
    override fun showTagGameView(game: Game) = tagGameView.showOverlay(modal = true) { this.game *= game }
    override fun hide(view: TagGameView) = view.hideOverlay()

    private val editFilterView: JavaFxEditFilterView by inject()
    override fun showEditFilterView(filter: NamedFilter) = editFilterView.showOverlay { this.initialNamedFilter *= filter }
    override fun hide(view: EditFilterView) = view.hideOverlay()

    private val deleteFilterView: JavaFxDeleteFilterView by inject()
    override fun showDeleteFilterView(filter: NamedFilter) = deleteFilterView.showOverlay { this.filter *= filter }
    override fun hide(view: DeleteFilterView) = view.hideOverlay()

    private val imageView: JavaFxImageGalleryView by inject()
    override fun showImageGalleryView(params: ViewImageParams) =
        imageView.showOverlay(customizeOverlay = imageView.customizeOverlay) {
            this.imageParams *= params
        }

    override fun hide(view: ImageGalleryView) = view.hideOverlay()

    private val bulkUpdateGamesView: JavaFxBulkUpdateGamesView by inject()
    override fun showBulkUpdateGamesView() = bulkUpdateGamesView.showOverlay()
    override fun hide(view: BulkUpdateGamesView) = view.hideOverlay()

    private val syncGamesWithMissingProvidersView: JavaFxSyncGamesWithMissingProvidersView by inject()
    override fun showSyncGamesWithMissingProvidersView() = syncGamesWithMissingProvidersView.showOverlay()
    override fun hide(view: SyncGamesWithMissingProvidersView) = view.hideOverlay()

    private val cleanupDatabaseView: JavaFxCleanupDatabaseView by inject()
    override fun showCleanupDatabaseView(staleData: StaleData) = cleanupDatabaseView.showOverlay(modal = true) { this.staleData *= staleData }
    override fun hide(view: CleanupDatabaseView) = view.hideOverlay()

    private val importDatabaseView: JavaFxImportDatabaseView by inject()
    override fun showImportDatabaseView() = importDatabaseView.showOverlay()
    override fun hide(view: ImportDatabaseView) = view.hideOverlay()

    private val exportDatabaseView: JavaFxExportDatabaseView by inject()
    override fun showExportDatabaseView() = exportDatabaseView.showOverlay()
    override fun hide(view: ExportDatabaseView) = view.hideOverlay()

    override fun showDuplicatesView() = mainView.showDuplicatesReport()
    override fun hide(view: DuplicatesView) = mainView.showPreviousScreen()

    override fun showFolderNameDiffView() = mainView.showFolderNameDiffReport()
    override fun hide(view: FolderNameDiffView) = mainView.showPreviousScreen()

    private val logView: JavaFxLogView by inject()
    override fun showLogView(): LogView = logView.showOverlay()
    override fun hide(view: LogView) = view.hideOverlay()

    private val settingsView: JavaFxSettingsView by inject()
    override fun showSettingsView() = settingsView.showOverlay(modal = true)
    override fun hide(view: SettingsView) = view.hideOverlay()

    private val browserView: JavaFxBrowserView by inject()
    override fun showBrowserView(url: String) = browserView.showOverlay() { load(url) }
    override fun hide(view: BrowserView) {
        browserView.load(null)
        view.hideOverlay()
    }

    private val aboutView: JavaFxAboutView by inject()
    override fun showAboutView() = aboutView.showOverlay()
    override fun hide(view: AboutView) = view.hideOverlay()

    suspend fun showAreYouSureDialog(text: String = "Are You Sure?", icon: Node? = null, op: (VBox.() -> Unit)? = null): Boolean {
        val accept = CompletableDeferred<Boolean>()
        val view = object : ConfirmationWindow(text, icon) {
            override val root = buildAreYouSure(op = op)

            val job: Job = GlobalScope.launch(Dispatchers.Main) {
                accept.complete(select {
                    acceptActions.onReceive { true }
                    cancelActions.onReceive { false }
                })
            }.apply {
                invokeOnCompletion {
                    acceptActions.close()
                    cancelActions.close()
                }
            }

            init {
                skipFirstDock = true
                skipFirstUndock = true
            }
        }
        mainView.showOverlay(view, modal = false, onExternalCloseRequested = {
            view.job.cancel()
            accept.complete(false)
            view.hideOverlay()
        })
        return accept.await().apply {
            view.hideOverlay()
        }
    }

    private fun Any.hideOverlay() = mainView.hideOverlay(this as View)

    private inline fun <V : View> V.showOverlay(
        modal: Boolean = false,
        noinline customizeOverlay: OverlayPane.OverlayLayer.() -> Unit = {},
        f: V.() -> Unit = {}
    ): V = apply {
        f()
        mainView.showOverlay(
            view = this,
            modal = modal,
            onExternalCloseRequested = { externalCloseRequests.offer(this) },
            customizeOverlay = customizeOverlay
        )
    }

    private inline fun <V : View> hideOverlayPane(f: () -> V): V {
        mainView.saveAndClearCurrentOverlays()
        return f()
    }

    private inline fun restoreOverlayPane(f: () -> Unit) {
        f()
        mainView.restoreSavedOverlays()
    }
}