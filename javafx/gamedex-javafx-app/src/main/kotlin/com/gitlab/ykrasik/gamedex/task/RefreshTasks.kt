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

package com.gitlab.ykrasik.gamedex.task

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.ProviderData
import com.gitlab.ykrasik.gamedex.ProviderHeader
import com.gitlab.ykrasik.gamedex.core.GameProviderService
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.repository.GameRepository
import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.ui.Task
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 04/06/2017
 * Time: 09:43
 */
// TODO: Tasks should contain minimal logic and act as glue between the logic & display
@Singleton
class RefreshTasks @Inject constructor(
    private val gameRepository: GameRepository,
    private val providerService: GameProviderService,
    private val settings: GameSettings,
    private val providerRepository: GameProviderRepository
) {
    // TODO: Consider renaming 'refresh' to 'redownload'
    inner class RefreshGamesTask(private val games: List<Game>) : Task<Unit>("Refreshing ${games.size} games...") {
        private var numRefreshed = 0

        override suspend fun doRun() {
            var remaining = games.size

            // Operate on a copy of the games to avoid concurrent modifications
            games.sortedBy { it.name }.forEachIndexed { i, game ->
                progress.progress(i, games.size - 1)
                titleProperty.set("Refreshing $remaining games...")

                val providersToDownload = game.providerHeaders.filter { header ->
                    header.updateDate.plus(settings.stalePeriod).isBeforeNow
                }
                if (providersToDownload.isNotEmpty()) {
                    doRefreshGame(game, providersToDownload)
                    numRefreshed += 1
                }
                remaining -= 1
            }
        }

        override fun doneMessage() = "Done: Refreshed $numRefreshed games."
    }

    inner class RefreshGameTask(private val game: Game) : Task<Game>("Refreshing '${game.name}'...") {
        override suspend fun doRun() = doRefreshGame(game, game.providerHeaders)
        override fun doneMessage() = "Done refreshing: '${game.name}'."
    }

    private suspend fun Task<*>.doRefreshGame(game: Game, requestedProviders: List<ProviderHeader>): Game {
        val taskData = GameProviderService.ProviderTaskData(this, game.name, game.platform, game.path)
        val providersToDownload = requestedProviders.filter { providerRepository.isEnabled(it.id) }
        val downloadedProviderData = providerService.download(taskData, providersToDownload)
        // Replace existing data with new data, pass-through any data that wasn't replaced.
        val newProviderData = game.rawGame.providerData.filterNot { d -> providersToDownload.any { it.id == d.header.id } } + downloadedProviderData
        return updateGame(game, newProviderData)
    }

    private suspend fun updateGame(game: Game, newProviderData: List<ProviderData>): Game {
        val newRawGame = game.rawGame.copy(providerData = newProviderData)
        return gameRepository.update(newRawGame)
    }
}