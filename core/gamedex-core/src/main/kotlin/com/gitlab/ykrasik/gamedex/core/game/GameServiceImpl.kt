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
import com.gitlab.ykrasik.gamedex.app.api.util.*
import com.gitlab.ykrasik.gamedex.core.api.game.AddGameRequest
import com.gitlab.ykrasik.gamedex.core.api.game.GameService
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.api.provider.GameProviderRepository
import kotlinx.coroutines.experimental.CommonPool
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 26/04/2018
 * Time: 19:51
 */
@Singleton
internal class GameServiceImpl @Inject constructor(
    private val repo: GameRepository,
    private val gameFactory: GameFactory,
    libraryService: LibraryService,
    gameProviderRepository: GameProviderRepository  // FIXME: Go through service!!!
) : GameService {
    init {
        libraryService.libraries.changesChannel.subscribe(CommonPool) {
            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (it.type) {
                ListChangeType.Remove -> repo.invalidate()
                ListChangeType.Set -> rebuildGames()
            }
        }
        gameProviderRepository.enabledProviders.changesChannel.subscribe(CommonPool) {
            rebuildGames()
        }
    }

    override val games = repo.games.mapping { it.toGame() } as ListObservableImpl<Game> // ugly cast, whatever.

    override fun add(request: AddGameRequest) = quickTask("Adding Game '${request.metadata.path}'...") {
        message1 = "Adding Game '${request.metadata.path}'..."
        doneMessage { "Added Game: '${request.metadata.path}'." }
        repo.add(request).toGame()
    }

    override fun addAll(requests: List<AddGameRequest>) = task("Adding ${requests.size} Games...") {
        message1 = "Adding ${requests.size} Games..."
        doneMessage { "Added $processed/$totalWork Games." }

        totalWork = requests.size
        this@GameServiceImpl.games.buffered {
            requests.chunked(50).flatMap { requests ->
                repo.addAll(requests) { incProgress() }.map { it.toGame() }
            }
        }
    }

    override fun replace(source: Game, target: RawGame) = quickTask("Updating Game '${source.name}'...") {
        message1 = "Updating Game '${source.name}'..."
        doneMessage { "Updated Game: '${source.name}'." }
        repo.replace(source.rawGame, target)
        target.toGame()
    }

    override fun delete(game: Game) = quickTask("Deleting Game '${game.name}'...") {
        message1 = "Deleting Game '${game.name}'..."
        doneMessage { "Deleted Game: '${game.name}'." }
        repo.delete(game.rawGame)
    }

    override fun deleteAll(games: List<Game>) = quickTask("Deleting ${games.size} Games...") {
        message1 = "Deleting ${games.size} Games..."
        doneMessage { "Deleted $processed/$totalWork Games." }

        totalWork = games.size
        this@GameServiceImpl.games.buffered {
            games.chunked(200).forEach { chunk ->
                repo.deleteAll(chunk.map { it.rawGame })
                incProgress(chunk.size)
            }
        }
    }

    override fun deleteAllUserData() = quickTask("Deleting all user data...") {
        message1 = "Deleting all user data..."
        doneMessage { "Deleted all user data." }
        repo.deleteAllUserData()
    }

    override fun invalidate() = quickTask {
        repo.invalidate()
    }

    private fun rebuildGames() = games.setAll(games.map { it.rawGame.toGame() })

    override fun get(id: Int): Game = games.find { it.id == id }
        ?: throw IllegalArgumentException("Game doesn't exist: id=$id")

    private fun RawGame.toGame(): Game = gameFactory.create(this)
}