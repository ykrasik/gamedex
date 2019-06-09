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

package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.app.api.util.MultiChannel
import com.gitlab.ykrasik.gamedex.app.api.util.MultiReceiveChannel
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.provider.EnabledGameProvider
import com.gitlab.ykrasik.gamedex.core.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.core.provider.SyncGamesEvent
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import com.gitlab.ykrasik.gamedex.core.util.*
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.provider.supportedPlatforms
import com.gitlab.ykrasik.gamedex.provider.supports
import com.google.inject.ImplementedBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 17/06/2018
 * Time: 10:32
 */
@ImplementedBy(CommonDataImpl::class)
interface CommonData {
    val games: ListObservable<Game>
    val platformGames: ListObservable<Game>

    val genres: ListObservable<String>
    val platformGenres: ListObservable<String>

    val tags: ListObservable<TagId>
    val platformTags: ListObservable<String>

    val filterTags: ListObservable<TagId>
    val platformFilterTags: ListObservable<String>

    val libraries: ListObservable<Library>
    val contentLibraries: ListObservable<Library>
    val platformLibraries: ListObservable<Library>

    val allProviders: ListObservable<GameProvider>
    val platformProviders: ListObservable<GameProvider>
    val enabledProviders: ListObservable<EnabledGameProvider>

    val platformsWithLibraries: ListObservable<Platform>
    val platformsWithEnabledProviders: ListObservable<Platform>

    val isGameSyncRunning: MultiReceiveChannel<Boolean>
}

@Singleton
class CommonDataImpl @Inject constructor(
    gameService: GameService,
    libraryService: LibraryService,
    gameProviderService: GameProviderService,
    settingsService: SettingsService,
    eventBus: EventBus
) : CommonData {

    override val games = gameService.games
    // The platform doesn't change often, so an unoptimized filter is acceptable here.
    override val platformGames =
        games.filtering(settingsService.game.platformChannel.subscribe().map(Dispatchers.Default) { platform ->
            { game: Game -> game.platform == platform }
        })

    override val genres = games.genres()
    override val platformGenres = platformGames.genres()
    private fun ListObservable<Game>.genres() = flatMapping { it.genres }.distincting().sortingBy { it }

    override val tags = games.tags()
    override val platformTags = platformGames.tags()
    private fun ListObservable<Game>.tags() = flatMapping { it.tags }.distincting().sortingBy { it }

    override val filterTags = games.filterTags()
    override val platformFilterTags = games.filterTags()
    private fun ListObservable<Game>.filterTags() = flatMapping { it.filterTags }.distincting().sortingBy { it }

    override val libraries = libraryService.libraries
    override val contentLibraries = libraries.filtering { it.type != LibraryType.Excluded }
    override val platformLibraries =
        contentLibraries.filtering(settingsService.game.platformChannel.subscribe().map(Dispatchers.Default) { platform ->
            { library: Library -> library.platform == platform }
        })

    override val allProviders = ListObservableImpl(gameProviderService.allProviders)
    override val platformProviders =
        allProviders.filtering(settingsService.game.platformChannel.subscribe().map(Dispatchers.Default) { platform ->
            { provider: GameProvider -> provider.supports(platform) }
        })
    override val enabledProviders = gameProviderService.enabledProviders

    override val platformsWithLibraries = contentLibraries.mapping { it.platform }.distincting()
    override val platformsWithEnabledProviders = enabledProviders.flatMapping { provider -> provider.supportedPlatforms }.distincting()

    override val isGameSyncRunning = MultiChannel.conflated(false).apply {
        eventBus.on<SyncGamesEvent.Started> { send(true) }
        eventBus.on<SyncGamesEvent.Finished> { send(false) }
    }
}