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

package com.gitlab.ykrasik.gamedex.core.game.presenter

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameId
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.app.api.filter.isEmpty
import com.gitlab.ykrasik.gamedex.app.api.game.SortBy
import com.gitlab.ykrasik.gamedex.app.api.game.SortOrder
import com.gitlab.ykrasik.gamedex.app.api.game.ViewWithGames
import com.gitlab.ykrasik.gamedex.app.api.util.MultiChannel
import com.gitlab.ykrasik.gamedex.app.api.util.combineLatest
import com.gitlab.ykrasik.gamedex.core.CommonData
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.filter.FilterService
import com.gitlab.ykrasik.gamedex.core.game.CurrentPlatformFilterRepository
import com.gitlab.ykrasik.gamedex.core.game.GameEvent
import com.gitlab.ykrasik.gamedex.core.game.GameSearchService
import com.gitlab.ykrasik.gamedex.core.settings.GameSettingsRepository
import com.gitlab.ykrasik.gamedex.util.logger
import com.gitlab.ykrasik.gamedex.util.time
import org.slf4j.Logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/06/2018
 * Time: 09:53
 */
@Singleton
class GamesPresenter @Inject constructor(
    private val commonData: CommonData,
    private val filterService: FilterService,
    private val gameSearchService: GameSearchService,
    private val settingsRepo: GameSettingsRepository,
    private val repo: CurrentPlatformFilterRepository,
    private val eventBus: EventBus
) : Presenter<ViewWithGames> {
    private val log = logger()

    override fun present(view: ViewWithGames) = object : ViewSession() {
        init {
            commonData.platformGames.bind(view.games)

            settingsRepo.sortByChannel.combineLatest(settingsRepo.sortOrderChannel).forEach { (sortBy, sortOrder) ->
                setSort(sortBy, sortOrder)
            }

            view.searchText.debounce().forEach { onSearchTextChanged(it) }

            val searchText = MultiChannel.conflated("")
            view.searchActions.forEach { searchText.offer(view.searchText.value) }

            searchText.subscribe().combineLatest(repo.currentPlatformFilter.subscribe()).forEach { (search, filter) ->
                setSearchAndFilter(search, filter)
            }

            eventBus.forEach<GameEvent> {
                setSearchAndFilter(searchText.peek(), repo.currentPlatformFilter.peek())
            }
        }

        private fun setSort(sortBy: SortBy, sortOrder: SortOrder) {
            val comparator = when (sortBy) {
                SortBy.Name -> nameComparator
                SortBy.CriticScore -> criticScoreComparator.then(nameComparator)
                SortBy.UserScore -> userScoreComparator.then(nameComparator)
                SortBy.MinScore -> compareBy<Game> { it.minScore }.then(criticScoreComparator).then(userScoreComparator).then(nameComparator)
                SortBy.MaxScore -> compareBy<Game> { it.maxScore }.then(criticScoreComparator).then(userScoreComparator).then(nameComparator)
                SortBy.AvgScore -> compareBy<Game> { it.avgScore }.then(criticScoreComparator).then(userScoreComparator).then(nameComparator)
                SortBy.Size -> compareBy<Game> { it.fileTree.value?.size }.then(nameComparator)
                SortBy.ReleaseDate -> compareBy(Game::releaseDate).then(nameComparator)
                SortBy.CreateDate -> compareBy(Game::createDate)
                SortBy.UpdateDate -> compareBy(Game::updateDate)
            }

            view.sort *= if (sortOrder == SortOrder.Asc) {
                comparator
            } else {
                comparator.reversed()
            }
        }

        private fun onSearchTextChanged(query: String) {
            val suggestions = if (query.isNotBlank()) {
                search(query).take(20).toList()
            } else {
                emptyList()
            }
            view.searchSuggestions.setAll(suggestions)
        }

        private fun setSearchAndFilter(query: String, filter: Filter) {
            val isEmpty = filter.isEmpty && query.isBlank()
            val matchingGameIds: Set<GameId> = if (!isEmpty) {
                log.time("Filtering games...", { timeTaken, results -> "${results.size} results in $timeTaken" }, Logger::trace) {
                    val searchedGames = search(query)
                    val matchingGames = filterService.filter(searchedGames, filter)
                    matchingGames.mapTo(mutableSetOf()) { it.id }
                }
            } else {
                emptySet()
            }
            view.filter *= { game: Game ->
                isEmpty || matchingGameIds.contains(game.id)
            }
        }

        private fun search(query: String): Sequence<Game> =
            gameSearchService.search(query).asSequence()
                .filter { settingsRepo.platform.matches(it.platform) }
    }

    private companion object {
        val nameComparator = Comparator<Game> { o1, o2 -> o1.name.compareTo(o2.name, ignoreCase = true) }
        val criticScoreComparator = compareBy(Game::criticScore)
        val userScoreComparator = compareBy(Game::userScore)
    }
}