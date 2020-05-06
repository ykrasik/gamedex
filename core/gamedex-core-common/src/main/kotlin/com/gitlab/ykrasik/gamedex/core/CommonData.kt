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

package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.app.api.game.AvailablePlatform
import com.gitlab.ykrasik.gamedex.app.api.util.StatefulMultiReadChannel
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.core.provider.SyncGameService
import com.gitlab.ykrasik.gamedex.core.settings.GameSettingsRepository
import com.gitlab.ykrasik.gamedex.core.util.*
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.gitlab.ykrasik.gamedex.util.Try
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

    val genres: ListObservable<Genre>
    val platformGenres: ListObservable<Genre>

    val tags: ListObservable<TagId>
    val platformTags: ListObservable<TagId>

    val filterTags: ListObservable<TagId>
    val platformFilterTags: ListObservable<TagId>

    val libraries: ListObservable<Library>
    val contentLibraries: ListObservable<Library>
    val platformLibraries: ListObservable<Library>

    val allProviders: ListObservable<GameProvider.Metadata>
    val platformProviders: ListObservable<GameProvider.Metadata>
    val enabledProviders: ListObservable<GameProvider.Metadata>

    val platformsWithLibraries: ListObservable<Platform>
    val platformsWithEnabledProviders: ListObservable<Platform>

    val isGameSyncRunning: StatefulMultiReadChannel<Boolean>
    val canSyncOrUpdateGames: StatefulMultiReadChannel<IsValid>
}

@Singleton
class CommonDataImpl @Inject constructor(
    gameService: GameService,
    libraryService: LibraryService,
    gameProviderService: GameProviderService,
    syncGameService: SyncGameService,
    settingsRepo: GameSettingsRepository
) : CommonData {

    override val games = gameService.games
    // The platform doesn't change often, so an unoptimized filter is acceptable here.
    override val platformGames =
        games.filtering(settingsRepo.platform.subscribe().map(Dispatchers.Default) { platform ->
            { game: Game -> platform.matches(game.platform) }
        })

    override val genres = games.genres()
    override val platformGenres = platformGames.genres()
    private fun ListObservable<Game>.genres() = flatMapping { it.genres }.distincting().sortingBy { it.id }

    override val tags = games.tags()
    override val platformTags = platformGames.tags()
    private fun ListObservable<Game>.tags() = flatMapping { it.tags }.distincting().sortingBy { it }

    override val filterTags = games.filterTags()
    override val platformFilterTags = games.filterTags()
    private fun ListObservable<Game>.filterTags() = flatMapping { it.filterTags }.distincting().sortingBy { it }

    override val libraries = libraryService.libraries
    override val contentLibraries = libraries.filtering { it.type != LibraryType.Excluded }
    override val platformLibraries =
        contentLibraries.filtering(settingsRepo.platform.subscribe().map(Dispatchers.Default) { platform ->
            { library: Library -> platform.matches(library.platform) }
        })

    override val allProviders = ListObservableImpl(gameProviderService.allProviders)
    override val platformProviders =
        allProviders.filtering(settingsRepo.platform.subscribe().map(Dispatchers.Default) { platform ->
            { provider: GameProvider.Metadata ->
                when (platform) {
                    is AvailablePlatform.All -> true
                    is AvailablePlatform.SinglePlatform -> provider.supports(platform.platform)
                }
            }
        })
    override val enabledProviders = gameProviderService.enabledProviders

    override val platformsWithLibraries = contentLibraries.mapping { it.platform }.distincting()
    override val platformsWithEnabledProviders = enabledProviders.flatMapping { provider -> provider.supportedPlatforms }.distincting()

    override val isGameSyncRunning = syncGameService.isGameSyncRunning
    override val canSyncOrUpdateGames: StatefulMultiReadChannel<IsValid> =
        contentLibraries.itemsChannel.combineLatest(platformsWithEnabledProviders.itemsChannel, isGameSyncRunning) { libraries, platformsWithEnabledProviders, isGameSyncRunning ->
            Try {
                check(!isGameSyncRunning) { "Game sync in progress!" }
                check(libraries.isNotEmpty()) { "Please add at least 1 library!" }
                check(platformsWithEnabledProviders.isNotEmpty()) { "Enable at least 1 provider!" }
                check(libraries.any { it.platform in platformsWithEnabledProviders }) { "Enable a provider that supports your platform!" }
            }
        }
}