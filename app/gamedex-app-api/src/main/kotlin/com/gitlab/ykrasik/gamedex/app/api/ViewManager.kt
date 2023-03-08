/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.Library
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
import com.gitlab.ykrasik.gamedex.app.api.web.BrowserView
import kotlinx.coroutines.flow.Flow

/**
 * User: ykrasik
 * Date: 21/05/2018
 * Time: 10:29
 *
 * A required implementation by the view layer that allows showing & hiding views.
 */
interface ViewManager {
    val externalCloseRequests: Flow<Any>

    fun showTaskView(): TaskView
    fun hide(view: TaskView)

    fun showSyncGamesView(): SyncGamesView
    fun hide(view: SyncGamesView)

    fun showGameDetailsView(params: ViewGameParams): GameDetailsView
    fun hide(view: GameDetailsView)

    fun showEditLibraryView(library: Library?): EditLibraryView
    fun hide(view: EditLibraryView)

    fun showDeleteLibraryView(library: Library): DeleteLibraryView
    fun hide(view: DeleteLibraryView)

    fun showEditGameView(params: EditGameParams): EditGameView
    fun hide(view: EditGameView)

    fun showRawGameDataView(game: Game): RawGameDataView
    fun hide(view: RawGameDataView)

    fun showDeleteGameView(game: Game): DeleteGameView
    fun hide(view: DeleteGameView)

    fun showRenameMoveGameView(params: RenameMoveGameParams): RenameMoveGameView
    fun hide(view: RenameMoveGameView)

    fun showTagGameView(game: Game): TagGameView
    fun hide(view: TagGameView)

    fun showEditFilterView(filter: NamedFilter): EditFilterView
    fun hide(view: EditFilterView)

    fun showDeleteFilterView(filter: NamedFilter): DeleteFilterView
    fun hide(view: DeleteFilterView)

    fun showImageGalleryView(params: ViewImageParams): ImageGalleryView
    fun hide(view: ImageGalleryView)

    fun showBulkUpdateGamesView(): BulkUpdateGamesView
    fun hide(view: BulkUpdateGamesView)

    fun showSyncGamesWithMissingProvidersView(): SyncGamesWithMissingProvidersView
    fun hide(view: SyncGamesWithMissingProvidersView)

    fun showImportDatabaseView(): ImportDatabaseView
    fun hide(view: ImportDatabaseView)

    fun showExportDatabaseView(): ExportDatabaseView
    fun hide(view: ExportDatabaseView)

    fun showCleanupDatabaseView(cleanupData: CleanupData): CleanupDatabaseView
    fun hide(view: CleanupDatabaseView)

    fun showDuplicatesView(): DuplicatesView
    fun hide(view: DuplicatesView)

    fun showFolderNameDiffView(): FolderNameDiffView
    fun hide(view: FolderNameDiffView)

    fun showLogView(): LogView
    fun hide(view: LogView)

    fun showSettingsView(): SettingsView
    fun hide(view: SettingsView)

    fun showBrowserView(url: String): BrowserView
    fun hide(view: BrowserView)

    fun showAboutView(): AboutView
    fun hide(view: AboutView)
}