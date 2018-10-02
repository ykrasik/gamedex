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

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.app.api.util.*
import com.gitlab.ykrasik.gamedex.core.api.game.GameService
import com.gitlab.ykrasik.gamedex.core.api.provider.EnabledGameProvider
import com.gitlab.ykrasik.gamedex.core.api.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.core.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.google.inject.ImplementedBy
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 17/06/2018
 * Time: 10:32
 */
@ImplementedBy(CommonDataImpl::class)
// TODO: not really an interface, but the implementation is more convenient than a data class.
interface CommonData {
    val games: ListObservable<Game>
    val genres: ListObservable<String>
    val tags: ListObservable<String>

    val platformGames: ListObservable<Game>
    val platformGenres: ListObservable<String>
    val platformTags: ListObservable<String>

    val libraries: ListObservable<Library>
    val realLibraries: ListObservable<Library>
    val platformLibraries: ListObservable<Library>

    val allProviders: List<GameProvider>
    val enabledProviders: ListObservable<EnabledGameProvider>

    val platformsWithLibraries: ListObservable<Platform>
}

@Singleton
class CommonDataImpl @Inject constructor(
    gameService: GameService,
    libraryService: LibraryService,
    gameProviderService: GameProviderService,
    settingsService: SettingsService
) : CommonData {

    override val games = gameService.games
    override val genres = games.genres()
    override val tags = games.tags()

    // The platform doesn't change that often, so an unoptimized filter is acceptable here.
    override val platformGames =
        games.filtering(settingsService.game.platformChannel.subscribe().map(Dispatchers.Default) { platform ->
            { game: Game -> game.platform == platform }
        })
    override val platformGenres = platformGames.genres()
    override val platformTags = platformGames.tags()

    private fun ListObservable<Game>.genres() = flatMapping { it.genres }.distincting().sortingBy { it }
    private fun ListObservable<Game>.tags() = flatMapping { it.tags }.distincting().sortingBy { it }

    override val libraries = libraryService.libraries
    override val realLibraries = libraries.filtering { it.platform != Platform.excluded }
    override val platformLibraries =
        realLibraries.filtering(settingsService.game.platformChannel.subscribe().map(Dispatchers.Default) { platform ->
            { library: Library -> library.platform == platform }
        })

    override val allProviders = gameProviderService.allProviders
    override val enabledProviders = gameProviderService.enabledProviders

    override val platformsWithLibraries = realLibraries.mapping { it.platform }.distincting()
}