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

package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.app.api.ViewService
import com.gitlab.ykrasik.gamedex.core.game.delete.DeleteGamePresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.details.GameDetailsPresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.discover.DiscoverGameChooseResultsPresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.discover.DiscoverGamesWithoutProvidersPresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.discover.DiscoverNewGamesPresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.discover.RediscoverGamePresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.download.GameDownloadStaleDurationPresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.download.RedownloadAllStaleGamesPresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.download.RedownloadGamePresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.edit.EditGamePresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.rename.RenameMoveGamePresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.tag.TagGamePresenterFactory
import com.gitlab.ykrasik.gamedex.core.general.CleanupDbPresenterFactory
import com.gitlab.ykrasik.gamedex.core.general.ClearUserDataPresenterFactory
import com.gitlab.ykrasik.gamedex.core.general.ExportDatabasePresenterFactory
import com.gitlab.ykrasik.gamedex.core.general.ImportDatabasePresenterFactory
import com.gitlab.ykrasik.gamedex.core.library.DeleteLibraryPresenterFactory
import com.gitlab.ykrasik.gamedex.core.library.EditLibraryPresenterFactory
import com.gitlab.ykrasik.gamedex.core.library.LibrariesPresenterFactory
import com.gitlab.ykrasik.gamedex.core.log.LogEntriesPresenterFactory
import com.gitlab.ykrasik.gamedex.core.log.LogLevelPresenterFactory
import com.gitlab.ykrasik.gamedex.core.log.LogTailPresenterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 26/05/2018
 * Time: 17:33
 */
@Singleton
class ViewServiceImpl @Inject constructor(
    private val libraries: LibrariesPresenterFactory,
    private val editLibrary: EditLibraryPresenterFactory,
    private val deleteLibrary: DeleteLibraryPresenterFactory,
    private val gameDetails: GameDetailsPresenterFactory,
    private val redownloadGame: RedownloadGamePresenterFactory,
    private val rediscoverGame: RediscoverGamePresenterFactory,
    private val editGame: EditGamePresenterFactory,
    private val tagGame: TagGamePresenterFactory,
    private val renameMoveGame: RenameMoveGamePresenterFactory,
    private val deleteGame: DeleteGamePresenterFactory,
    private val gameDownloadStaleDuration: GameDownloadStaleDurationPresenterFactory,
    private val redownloadAllStaleGames: RedownloadAllStaleGamesPresenterFactory,
    private val discoverGameChooseResults: DiscoverGameChooseResultsPresenterFactory,
    private val discoverNewGames: DiscoverNewGamesPresenterFactory,
    private val discoverGamesWithoutProviders: DiscoverGamesWithoutProvidersPresenterFactory,
    private val exportDatabase: ExportDatabasePresenterFactory,
    private val importDatabase: ImportDatabasePresenterFactory,
    private val clearUserData: ClearUserDataPresenterFactory,
    private val cleanupDb: CleanupDbPresenterFactory,
    private val logEntries: LogEntriesPresenterFactory,
    private val logLevel: LogLevelPresenterFactory,
    private val logTail: LogTailPresenterFactory
) : ViewService {
    private val activePresenters = mutableMapOf<Any, List<Presenter>>()

    override fun register(view: Any) {
        check(activePresenters.put(view, presentView(view)) == null) { "View already registered: $view" }
    }

    private fun presentView(view: Any): List<Presenter> = view.run {
        listOfNotNull(
            presentViewIfApplicable(libraries),
            presentViewIfApplicable(editLibrary),
            presentViewIfApplicable(deleteLibrary),
            presentViewIfApplicable(gameDetails),
            presentViewIfApplicable(redownloadGame),
            presentViewIfApplicable(rediscoverGame),
            presentViewIfApplicable(editGame),
            presentViewIfApplicable(tagGame),
            presentViewIfApplicable(renameMoveGame),
            presentViewIfApplicable(deleteGame),
            presentViewIfApplicable(gameDownloadStaleDuration),
            presentViewIfApplicable(redownloadAllStaleGames),
            presentViewIfApplicable(discoverGameChooseResults),
            presentViewIfApplicable(discoverNewGames),
            presentViewIfApplicable(discoverGamesWithoutProviders),
            presentViewIfApplicable(exportDatabase),
            presentViewIfApplicable(importDatabase),
            presentViewIfApplicable(clearUserData),
            presentViewIfApplicable(cleanupDb),
            presentViewIfApplicable(logEntries),
            presentViewIfApplicable(logLevel),
            presentViewIfApplicable(logTail)
        )
    }

    private inline fun <reified V> Any.presentViewIfApplicable(factory: PresenterFactory<V>): Presenter? =
        (this as? V)?.let { factory.present(it) }

//    private fun presentView(view: Any): List<Presenter> = view.javaClass.interfaces.mapNotNull { iface ->
//        when (iface.kotlin) {
//            RedownloadGameView::class -> redownloadGame.present(view as RedownloadGameView)
//            TagGameView::class -> tagGame.present(view as TagGameView)
//            RenameMoveGameView::class -> renameMoveGame.present(view as RenameMoveGameView)
//            DeleteGameView::class -> deleteGame.present(view as DeleteGameView)
//            DownloadStaleDurationView::class -> gameDownloadStaleDuration.present(view as DownloadStaleDurationView)
//            RedownloadAllStaleGamesView::class -> redownloadAllStaleGames.present(view as RedownloadAllStaleGamesView)
//            DiscoverGameChooseResultsView::class -> discoverGameChooseResults.present(view as DiscoverGameChooseResultsView)
//            else -> null
//        }
//    }

    override fun unregister(view: Any) {
        val presentersToDestroy = checkNotNull(activePresenters.remove(view)) { "View not registered: $view" }
        presentersToDestroy.forEach { it.destroy() }
    }

    override fun onShow(view: Any) {
        println("$view: Showing")
        // TODO: Eventually, make it activePresenters[view]!!
        activePresenters[view]?.forEach { it.show() }
    }

    override fun onHide(view: Any) {
        println("$view: Hidden")
        // TODO: Eventually, make it activePresenters[view]!!
        activePresenters[view]?.forEach { it.hide() }
    }
}