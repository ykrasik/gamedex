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

package com.gitlab.ykrasik.gamedex.core.game

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.game.SortBy
import com.gitlab.ykrasik.gamedex.app.api.game.SortOrder
import com.gitlab.ykrasik.gamedex.app.api.game.ViewWithGames
import com.gitlab.ykrasik.gamedex.app.api.util.filtering
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.PresenterFactory
import com.gitlab.ykrasik.gamedex.core.api.file.FileSystemService
import com.gitlab.ykrasik.gamedex.core.api.game.GameService
import com.gitlab.ykrasik.gamedex.core.filter.FilterContextImpl
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import kotlinx.coroutines.experimental.channels.map
import kotlinx.coroutines.experimental.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/06/2018
 * Time: 09:53
 */
@Singleton
class GamesPresenterFactory @Inject constructor(
    gameService: GameService,
    fileSystemService: FileSystemService,
    settingsService: SettingsService
) : PresenterFactory<ViewWithGames> {
    private val nameComparator = Comparator<Game> { o1, o2 -> o1.name.compareTo(o2.name, ignoreCase = true) }
    private val criticScoreComparator = compareBy(Game::criticScore)
    private val userScoreComparator = compareBy(Game::userScore)

    private val sortComparatorChannel = settingsService.game.sortChannel.subscribe().map { sort ->
        val comparator = when (sort.sortBy) {
            SortBy.name_ -> nameComparator
            SortBy.criticScore -> criticScoreComparator.then(nameComparator)
            SortBy.userScore -> userScoreComparator.then(nameComparator)
            SortBy.minScore -> compareBy<Game> { it.minScore }.then(criticScoreComparator).then(userScoreComparator).then(nameComparator)
            SortBy.avgScore -> compareBy<Game> { it.avgScore }.then(criticScoreComparator).then(userScoreComparator).then(nameComparator)
            SortBy.size -> compareBy<Game> { runBlocking { fileSystemService.size(it.path).await() } }.then(nameComparator)        // FIXME: Hangs UI thread!!!
            SortBy.releaseDate -> compareBy(Game::releaseDate).then(nameComparator)
            SortBy.updateDate -> compareBy(Game::updateDate)
        }
        if (sort.order == SortOrder.asc) {
            comparator
        } else {
            comparator.reversed()
        }
    }

    private val platformPredicateChannel = settingsService.game.platformChannel.subscribe().map { platform ->
        { game: Game -> game.platform == platform }
    }

    // The platform doesn't change that often, so an unoptimized filter is acceptable here.
    private val platformGames = gameService.games.filtering(platformPredicateChannel)

    private val filterPredicate = settingsService.game.currentPlatformSettingsChannel.map { settings ->
        val context = FilterContextImpl(emptyList(), fileSystemService)
        return@map { game: Game ->
            game.matchesSearchQuery(settings.search) &&
            runBlocking {
                settings.filter.evaluate(game, context)
            }
        }
    }

    override fun present(view: ViewWithGames) = object : Presenter() {
        init {
            platformGames.bindTo(view.games)
            sortComparatorChannel.subscribeOnUi { view.sort = it }
            filterPredicate.subscribeOnUi { view.filter = it }
        }
    }
}