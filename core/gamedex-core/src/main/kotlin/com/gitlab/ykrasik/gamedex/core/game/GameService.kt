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

import com.gitlab.ykrasik.gamdex.core.api.game.AddGameRequest
import com.gitlab.ykrasik.gamdex.core.api.game.GameService
import com.gitlab.ykrasik.gamdex.core.api.task.Progress
import com.gitlab.ykrasik.gamdex.core.api.task.Task
import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.RawGame
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 01/03/2018
 * Time: 22:12
 */
@Singleton
class GameServiceImpl @Inject constructor(private val gameRepository: GameRepository) : GameService {
    override val games = gameRepository.games

    override fun get(id: Int) = gameRepository[id]

    override fun add(request: AddGameRequest) = Task("Add $request") {
        gameRepository.add(request)
    }

    override fun addAll(requests: List<AddGameRequest>, progress: Progress) = Task("Add ${requests.size} Games") {
        gameRepository.addAll(requests, progress)
    }

    override suspend fun replace(source: Game, target: RawGame) = gameRepository.replace(source, target)

    override fun delete(game: Game) = Task("Delete $game") { progress ->
        gameRepository.delete(game)

        progress.doneMessage = "Deleted Game '${game.name}'"
    }

    override fun deleteAll(games: List<Game>) = Task("Delete ${games.size} Games") {
        gameRepository.deleteAll(games)
    }

    // TODO: Again, separate tasks with progress that are displayed on the ui from simple coroutines.
    override fun deleteAllUserData() = Task("Delete all user data") { progress ->
        // TODO: Don't send messages here, do it in the controller / presenter
        progress.message("Deleting all game user data...")
        gameRepository.clearUserData()
        progress.doneMessage = "All game user data deleted."
    }

    override fun invalidate() = Task {
        gameRepository.invalidate()
    }
}