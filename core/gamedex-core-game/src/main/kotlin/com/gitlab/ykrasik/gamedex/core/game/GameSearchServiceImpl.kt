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

package com.gitlab.ykrasik.gamedex.core.game

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.core.util.ListEvent
import com.gitlab.ykrasik.gamedex.util.logger
import com.gitlab.ykrasik.gamedex.util.time
import com.miguelfonseca.completely.AutocompleteEngine
import com.miguelfonseca.completely.IndexAdapter
import com.miguelfonseca.completely.data.Indexable
import com.miguelfonseca.completely.data.ScoredObject
import com.miguelfonseca.completely.text.analyze.tokenize.WordTokenizer
import com.miguelfonseca.completely.text.analyze.transform.LowerCaseTransformer
import com.miguelfonseca.completely.text.index.PatriciaTrie
import com.miguelfonseca.completely.text.match.EditDistanceAutomaton
import org.slf4j.Logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 10/10/2018
 * Time: 22:04
 */
@Singleton
class GameSearchServiceImpl @Inject constructor(private val gameService: GameService) : GameSearchService {
    private val log = logger()

    private val platformEngines = Platform.values().associate { platform ->
        platform to AutocompleteEngine.Builder<IndexableGame>()
            .setIndex(GameAdapter())
            .setAnalyzers(LowerCaseTransformer(), WordTokenizer())
            .build()
    }

    init {
        log.time("Building search index...") {
            addGames(gameService.games)
        }
        gameService.games.changesChannel.subscribe { event ->
            when (event) {
                is ListEvent.ItemAdded -> addGame(event.item)
                is ListEvent.ItemsAdded -> addGames(event.items)
                is ListEvent.ItemRemoved -> removeGame(event.item)
                is ListEvent.ItemsRemoved -> removeGames(event.items)
                is ListEvent.ItemSet -> {
                    removeGame(event.prevItem)
                    addGame(event.item)
                }
                is ListEvent.ItemsSet -> {
                    if (event.prevItems.isNotEmpty()) {
                        removeGames(event.prevItems)
                    }
                    addGames(event.items)
                }
                else -> Unit
            }
        }
    }

    private fun addGame(game: Game) = game.platform.engine.add(game.toIndexable())
    private fun addGames(games: List<Game>) {
        val gamesByPlatform = games.groupBy { it.platform }
        gamesByPlatform.forEach { (platform, platformGames) ->
            platform.engine.addAll(platformGames.map { it.toIndexable() })
        }
    }

    private fun removeGame(game: Game) = check(game.platform.engine.remove(game.toIndexable())) { "Search index did not contain removed game: $game" }
    private fun removeGames(games: List<Game>) {
        val gamesByPlatform = games.groupBy { it.platform }
        gamesByPlatform.forEach { (platform, platformGames) ->
            check(platform.engine.removeAll(platformGames.map { it.toIndexable() })) { "Search index did not contain any removed games: $games" }
        }
    }

    override fun search(query: String, platform: Platform) =
        if (query.isBlank()) {
            gameService.games
        } else {
            log.time("Searching '$query'...", { timeTaken, results -> "${results.size} matches in $timeTaken" }, Logger::trace) {
                platform.engine.search(query).map { it.game }
            }
        }

    override fun suggest(query: String, platform: Platform, maxResults: Int) =
        if (query.isBlank()) {
            emptyList()
        } else {
            platform.engine.search(query, maxResults).mapNotNull { it.game.takeIf { it.platform == platform }?.name }
        }

    private val Platform.engine get() = platformEngines.getValue(this)

    private class GameAdapter : IndexAdapter<IndexableGame> {
        private val index = PatriciaTrie<IndexableGame>()

        override fun get(token: String): Collection<ScoredObject<IndexableGame>> {
            // Set threshold according to the token length
            val threshold = Math.log(Math.max(token.length - 1, 1).toDouble())
            return index.getAny(EditDistanceAutomaton(token, threshold))
        }

        override fun put(token: String, value: IndexableGame?): Boolean {
            return index.put(token, value)
        }

        override fun remove(value: IndexableGame): Boolean {
            return index.remove(value)
        }
    }

    private fun Game.toIndexable() = IndexableGame(this)

    private data class IndexableGame(val game: Game) : Indexable {
        override fun getFields() = listOf(game.name)
    }
}
