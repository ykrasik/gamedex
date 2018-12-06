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
import com.gitlab.ykrasik.gamedex.core.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.core.task.Task
import com.gitlab.ykrasik.gamedex.core.task.task
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
    fun redownloadGame(game: Game): Task<Unit> = redownloadGames(listOf(game))
    fun redownloadGames(games: List<Game>): Task<Unit>
}

@Singleton
class GameDownloadServiceImpl @Inject constructor(
    private val gameService: GameService,
    private val gameProviderService: GameProviderService
) : GameDownloadService {
    override fun redownloadGames(games: List<Game>) = task("Re-Downloading " + if (games.size == 1) "'${games.first().name}'..." else "${games.size} games...", isCancellable = true) {
        gameProviderService.checkAtLeastOneProviderEnabled()

        successOrCancelledMessage { success ->
            "${if (success) "Done" else "Cancelled"}: Re-Downloaded " + if (games.size == 1) "'${games.firstOrNull()?.name}'." else "$processedItems / $totalItems Games."
        }

        games.forEachWithProgress { game ->
            val providersToDownload = game.providerHeaders.filter { gameProviderService.isEnabled(it.id) }
            if (providersToDownload.isEmpty()) return@forEachWithProgress

            val downloadedProviderData = executeSubTask(gameProviderService.download(game.name, game.platform, providersToDownload))

            // Replace existing data with new data, pass-through any data that wasn't replaced.
            val newProviderData = game.rawGame.providerData.filterNot { d ->
                providersToDownload.any { it.id == d.header.id }
            } + downloadedProviderData
            val newRawGame = game.rawGame.copy(providerData = newProviderData)
            executeSubTask(gameService.replace(game, newRawGame))
        }
    }
}