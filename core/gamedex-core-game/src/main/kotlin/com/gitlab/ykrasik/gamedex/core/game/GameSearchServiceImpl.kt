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
import com.gitlab.ykrasik.gamedex.app.api.util.*
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

    private val engine = AutocompleteEngine.Builder<IndexableGame>()
        .setIndex(GameAdapter())
        .setAnalyzers(LowerCaseTransformer(), WordTokenizer())
        .build()

    init {
        log.time("Building search index...") {
            engine.addAll(gameService.games.map { IndexableGame(it) })
        }
        gameService.games.changesChannel.subscribe { event ->
            when (event) {
                is ListItemAddedEvent -> addGame(event.item)
                is ListItemsAddedEvent -> addGames(event.items)
                is ListItemRemovedEvent -> removeGame(event.item)
                is ListItemsRemovedEvent -> removeGames(event.items)
                is ListItemSetEvent -> {
                    removeGame(event.prevItem)
                    addGame(event.item)
                }
                is ListItemsSetEvent -> {
                    if (event.prevItems.isNotEmpty()) {
                        removeGames(event.prevItems)
                    }
                    addGames(event.items)
                }
            }
        }
    }

    private fun addGame(game: Game) = engine.add(game.toIndexable())
    private fun addGames(games: List<Game>) = engine.addAll(games.map { it.toIndexable() })
    private fun removeGame(game: Game) = check(engine.remove(game.toIndexable())) { "Search index did not contain removed game: $game" }
    private fun removeGames(games: List<Game>) = check(engine.removeAll(games.map { it.toIndexable() })) { "Search index did not contain any removed games: $games" }

    override fun search(query: String) =
        if (query.isBlank())
            gameService.games
        else log.time("Searching '$query'...", { timeTaken, results -> "${results.size} matches in $timeTaken" }, Logger::trace) {
            engine.search(query).map { it.game }
        }
}

private fun Game.toIndexable() = IndexableGame(this)

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

private data class IndexableGame(val game: Game) : Indexable {
    override fun getFields() = listOf(game.name)
}
