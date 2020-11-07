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
import com.gitlab.ykrasik.gamedex.core.util.FlowScope
import com.gitlab.ykrasik.gamedex.core.util.ListObservableImpl
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.gitlab.ykrasik.gamedex.util.and
import com.google.inject.ImplementedBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 17/06/2018
 * Time: 10:32
 */
@ImplementedBy(CommonDataImpl::class)
interface CommonData {
    val games: StateFlow<List<Game>>
    val currentPlatformGames: StateFlow<List<Game>>

    val genres: StateFlow<List<Genre>>
    val currentPlatformGenres: StateFlow<List<Genre>>

    val tags: StateFlow<List<TagId>>
    val currentPlatformTags: StateFlow<List<TagId>>

    val filterTags: StateFlow<List<TagId>>
    val currentPlatformFilterTags: StateFlow<List<TagId>>

    val libraries: StateFlow<List<Library>>
    val contentLibraries: StateFlow<List<Library>>
    val currentPlatformLibraries: StateFlow<List<Library>>

    val allProviders: List<GameProvider.Metadata>
    val currentPlatformProviders: StateFlow<List<GameProvider.Metadata>>
    val enabledProviders: StateFlow<List<GameProvider.Metadata>>

    val platformsWithLibraries: StateFlow<List<Platform>>
    val platformsWithEnabledProviders: StateFlow<List<Platform>>

    val isGameSyncRunning: StateFlow<Boolean>
    val disableWhenGameSyncIsRunning: Flow<IsValid>
    val canSyncOrUpdateGames: Flow<IsValid>
}

@Singleton
class CommonDataImpl @Inject constructor(
    gameService: GameService,
    libraryService: LibraryService,
    gameProviderService: GameProviderService,
    syncGameService: SyncGameService,
    settingsRepo: GameSettingsRepository,   // TODO: Consider hiding this class, send 'currentPlatform' events to the eventBus and have each service expose what this service does?
) : FlowScope(Dispatchers.Default, baseDebugName = "CommonDataImpl"), CommonData {

    override val games = gameService.games.items

    override val currentPlatformGames = games.combine(settingsRepo.platform) { games, platform ->
        games.filter { game -> platform.matches(game.platform) }
    }.toMutableStateFlow("currentPlatformGames")

    override val genres = games.genres("genres")
    override val currentPlatformGenres = currentPlatformGames.genres("currentPlatformGenres")
    private fun Flow<List<Game>>.genres(debugName: String) = map { it.flatMap { it.genres }.distinct().sortedBy { it.id } }.toMutableStateFlow(debugName)

    override val tags = games.tags("tags")
    override val currentPlatformTags = currentPlatformGames.tags("currentPlatformTags")
    private fun Flow<List<Game>>.tags(debugName: String) = map { it.flatMap { it.tags }.distinct().sortedBy { it } }.toMutableStateFlow(debugName)

    override val filterTags = games.filterTags("filterTags")
    override val currentPlatformFilterTags = games.filterTags("currentPlatformFilterTags")
    private fun Flow<List<Game>>.filterTags(debugName: String) = map { it.flatMap { it.filterTags }.distinct().sortedBy { it } }.toMutableStateFlow(debugName)

    override val libraries = libraryService.libraries.items
    override val contentLibraries = libraries.map { it.filter { it.type != LibraryType.Excluded } }.toMutableStateFlow("contentLibraries")
    override val currentPlatformLibraries = contentLibraries.combine(settingsRepo.platform) { libraries, platform ->
        libraries.filter { library -> platform.matches(library.platform) }
    }.toMutableStateFlow("currentPlatformLibraries")

    override val allProviders = ListObservableImpl(gameProviderService.allProviders)
    override val currentPlatformProviders = settingsRepo.platform.map { platform ->
        allProviders.filter { provider ->
            when (platform) {
                is AvailablePlatform.All -> true
                is AvailablePlatform.SinglePlatform -> provider.supports(platform.platform)
            }
        }
    }.toMutableStateFlow("currentPlatformProviders")
    override val enabledProviders = gameProviderService.enabledProviders.items

    override val platformsWithLibraries = contentLibraries.map { libraries -> libraries.map { it.platform }.distinct() }
        .toMutableStateFlow("platformsWithLibraries")
    override val platformsWithEnabledProviders = enabledProviders.map { providers -> providers.flatMap { it.supportedPlatforms }.distinct() }
        .toMutableStateFlow("platformsWithEnabledProviders")

    override val isGameSyncRunning = syncGameService.isGameSyncRunning
    override val disableWhenGameSyncIsRunning = isGameSyncRunning.map {
        IsValid { check(!it) { "Game sync in progress!" } }
    }
    override val canSyncOrUpdateGames: Flow<IsValid> =
        combine(disableWhenGameSyncIsRunning, contentLibraries, platformsWithEnabledProviders) { disableWhenGameSyncIsRunning, libraries, platformsWithEnabledProviders ->
            disableWhenGameSyncIsRunning and IsValid {
                check(libraries.isNotEmpty()) { "Please add at least 1 library!" }
                check(platformsWithEnabledProviders.isNotEmpty()) { "Enable at least 1 provider!" }
                check(libraries.any { it.platform in platformsWithEnabledProviders }) { "Enable a provider that supports your platform!" }
            }
        }

    private fun <T> Flow<List<T>>.toMutableStateFlow(debugName: String): MutableStateFlow<List<T>> =
        toMutableStateFlow(emptyList(), debugName, traceValues = false)
}