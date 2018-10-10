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
import com.gitlab.ykrasik.gamedex.ProviderHeader
import com.gitlab.ykrasik.gamedex.app.api.util.Task
import com.gitlab.ykrasik.gamedex.app.api.util.task
import com.gitlab.ykrasik.gamedex.core.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import com.gitlab.ykrasik.gamedex.util.now
import com.gitlab.ykrasik.gamedex.util.toHumanReadable
import com.google.inject.ImplementedBy
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 29/04/2018
 * Time: 15:54
 */
@ImplementedBy(GameDownloadServiceImpl::class)
interface GameDownloadService {
    fun redownloadGamesCreatedBeforePeriod(): Task<Unit>
    fun redownloadGamesUpdatedAfterPeriod(): Task<Unit>

    fun redownloadGame(game: Game): Task<Game>
}

@Singleton
class GameDownloadServiceImpl @Inject constructor(
    private val gameService: GameService,
    private val gameProviderService: GameProviderService,
    private val settingsService: SettingsService
) : GameDownloadService {
    override fun redownloadGamesCreatedBeforePeriod(): Task<Unit> {
        val target = now.minus(settingsService.game.redownloadCreatedBeforePeriod.toDurationTo(now))
        return redownloadGames("Re-Downloading games created after ${target.toHumanReadable()}...") { it.createDate > target }
    }

    override fun redownloadGamesUpdatedAfterPeriod(): Task<Unit> {
        val target = now.minus(settingsService.game.redownloadUpdatedAfterPeriod)
        return redownloadGames("Re-Downloading games updated before ${target.toHumanReadable()}...") { it.updateDate < target }
    }

    override fun redownloadGame(game: Game) = task("Re-Downloading '${game.name}'...") {
        gameProviderService.checkAtLeastOneProviderEnabled()

        doneMessageOrCancelled("Done: Re-Downloaded '${game.name}'.")
        downloadGame(game, game.providerHeaders)
    }

    private fun redownloadGames(title: String, shouldRedownload: (ProviderHeader) -> Boolean) = task(title) {
        gameProviderService.checkAtLeastOneProviderEnabled()

        val masterTask = this
        message1 = title
        var redownloaded = 0
        doneMessage { success -> "${if (success) "Done" else "Cancelled"}: Re-Downloaded $redownloaded / $totalWork Games." }

        runSubTask {
            // Operate on a copy of the games to avoid concurrent modifications.
            gameService.games.sortedBy { it.name }.forEachWithProgress(masterTask) { game ->
                val providersToDownload = game.providerHeaders.filter(shouldRedownload)
                if (providersToDownload.isNotEmpty()) {
                    downloadGame(game, providersToDownload)
                    redownloaded += 1
                }
            }
        }
    }

    private suspend fun Task<*>.downloadGame(game: Game, requestedProviders: List<ProviderHeader>): Game {
        val providersToDownload = requestedProviders.filter { gameProviderService.isEnabled(it.id) }
        val downloadedProviderData = runMainTask(gameProviderService.download(game.name, game.platform, game.path, providersToDownload))
        // Replace existing data with new data, pass-through any data that wasn't replaced.
        val newProviderData = game.rawGame.providerData.filterNot { d -> providersToDownload.any { it.id == d.header.id } } + downloadedProviderData
        val newRawGame = game.rawGame.copy(providerData = newProviderData)
        return runMainTask(gameService.replace(game, newRawGame))
    }
}