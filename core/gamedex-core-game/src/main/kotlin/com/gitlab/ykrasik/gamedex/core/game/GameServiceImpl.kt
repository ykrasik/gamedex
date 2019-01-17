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
import com.gitlab.ykrasik.gamedex.GameId
import com.gitlab.ykrasik.gamedex.RawGame
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import com.gitlab.ykrasik.gamedex.core.task.Task
import com.gitlab.ykrasik.gamedex.core.task.task
import com.gitlab.ykrasik.gamedex.core.util.ListEvent
import com.gitlab.ykrasik.gamedex.core.util.ListObservableImpl
import com.gitlab.ykrasik.gamedex.core.util.broadcastTo
import com.gitlab.ykrasik.gamedex.core.util.mapping
import com.gitlab.ykrasik.gamedex.util.logger
import com.gitlab.ykrasik.gamedex.util.time
import com.gitlab.ykrasik.gamedex.util.toFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 26/04/2018
 * Time: 19:51
 */
@Singleton
class GameServiceImpl @Inject constructor(
    private val repo: GameRepository,
    private val gameFactory: GameFactory,
    eventBus: EventBus,
    libraryService: LibraryService,
    settingsService: SettingsService
) : GameService {
    private val log = logger()

    override val games = log.time("Processing games...") {
        repo.games.mapping { it.toGame() } as ListObservableImpl<Game>
    }

    init {
        games.broadcastTo(eventBus, Game::id, ::GamesAddedEvent, ::GamesDeletedEvent, ::GamesUpdatedEvent)

        libraryService.libraries.changesChannel.subscribe {
            when (it) {
                is ListEvent.RemoveEvent -> repo.invalidate()
                is ListEvent.SetEvent -> rebuildGames()
                else -> Unit
            }
        }
        settingsService.providerOrder.dataChannel.drop(1).subscribe {
            rebuildGames()
        }
    }

    override fun add(request: AddGameRequest): Task<Game> {
        val nameBestEffort = request.providerData.firstOrNull()?.gameData?.name ?: request.metadata.path.toFile().name
        return task("Adding Game '$nameBestEffort'...") {
            val game = repo.add(request).toGame()
            successMessage = { "Added Game: '${game.name}'." }
            game
        }
    }

    override fun addAll(requests: List<AddGameRequest>) = task("Adding ${requests.size} Games...") {
        successMessage = { "Added $processedItems/$totalItems Games." }

        totalItems = requests.size
        repo.games.buffered {
            requests.chunked(50).flatMap { requests ->
                repo.addAll(requests) { incProgress() }.map { it.toGame() }
            }
        }
    }

    override fun replace(source: Game, target: RawGame) = task("Updating Game '${source.name}'...") {
        val updatedTarget = target.withMetadata { it.updatedNow() }
        repo.replace(source.rawGame, updatedTarget)
        val updatedGame = updatedTarget.toGame()
        successMessage = { "Updated Game: '${updatedGame.name}'." }
        updatedGame
    }

    override fun delete(game: Game) = task("Deleting Game '${game.name}'...") {
        successMessage = { "Deleted Game: '${game.name}'." }
        repo.delete(game.rawGame)
    }

    override fun deleteAll(games: List<Game>) = task("Deleting ${games.size} Games...") {
        successMessage = { "Deleted $processedItems/$totalItems Games." }

        totalItems = games.size
        repo.games.buffered {
            games.chunked(200).forEach { chunk ->
                repo.deleteAll(chunk.map { it.rawGame })
                incProgress(chunk.size)
            }
        }
    }

    override fun deleteAllUserData() = task("Deleting all user data...") {
        successMessage = { "Deleted all user data." }
        repo.deleteAllUserData()
    }

    override fun buildGame(rawGame: RawGame) = rawGame.toGame()

    private fun rebuildGames() = repo.games.touch()

    // FIXME: ineffective for large collections
    override fun get(id: GameId): Game = games.find { it.id == id }
        ?: throw IllegalArgumentException("Game doesn't exist: id=$id")

    private fun RawGame.toGame(): Game = gameFactory.create(this)
}