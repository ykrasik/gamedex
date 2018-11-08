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
import com.gitlab.ykrasik.gamedex.GameId
import com.gitlab.ykrasik.gamedex.RawGame
import com.gitlab.ykrasik.gamedex.app.api.util.ListChangeType
import com.gitlab.ykrasik.gamedex.app.api.util.ListObservableImpl
import com.gitlab.ykrasik.gamedex.app.api.util.mapping
import com.gitlab.ykrasik.gamedex.core.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import com.gitlab.ykrasik.gamedex.core.task.Task
import com.gitlab.ykrasik.gamedex.core.task.task
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
    libraryService: LibraryService,
    settingsService: SettingsService
) : GameService {
    override val games = repo.games.mapping { it.toGame() }

    init {
        libraryService.libraries.changesChannel.subscribe {
            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (it.type) {
                ListChangeType.Remove -> repo.invalidate()
                ListChangeType.Set -> rebuildGames()
            }
        }
        settingsService.providerOrder.dataChannel.subscribe {
            // This gets called immediately when the object is created, in effect twice.
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

    override fun invalidate() = repo.invalidate()

    // ugly cast, whatever.
    private fun rebuildGames() = (games as ListObservableImpl<Game>).setAll(games.map { it.rawGame.toGame() })

    // FIXME: ineffective for large collections
    override fun get(id: GameId): Game = games.find { it.id == id }
        ?: throw IllegalArgumentException("Game doesn't exist: id=$id")

    private fun RawGame.toGame(): Game = gameFactory.create(this)
}