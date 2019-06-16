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

package com.gitlab.ykrasik.gamedex.core.provider

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.ProviderData
import com.gitlab.ykrasik.gamedex.RawGame
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.core.filter.FilterService
import com.gitlab.ykrasik.gamedex.core.game.GameService
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
@ImplementedBy(UpdateGameServiceImpl::class)
interface UpdateGameService {
    fun updateGame(game: Game): Task<Unit>
    fun bulkUpdateGames(filter: Filter): Task<Unit>
}

@Singleton
class UpdateGameServiceImpl @Inject constructor(
    private val gameService: GameService,
    private val gameProviderService: GameProviderService,
    private val filterService: FilterService
) : UpdateGameService {
    override fun updateGame(game: Game) = updateGames(listOf(game))

    override fun bulkUpdateGames(filter: Filter): Task<Unit> {
        val games = filterService.filter(gameService.games, filter).sortedBy { it.name }
        return updateGames(games)
    }

    private fun updateGames(games: List<Game>) = task("Updating ${if (games.size == 1) "'${games.first().name}'..." else "${games.size} games..."}", isCancellable = true) {
        gameProviderService.assertHasEnabledProvider()

        successOrCancelledMessage { success ->
            "${if (success) "Done" else "Cancelled"}: Updated ${if (games.size == 1) "'${games.firstOrNull()?.name}'." else "$processedItems / $totalItems Games."}"
        }

        games.forEachWithProgress { game ->
            val providersToDownload = game.providerHeaders.filter { gameProviderService.isEnabled(it.providerId) }.toList()
            if (providersToDownload.isEmpty()) return@forEachWithProgress

            val downloadedProviderData = executeSubTask(gameProviderService.fetch(game.name, game.platform, providersToDownload))

            // Replace existing data with new data, pass-through any data that wasn't replaced.
            val newRawGame = game.rawGame.withProviderData(downloadedProviderData)
            executeSubTask(gameService.replace(game, newRawGame))
        }
    }

    private fun RawGame.withProviderData(providerData: List<ProviderData>): RawGame = copy(
        providerData = this.providerData.filterNot { d -> providerData.any { it.header.providerId == d.header.providerId } } +
            providerData.map { it.updatedNow() }
    )
}