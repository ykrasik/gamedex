/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.core.filter.FilterEvent
import com.gitlab.ykrasik.gamedex.core.flowOf
import com.gitlab.ykrasik.gamedex.core.library.LibraryEvent
import com.gitlab.ykrasik.gamedex.core.settings.ProviderOrderSettingsRepository
import com.gitlab.ykrasik.gamedex.core.task.Task
import com.gitlab.ykrasik.gamedex.core.task.task
import com.gitlab.ykrasik.gamedex.core.util.broadcastTo
import com.gitlab.ykrasik.gamedex.core.util.flowScope
import com.gitlab.ykrasik.gamedex.core.util.mapObservable
import com.gitlab.ykrasik.gamedex.core.util.toMap
import com.gitlab.ykrasik.gamedex.util.file
import com.gitlab.ykrasik.gamedex.util.logger
import com.gitlab.ykrasik.gamedex.util.time
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.merge
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
    settingsRepo: ProviderOrderSettingsRepository,
) : GameService {
    private val log = logger()

    override val games = log.time("Processing games...") {
        repo.games.mapObservable { it.toGame() }
    }

    private val gamesById = games.toMap(Game::id)

    init {
        games.broadcastTo(eventBus, Game::id, GameEvent::Added, GameEvent::Deleted, GameEvent::Updated)

        flowScope(Dispatchers.IO) {
            eventBus.flowOf<LibraryEvent.Deleted>().forEach(debugName = "onLibraryDeleted") { repo.invalidate() }
        }
        flowScope(Dispatchers.Default) {
            merge(
                eventBus.flowOf<LibraryEvent.Updated>(),
                eventBus.flowOf<FilterEvent>(),
                settingsRepo.changes()
            ).forEach(debugName = "onRebuildGames") {
                rebuildGames()
            }
        }
    }

    override fun add(request: AddGameRequest): Task<Game> {
        val nameBestEffort = request.providerData.firstOrNull()?.gameData?.name ?: request.metadata.path.file.name
        return task("Adding Game '$nameBestEffort'...") {
            repo.add(request).toGame()
        }
    }

    override fun addAll(requests: List<AddGameRequest>) = task("Adding ${requests.size} Games...") {
        successMessage = { "Added $processedItems Games." }

        totalItems.value = requests.size
        repo.games.conflate {
            requests.chunked(50).flatMap { requests ->
                repo.addAll(requests) { incProgress() }.map { it.toGame() }
            }
        }
    }

    override fun replace(source: Game, target: RawGame) = task("Updating Game '${source.name}'...") {
        if (source.rawGame != target) {
            val updatedTarget = target.withMetadata { it.updatedNow() }
            repo.replace(source.rawGame, updatedTarget)
            updatedTarget.toGame()
        } else {
            source
        }
    }

    override fun delete(game: Game) = task("Deleting Game '${game.name}'...") {
        repo.delete(game.rawGame)
    }

    override fun deleteAll(games: List<Game>) = task("Deleting ${games.size} Games...") {
        successMessage = { "Deleted $processedItems Games." }

        totalItems.value = games.size
        repo.games.conflate {
            games.chunked(200).forEach { chunk ->
                repo.deleteAll(chunk.map { it.rawGame })
                incProgress(chunk.size)
            }
        }
    }

    override fun deleteAllUserData() = task("Deleting all user data...") {
        repo.deleteAllUserData()
    }

    override fun buildGame(rawGame: RawGame) = rawGame.toGame()

    private fun rebuildGames() = repo.games.touch()

    override fun get(id: GameId): Game = gamesById.getOrElse(id) {
        throw NoSuchElementException("Game doesn't exist: id=$id")
    }

    private fun RawGame.toGame(): Game = gameFactory.create(this)
}