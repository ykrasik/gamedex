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
import com.gitlab.ykrasik.gamedex.ProviderData
import com.gitlab.ykrasik.gamedex.ProviderHeader
import com.gitlab.ykrasik.gamedex.UserData
import com.gitlab.ykrasik.gamedex.app.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.app.api.util.Task
import com.gitlab.ykrasik.gamedex.app.api.util.TaskType
import com.gitlab.ykrasik.gamedex.core.api.game.GamePresenter
import com.gitlab.ykrasik.gamedex.core.api.game.GameService
import com.gitlab.ykrasik.gamedex.core.api.provider.GameProviderRepository
import com.gitlab.ykrasik.gamedex.core.api.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.core.api.provider.ProviderTaskData
import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfigRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 05/04/2018
 * Time: 10:50
 */
@Singleton
class GamePresenterImpl @Inject constructor(
    private val gameService: GameService,
    private val taskRunner: TaskRunner,
    private val providerRepository: GameProviderRepository,
    private val providerService: GameProviderService,
    userConfigRepository: UserConfigRepository
) : GamePresenter {
    private val gameUserConfig = userConfigRepository[GameUserConfig::class]

    override suspend fun redownloadAllGames() = redownloadGames(gameService.games)

    override suspend fun redownloadGame(game: Game) = runTask("Re-Download '${game.name}'", TaskType.Quick) {
        doneMessageOrCancelled("Done: Re-Downloaded '${game.name}'.")
        downloadGame(game, game.providerHeaders)
    }

    override suspend fun redownloadGames(games: List<Game>) = runTask("Re-Download Games") {
        val masterTask = this
        message1 = "Re-Downloading ${games.size} Games..."
        doneMessage { success -> "${if (success) "Done" else "Cancelled"}: Re-Downloaded $processed / $totalWork Games." }

        runSubTask {
            // Operate on a copy of the games to avoid concurrent modifications.
            games.sortedBy { it.name }.forEachWithProgress(masterTask) { game ->
                val providersToDownload = game.providerHeaders.filter { header ->
                    header.updateDate.plus(gameUserConfig.stalePeriod).isBeforeNow
                }
                if (providersToDownload.isNotEmpty()) {
                    downloadGame(game, providersToDownload)
                }
            }
        }
    }

    private suspend fun Task<*>.downloadGame(game: Game, requestedProviders: List<ProviderHeader>): Game {
        val taskData = ProviderTaskData(this, game.name, game.platform, game.path)
        val providersToDownload = requestedProviders.filter { providerRepository.isEnabled(it.id) }
        val downloadedProviderData = providerService.download(taskData, providersToDownload)
        // Replace existing data with new data, pass-through any data that wasn't replaced.
        val newProviderData = game.rawGame.providerData.filterNot { d -> providersToDownload.any { it.id == d.header.id } } + downloadedProviderData
        return updateGame(game, newProviderData, game.userData)
    }

    private suspend fun Task<*>.updateGame(game: Game, newProviderData: List<ProviderData>, newUserData: UserData?): Game {
        val newRawGame = game.rawGame.copy(providerData = newProviderData, userData = newUserData)
        return runMainTask(gameService.replace(game, newRawGame))
    }

    private suspend fun <T> runTask(title: String, type: TaskType = TaskType.Long, run: suspend Task<*>.() -> T): T {
        require(providerRepository.enabledProviders.isNotEmpty()) { "No providers are enabled! Please make sure there's at least 1 enabled provider in the settings menu." }
        return taskRunner.runTask(title, type, run = run)
    }
}