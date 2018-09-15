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

import com.gitlab.ykrasik.gamedex.RawGame
import com.gitlab.ykrasik.gamedex.app.api.util.ListObservableImpl
import com.gitlab.ykrasik.gamedex.core.api.game.AddGameRequest
import com.gitlab.ykrasik.gamedex.core.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.util.logger
import com.gitlab.ykrasik.gamedex.util.millisTaken
import com.gitlab.ykrasik.gamedex.util.toHumanReadableDuration
import kotlinx.coroutines.experimental.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 25/12/2016
 * Time: 19:18
 */
@Singleton
internal class GameRepository @Inject constructor(private val persistenceService: PersistenceService) {
    private val log = logger()

    val games = ListObservableImpl(fetchGames())

    private fun fetchGames(): List<RawGame> {
        log.info("Fetching games...")
        val (games, millisTaken) = millisTaken { persistenceService.fetchGames() }
        log.info("Fetched ${games.size} games in ${millisTaken.toHumanReadableDuration()}")
        return games
    }

    fun add(request: AddGameRequest): RawGame {
        val game = persistenceService.insertGame(request.metadata.updatedNow(), request.providerData, request.userData)
        games += game
        return game
    }

    suspend fun addAll(requests: List<AddGameRequest>, afterEach: (RawGame) -> Unit): List<RawGame> {
        // Write games to db in chunks of concurrent requests - wait until all games are written and then move on to the next chunk.
        // This is in order to allow the ui thread to run roughly once after every chunk.
        // Otherwise, it gets swamped during large operations (import large db) and UI appears to hang.
        val games = requests.map { request ->
            GlobalScope.async(Dispatchers.IO) {
                // TODO: Batch insert?
                persistenceService.insertGame(request.metadata, request.providerData, request.userData).also(afterEach)
            }
        }.map { it.await() }

        this.games += games
        return games
    }

    fun replace(source: RawGame, target: RawGame) {
        source.verifySuccess { persistenceService.updateGame(target) }
        games.replace(source, target)
    }

    fun delete(game: RawGame) {
        game.verifySuccess { persistenceService.deleteGame(game.id) }
        games -= game
    }

    fun deleteAll(games: List<RawGame>) {
        require(persistenceService.deleteGames(games.map { it.id }) == games.size) { "Not all games to be deleted existed: $games" }
        this.games -= games
    }

    fun deleteAllUserData() {
        persistenceService.clearUserData()
        games.setAll(games.map { it.copy(userData = null) })
    }

    suspend fun invalidate() = withContext(Dispatchers.IO) {
        // Re-fetch all games from persistence
        games.setAll(fetchGames())
    }

    private fun RawGame.verifySuccess(f: () -> Boolean) = require(f()) { "Game doesn't exist: $this" }
}