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

package com.gitlab.ykrasik.gamedex.core.game.presenter

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.app.api.filter.isEmpty
import com.gitlab.ykrasik.gamedex.app.api.game.SortBy
import com.gitlab.ykrasik.gamedex.app.api.game.SortOrder
import com.gitlab.ykrasik.gamedex.app.api.game.ViewWithGames
import com.gitlab.ykrasik.gamedex.app.api.util.MultiChannel
import com.gitlab.ykrasik.gamedex.core.CommonData
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.filter.FilterService
import com.gitlab.ykrasik.gamedex.core.game.GameSearchService
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import com.gitlab.ykrasik.gamedex.util.setAll
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
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
    private val settingsService: SettingsService
) : Presenter<ViewWithGames> {
    private val nameComparator = Comparator<Game> { o1, o2 -> o1.name.compareTo(o2.name, ignoreCase = true) }
    private val criticScoreComparator = compareBy(Game::criticScore)
    private val userScoreComparator = compareBy(Game::userScore)

    override fun present(view: ViewWithGames) = object : ViewSession() {
        private val searchText = MultiChannel.conflated("")

        init {
            commonData.platformGames.bind(view.games)

            view.searchText.debounce().forEach { onSearchTextChanged(it) }

            settingsService.game.sortByChannel.combineLatest(settingsService.game.sortOrderChannel).forEach { (sortBy, sortOrder) ->
                setSort(sortBy, sortOrder)
            }

            var platformFilterJob: Job? = null
            settingsService.currentPlatformSettingsChannel.forEach { settings ->
                platformFilterJob?.cancelAndJoin()
                platformFilterJob = searchText.combineLatest(settings.filterChannel).forEach { (search, filter) ->
                    setSearchAndFilter(search, filter, settings.platform)
                }
            }
        }

        private fun onSearchTextChanged(searchText: String) {
            this.searchText.offer(searchText)
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

        private fun setSearchAndFilter(search: String, filter: Filter, platform: Platform) {
            val searchedGames = gameSearchService.search(search, platform)
            val matchingGames = filterService.filter(searchedGames, filter)

            val suggestions = if (search.isNotBlank()) matchingGames.take(12) else emptyList()
            view.searchSuggestions.setAll(suggestions)
            view.isShowSearchSuggestions *= suggestions.isNotEmpty()

            val matchingGameIds = matchingGames.mapTo(mutableSetOf()) { it.id }
            view.filter *= { game: Game ->
                filter.isEmpty && search.isBlank() || matchingGameIds.contains(game.id)
            }
        }
    }
}