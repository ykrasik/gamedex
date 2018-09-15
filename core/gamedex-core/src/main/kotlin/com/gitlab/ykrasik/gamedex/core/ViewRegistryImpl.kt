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
import com.gitlab.ykrasik.gamedex.core.filter.MenuGameFilterPresenter
import com.gitlab.ykrasik.gamedex.core.filter.ReportGameFilterPresenter
import com.gitlab.ykrasik.gamedex.core.game.GamesPresenter
import com.gitlab.ykrasik.gamedex.core.game.all.SearchGamesPresenter
import com.gitlab.ykrasik.gamedex.core.game.all.SelectPlatformPresenter
import com.gitlab.ykrasik.gamedex.core.game.all.SortGamesPresenter
import com.gitlab.ykrasik.gamedex.core.game.delete.DeleteGamePresenter
import com.gitlab.ykrasik.gamedex.core.game.delete.ShowDeleteGamePresenter
import com.gitlab.ykrasik.gamedex.core.game.details.GameDetailsPresenter
import com.gitlab.ykrasik.gamedex.core.game.details.ShowGameDetailsPreseneter
import com.gitlab.ykrasik.gamedex.core.game.discover.DiscoverGameChooseResultsPresenter
import com.gitlab.ykrasik.gamedex.core.game.discover.DiscoverGamesWithoutProvidersPresenter
import com.gitlab.ykrasik.gamedex.core.game.discover.DiscoverNewGamesPresenter
import com.gitlab.ykrasik.gamedex.core.game.discover.RediscoverGamePresenter
import com.gitlab.ykrasik.gamedex.core.game.download.*
import com.gitlab.ykrasik.gamedex.core.game.edit.EditGamePresenter
import com.gitlab.ykrasik.gamedex.core.game.edit.ShowEditGamePresenter
import com.gitlab.ykrasik.gamedex.core.game.rename.RenameMoveGamePresenter
import com.gitlab.ykrasik.gamedex.core.game.rename.ShowRenameMoveGamePresenter
import com.gitlab.ykrasik.gamedex.core.game.tag.ShowTagGamePresenter
import com.gitlab.ykrasik.gamedex.core.game.tag.TagGamePresenter
import com.gitlab.ykrasik.gamedex.core.general.CleanupDbPresenter
import com.gitlab.ykrasik.gamedex.core.general.ClearUserDataPresenter
import com.gitlab.ykrasik.gamedex.core.general.ExportDatabasePresenter
import com.gitlab.ykrasik.gamedex.core.general.ImportDatabasePresenter
import com.gitlab.ykrasik.gamedex.core.library.*
import com.gitlab.ykrasik.gamedex.core.log.LogEntriesPresenter
import com.gitlab.ykrasik.gamedex.core.log.LogLevelPresenter
import com.gitlab.ykrasik.gamedex.core.log.LogTailPresenter
import com.gitlab.ykrasik.gamedex.core.settings.*
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
    libraries: LibrariesPresenter,
    showAddLibrary: ShowAddLibraryPresenter,
    showEditLibrary: ShowEditLibraryPresenter,
    editLibrary: EditLibraryPresenter,
    showDeleteLibrary: ShowDeleteLibraryPresenter,
    deleteLibrary: DeleteLibraryPresenter,

    selectPlatform: SelectPlatformPresenter,

    games: GamesPresenter,
    searchGames: SearchGamesPresenter,
    sortGames: SortGamesPresenter,

    showGameDetails: ShowGameDetailsPreseneter,
    gameDetails: GameDetailsPresenter,
    redownloadGame: RedownloadGamePresenter,
    rediscoverGame: RediscoverGamePresenter,
    showEditGame: ShowEditGamePresenter,
    editGame: EditGamePresenter,
    showDeleteGame: ShowDeleteGamePresenter,
    deleteGame: DeleteGamePresenter,
    showRenameMoveGame: ShowRenameMoveGamePresenter,
    renameMoveGame: RenameMoveGamePresenter,
    showTagGame: ShowTagGamePresenter,
    tagGame: TagGamePresenter,
    redownloadCreatedBeforePeriod: GameRedownloadCreatedBeforePeriodPresenter,
    redownloadUpdatedAfterPeriod: GameRedownloadUpdatedAfterPeriodPresenter,
    redownloadGamesCreatedBefore: RedownloadGamesCreatedBeforePresenter,
    redownloadGamesUpdatedAfter: RedownloadGamesUpdatedAfterPresenter,
    discoverGameChooseResults: DiscoverGameChooseResultsPresenter,
    discoverNewGames: DiscoverNewGamesPresenter,
    discoverGamesWithoutProviders: DiscoverGamesWithoutProvidersPresenter,

    menuGameFilter: MenuGameFilterPresenter,
    reportGameFilter: ReportGameFilterPresenter,

    showSettings: ShowSettingsPresenter,
    settings: SettingsPresenter,

    exportDatabase: ExportDatabasePresenter,
    importDatabase: ImportDatabasePresenter,
    clearUserData: ClearUserDataPresenter,
    cleanupDb: CleanupDbPresenter,

    changeGameCellDisplaySettings: ChangeGameCellDisplaySettingsPresenter,
    gameCellDisplaySettings: GameCellDisplaySettingsPresenter,
    changeGameNameOverlayDisplaySettings: ChangeGameNameOverlayDisplaySettingsPresenter,
    gameNameOverlayDisplaySettings: GameNameOverlayDisplaySettingsPresenter,
    changeGameMetaTagOverlayDisplaySettings: ChangeGameMetaTagOverlayDisplaySettingsPresenter,
    gameMetaTagOverlayDisplaySettings: GameMetaTagOverlayDisplaySettingsPresenter,
    changeGameVersionOverlayDisplaySettings: ChangeGameVersionOverlayDisplaySettingsPresenter,
    gameVersionOverlayDisplaySettings: GameVersionOverlayDisplaySettingsPresenter,
    providerOrderSettings: ProviderOrderSettingsPresenter,
    providerSettings: ProviderSettingsPresenter,

    logEntries: LogEntriesPresenter,
    logLevel: LogLevelPresenter,
    logTail: LogTailPresenter
) : ViewRegistry {
    private val presenterClasses: Map<KClass<*>, Presenter<*>> = listOf(
        presenter(libraries),
        presenter(showAddLibrary),
        presenter(showEditLibrary),
        presenter(editLibrary),
        presenter(showDeleteLibrary),
        presenter(deleteLibrary),

        presenter(selectPlatform),

        presenter(games),
        presenter(searchGames),
        presenter(sortGames),

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
        presenter(redownloadCreatedBeforePeriod),
        presenter(redownloadUpdatedAfterPeriod),
        presenter(redownloadGamesCreatedBefore),
        presenter(redownloadGamesUpdatedAfter),
        presenter(discoverGameChooseResults),
        presenter(discoverNewGames),
        presenter(discoverGamesWithoutProviders),

        presenter(menuGameFilter),
        presenter(reportGameFilter),

        presenter(showSettings),
        presenter(settings),

        presenter(exportDatabase),
        presenter(importDatabase),
        presenter(clearUserData),
        presenter(cleanupDb),

        presenter(changeGameCellDisplaySettings),
        presenter(gameCellDisplaySettings),
        presenter(changeGameNameOverlayDisplaySettings),
        presenter(gameNameOverlayDisplaySettings),
        presenter(changeGameMetaTagOverlayDisplaySettings),
        presenter(gameMetaTagOverlayDisplaySettings),
        presenter(changeGameVersionOverlayDisplaySettings),
        presenter(gameVersionOverlayDisplaySettings),
        presenter(providerOrderSettings),
        presenter(providerSettings),

        presenter(logEntries),
        presenter(logLevel),
        presenter(logTail)
    ).toMap()

    private val activePresentations = mutableMapOf<Any, List<Presentation>>()

    private inline fun <reified V : Any> presenter(factory: Presenter<V>) = V::class to factory

    override fun register(view: Any) {
        check(activePresentations.put(view, presentView(view)) == null) { "View already registered: $view" }
    }

    private fun presentView(view: Any): List<Presentation> {
        @Suppress("UNCHECKED_CAST")
        fun <T> Presenter<*>.doPresent() = (this as Presenter<T>).present(view as T)

        return view.javaClass.interfaces.map { iface ->
            presenterClasses[iface.kotlin]!!.doPresent<Any>()
        }
    }

    override fun unregister(view: Any) {
        val presentersToDestroy = checkNotNull(activePresentations.remove(view)) { "View not registered: $view" }
        presentersToDestroy.forEach { it.destroy() }
    }

    override fun onShow(view: Any) {
        println("$view: Showing")
        activePresentations[view]!!.forEach { it.show() }
    }

    override fun onHide(view: Any) {
        println("$view: Hidden")
        activePresentations[view]!!.forEach { it.hide() }
    }

//    init {
//        Runtime.getRuntime().addShutdownHook(thread(start = false, name = "Shutdown") {
//            runBlocking {
//                activePresentations.values.flatten().forEach { it.destroy() }
//            }
//        })
//    }
}