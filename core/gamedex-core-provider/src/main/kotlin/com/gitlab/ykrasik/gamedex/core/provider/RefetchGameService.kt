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
import com.gitlab.ykrasik.gamedex.core.filter.FilterContextFactory
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
@ImplementedBy(RefetchGameServiceImpl::class)
interface RefetchGameService {
    fun refetchGame(game: Game): Task<Unit>
    fun refetchGames(filter: Filter): Task<Unit>
}

@Singleton
class RefetchGameServiceImpl @Inject constructor(
    private val gameService: GameService,
    private val gameProviderService: GameProviderService,
    private val filterContextFactory: FilterContextFactory
) : RefetchGameService {
    override fun refetchGame(game: Game) = refetchGames(listOf(game))

    override fun refetchGames(filter: Filter): Task<Unit> {
        val context = filterContextFactory.create(emptyList())
        val games = gameService.games.filter { filter.evaluate(it, context) }.sortedBy { it.name }
        return refetchGames(games)
    }

    private fun refetchGames(games: List<Game>) = task("Re-Fetching ${if (games.size == 1) "'${games.first().name}'..." else "${games.size} games..."}", isCancellable = true) {
        gameProviderService.checkAtLeastOneProviderEnabled()

        successOrCancelledMessage { success ->
            "${if (success) "Done" else "Cancelled"}: Re-Fetched ${if (games.size == 1) "'${games.firstOrNull()?.name}'." else "$processedItems / $totalItems Games."}"
        }

        games.forEachWithProgress { game ->
            val providersToDownload = game.providerHeaders.filter { gameProviderService.isEnabled(it.id) }
            if (providersToDownload.isEmpty()) return@forEachWithProgress

            val downloadedProviderData = executeSubTask(gameProviderService.fetch(game.name, game.platform, providersToDownload))

            // Replace existing data with new data, pass-through any data that wasn't replaced.
            val newRawGame = game.rawGame.withProviderData(downloadedProviderData)
            executeSubTask(gameService.replace(game, newRawGame))
        }
    }

    private fun RawGame.withProviderData(providerData: List<ProviderData>): RawGame = copy(
        providerData = this.providerData.filterNot { d -> providerData.any { it.header.id == d.header.id } } +
            providerData.map { it.updatedNow() }
    )
}