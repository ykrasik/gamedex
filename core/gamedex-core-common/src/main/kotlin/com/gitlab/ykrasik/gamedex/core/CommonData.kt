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
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.core.provider.SyncGameService
import com.gitlab.ykrasik.gamedex.core.settings.GameSettingsRepository
import com.gitlab.ykrasik.gamedex.core.util.ListObservable
import com.gitlab.ykrasik.gamedex.core.util.ListObservableImpl
import com.gitlab.ykrasik.gamedex.core.util.filterObservable
import com.gitlab.ykrasik.gamedex.core.util.transform
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.gitlab.ykrasik.gamedex.util.and
import com.google.inject.ImplementedBy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
    val currentPlatformGames: ListObservable<Game>

    val genres: ListObservable<Genre>
    val currentPlatformGenres: ListObservable<Genre>

    val tags: ListObservable<TagId>
    val currentPlatformTags: ListObservable<TagId>

    val filterTags: ListObservable<TagId>
    val currentPlatformFilterTags: ListObservable<TagId>

    val libraries: ListObservable<Library>
    val contentLibraries: ListObservable<Library>
    val currentPlatformLibraries: ListObservable<Library>

    val allProviders: ListObservable<GameProvider.Metadata>
    val currentPlatformProviders: ListObservable<GameProvider.Metadata>
    val enabledProviders: ListObservable<GameProvider.Metadata>

    val platformsWithLibraries: ListObservable<Platform>
    val platformsWithEnabledProviders: ListObservable<Platform>

    val isGameSyncRunning: Flow<Boolean>
    val disableWhenGameSyncIsRunning: Flow<IsValid>
    val canSyncOrUpdateGames: Flow<IsValid>
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

    override val currentPlatformGames = games.filterObservable(settingsRepo.platform.map { platform ->
        { game: Game -> platform.matches(game.platform) }
    })

    override val genres = games.genres()
    override val currentPlatformGenres = currentPlatformGames.genres()
    private fun ListObservable<Game>.genres() = transform { it.flatMap { it.genres }.distinct().sortedBy { it.id } }

    override val tags = games.tags()
    override val currentPlatformTags = currentPlatformGames.tags()
    private fun ListObservable<Game>.tags() = transform { it.flatMap { it.tags }.distinct().sortedBy { it } }

    override val filterTags = games.filterTags()
    override val currentPlatformFilterTags = games.filterTags()
    private fun ListObservable<Game>.filterTags() = transform { it.flatMap { it.filterTags }.distinct().sortedBy { it } }

    override val libraries = libraryService.libraries
    override val contentLibraries = libraries.filterObservable { it.type != LibraryType.Excluded }
    override val currentPlatformLibraries = contentLibraries.filterObservable(settingsRepo.platform.map { platform ->
        { library: Library -> platform.matches(library.platform) }
    })

    override val allProviders = ListObservableImpl(gameProviderService.allProviders)
    override val currentPlatformProviders = allProviders.filterObservable(settingsRepo.platform.map { platform ->
        { provider: GameProvider.Metadata ->
            when (platform) {
                is AvailablePlatform.All -> true
                is AvailablePlatform.SinglePlatform -> provider.supports(platform.platform)
            }
        }
    })
    override val enabledProviders = gameProviderService.enabledProviders

    override val platformsWithLibraries = contentLibraries.transform { libraries -> libraries.map { it.platform }.distinct() }
    override val platformsWithEnabledProviders = enabledProviders.transform { providers -> providers.flatMap { it.supportedPlatforms }.distinct() }

    override val isGameSyncRunning = syncGameService.isGameSyncRunning
    override val disableWhenGameSyncIsRunning = isGameSyncRunning.map {
        IsValid { check(!it) { "Game sync in progress!" } }
    }
    override val canSyncOrUpdateGames: Flow<IsValid> =
        combine(disableWhenGameSyncIsRunning, contentLibraries.items, platformsWithEnabledProviders.items) { disableWhenGameSyncIsRunning, libraries, platformsWithEnabledProviders ->
            disableWhenGameSyncIsRunning and IsValid {
                check(libraries.isNotEmpty()) { "Please add at least 1 library!" }
                check(platformsWithEnabledProviders.isNotEmpty()) { "Enable at least 1 provider!" }
                check(libraries.any { it.platform in platformsWithEnabledProviders }) { "Enable a provider that supports your platform!" }
            }
        }
}