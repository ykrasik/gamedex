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
import com.gitlab.ykrasik.gamedex.RawGame
import com.gitlab.ykrasik.gamedex.core.api.game.AddGameRequest
import com.gitlab.ykrasik.gamedex.core.api.game.GameRepository
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryRepository
import com.gitlab.ykrasik.gamedex.core.api.provider.GameProviderRepository
import com.gitlab.ykrasik.gamedex.core.api.task.Task
import com.gitlab.ykrasik.gamedex.core.api.util.ListChangeType
import com.gitlab.ykrasik.gamedex.core.api.util.ListObservable
import com.gitlab.ykrasik.gamedex.core.api.util.SubjectListObservable
import com.gitlab.ykrasik.gamedex.core.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.util.logger
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 25/12/2016
 * Time: 19:18
 */
@Singleton
class GameRepositoryImpl @Inject constructor(
    private val persistenceService: PersistenceService,
    private val gameFactory: GameFactory,
    libraryRepository: LibraryRepository,
    gameProviderRepository: GameProviderRepository
) : GameRepository {
    private val log = logger()

    private val _games = SubjectListObservable(fetchGames())
    override val games: ListObservable<Game> = _games

    init {
        libraryRepository.libraries.changesObservable.subscribe {
            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (it.type) {
                ListChangeType.Remove -> launch(CommonPool) { invalidate() }
                ListChangeType.Set -> launch(CommonPool) { rebuildGames() }
            }
        }
        gameProviderRepository.enabledProviders.changesObservable.subscribe {
            launch(CommonPool) { rebuildGames() }
        }
    }

    private fun fetchGames(): List<Game> {
        log.info("Fetching games...")
        val games = persistenceService.fetchGames().map { it.toGame() }
        log.info("Fetched ${games.size} games.")
        return games
    }

    override fun add(request: AddGameRequest): Game {
        val rawGame = persistenceService.insertGame(request.metadata.updatedNow(), request.providerData, request.userData)
        val game = rawGame.toGame()
        _games += game
        return game
    }

    // TODO: Get a reference to the task and update it?
    override suspend fun addAll(requests: List<AddGameRequest>, afterEach: (Game) -> Unit): List<Game> {
        // Write games to db in chunks of concurrent requests - wait until all games are written and then move on to the next chunk.
        // This is in order to allow the ui thread to run roughly once after every chunk.
        // Otherwise, it gets swamped during large operations (import large db) and UI appears to hang.
        @Suppress("NAME_SHADOWING")
        val games = requests.chunked(50).flatMap { requests ->
            requests.map { request ->
                async(CommonPool) {
                    // TODO: Batch insert?
                    persistenceService.insertGame(request.metadata, request.providerData, request.userData).toGame().also(afterEach)
                }
            }.map { it.await() }
        }

        _games += games
        return games
    }

    override fun replace(source: Game, target: RawGame): Game {
        val newGame = target.toGame()
        source.verifySuccess { persistenceService.updateGame(target.withMetadata { it.updatedNow() }) }
        _games.replace(source, newGame)
        return newGame
    }

    override fun delete(game: Game) {
        log.info("Deleting '${game.name}'...")
        game.verifySuccess { persistenceService.deleteGame(game.id) }
        _games -= game
        log.info("Deleting '${game.name}': Done.")
    }

    override fun deleteAll(games: List<Game>, task: Task<*>) {
        if (games.isEmpty()) return

        task.totalWork = games.size
        @Suppress("NAME_SHADOWING")
        games.chunked(200).forEach { games ->
            require(persistenceService.deleteGames(games.map { it.id }) == games.size) { "Not all games to be deleted existed: $games" }
            task.incProgress(games.size)
        }

        _games -= games
    }

    override fun deleteAllUserData() {
        persistenceService.clearUserData()
        _games.set(_games.map { it.rawGame.copy(userData = null).toGame() })
    }

    override fun invalidate() {
        // Re-fetch all games from persistence
        _games.set(fetchGames())
    }

    private fun rebuildGames() = _games.set(_games.map { it.rawGame.toGame() })

    private fun RawGame.toGame(): Game = gameFactory.create(this)

    override fun get(id: Int): Game = _games.find { it.id == id }
        ?: throw IllegalArgumentException("Game doesn't exist: id=$id")

    private fun Game.verifySuccess(f: () -> Boolean) = require(f()) { "Game doesn't exist: $this" }
}