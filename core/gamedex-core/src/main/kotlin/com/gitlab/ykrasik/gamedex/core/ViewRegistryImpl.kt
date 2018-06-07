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

import com.gitlab.ykrasik.gamedex.app.api.ViewRegistry
import com.gitlab.ykrasik.gamedex.core.filter.MenuGameFilterPresenterFactory
import com.gitlab.ykrasik.gamedex.core.filter.ReportGameFilterPresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.GamesPresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.delete.DeleteGamePresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.delete.ShowDeleteGamePresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.details.GameDetailsPresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.details.ShowGameDetailsPreseneterFactory
import com.gitlab.ykrasik.gamedex.core.game.discover.DiscoverGameChooseResultsPresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.discover.DiscoverGamesWithoutProvidersPresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.discover.DiscoverNewGamesPresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.discover.RediscoverGamePresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.download.GameDownloadStaleDurationPresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.download.RedownloadAllStaleGamesPresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.download.RedownloadGamePresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.edit.EditGamePresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.edit.ShowEditGamePresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.presenter.SearchGamesPresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.presenter.SelectPlatformPresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.rename.RenameMoveGamePresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.rename.ShowRenameMoveGamePresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.tag.ShowTagGamePresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.tag.TagGamePresenterFactory
import com.gitlab.ykrasik.gamedex.core.general.CleanupDbPresenterFactory
import com.gitlab.ykrasik.gamedex.core.general.ClearUserDataPresenterFactory
import com.gitlab.ykrasik.gamedex.core.general.ExportDatabasePresenterFactory
import com.gitlab.ykrasik.gamedex.core.general.ImportDatabasePresenterFactory
import com.gitlab.ykrasik.gamedex.core.library.*
import com.gitlab.ykrasik.gamedex.core.log.LogEntriesPresenterFactory
import com.gitlab.ykrasik.gamedex.core.log.LogLevelPresenterFactory
import com.gitlab.ykrasik.gamedex.core.log.LogTailPresenterFactory
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 26/05/2018
 * Time: 17:33
 */
@Singleton
class ViewRegistryImpl @Inject constructor(
    libraries: LibrariesPresenterFactory,
    showAddLibrary: ShowAddLibraryPresenterFactory,
    showEditLibrary: ShowEditLibraryPresenterFactory,
    editLibrary: EditLibraryPresenterFactory,
    showDeleteLibrary: ShowDeleteLibraryPresenterFactory,
    deleteLibrary: DeleteLibraryPresenterFactory,

    selectPlatform: SelectPlatformPresenterFactory,

    games: GamesPresenterFactory,
    searchGames: SearchGamesPresenterFactory,

    showGameDetails: ShowGameDetailsPreseneterFactory,
    gameDetails: GameDetailsPresenterFactory,
    redownloadGame: RedownloadGamePresenterFactory,
    rediscoverGame: RediscoverGamePresenterFactory,
    showEditGame: ShowEditGamePresenterFactory,
    editGame: EditGamePresenterFactory,
    showDeleteGame: ShowDeleteGamePresenterFactory,
    deleteGame: DeleteGamePresenterFactory,
    showRenameMoveGame: ShowRenameMoveGamePresenterFactory,
    renameMoveGame: RenameMoveGamePresenterFactory,
    showTagGame: ShowTagGamePresenterFactory,
    tagGame: TagGamePresenterFactory,
    gameDownloadStaleDuration: GameDownloadStaleDurationPresenterFactory,
    redownloadAllStaleGames: RedownloadAllStaleGamesPresenterFactory,
    discoverGameChooseResults: DiscoverGameChooseResultsPresenterFactory,
    discoverNewGames: DiscoverNewGamesPresenterFactory,
    discoverGamesWithoutProviders: DiscoverGamesWithoutProvidersPresenterFactory,

    menuGameFilter: MenuGameFilterPresenterFactory,
    reportGameFilter: ReportGameFilterPresenterFactory,

    exportDatabase: ExportDatabasePresenterFactory,
    importDatabase: ImportDatabasePresenterFactory,
    clearUserData: ClearUserDataPresenterFactory,
    cleanupDb: CleanupDbPresenterFactory,

    logEntries: LogEntriesPresenterFactory,
    logLevel: LogLevelPresenterFactory,
    logTail: LogTailPresenterFactory
) : ViewRegistry {
    private val presenterClasses: Map<KClass<*>, PresenterFactory<*>> = listOf(
        presenter(libraries),
        presenter(showAddLibrary),
        presenter(showEditLibrary),
        presenter(editLibrary),
        presenter(showDeleteLibrary),
        presenter(deleteLibrary),

        presenter(selectPlatform),

        presenter(games),
        presenter(searchGames),

        presenter(showGameDetails),
        presenter(gameDetails),
        presenter(redownloadGame),
        presenter(rediscoverGame),
        presenter(showEditGame),
        presenter(editGame),
        presenter(showDeleteGame),
        presenter(deleteGame),
        presenter(showRenameMoveGame),
        presenter(renameMoveGame),
        presenter(showTagGame),
        presenter(tagGame),
        presenter(gameDownloadStaleDuration),
        presenter(redownloadAllStaleGames),
        presenter(discoverGameChooseResults),
        presenter(discoverNewGames),
        presenter(discoverGamesWithoutProviders),

        presenter(menuGameFilter),
        presenter(reportGameFilter),

        presenter(exportDatabase),
        presenter(importDatabase),
        presenter(clearUserData),
        presenter(cleanupDb),

        presenter(logEntries),
        presenter(logLevel),
        presenter(logTail)
    ).toMap()

    private val activePresenters = mutableMapOf<Any, List<Presenter>>()

    private inline fun <reified V : Any> presenter(factory: PresenterFactory<V>) = V::class to factory

    override fun register(view: Any) {
        check(activePresenters.put(view, presentView(view)) == null) { "View already registered: $view" }
    }

    private fun presentView(view: Any): List<Presenter> {
        @Suppress("UNCHECKED_CAST")
        fun <T> PresenterFactory<*>.doPresent() = (this as PresenterFactory<T>).present(view as T)

        return view.javaClass.interfaces.map { iface ->
            presenterClasses[iface.kotlin]!!.doPresent<Any>()
        }
    }

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