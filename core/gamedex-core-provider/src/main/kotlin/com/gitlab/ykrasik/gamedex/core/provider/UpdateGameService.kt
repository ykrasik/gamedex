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

package com.gitlab.ykrasik.gamedex.core.provider

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.ProviderData
import com.gitlab.ykrasik.gamedex.ProviderHeader
import com.gitlab.ykrasik.gamedex.RawGame
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.app.api.provider.UpdateGameProgressData
import com.gitlab.ykrasik.gamedex.core.filter.FilterService
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.task.Task
import com.gitlab.ykrasik.gamedex.core.task.task
import com.gitlab.ykrasik.gamedex.util.SingleValueStorage
import com.gitlab.ykrasik.gamedex.util.logger
import com.google.inject.BindingAnnotation
import com.google.inject.ImplementedBy
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * User: ykrasik
 * Date: 29/04/2018
 * Time: 15:54
 */
@ImplementedBy(UpdateGameServiceImpl::class)
interface UpdateGameService {
    val inProgressUpdate: UpdateGameProgressData?

    fun updateGame(game: Game): Task<Unit>
    fun bulkUpdateGames(filter: Filter): Task<Unit>
    fun bulkUpdateGames(updateData: UpdateGameProgressData): Task<Unit>
}

@Singleton
class UpdateGameServiceImpl @Inject constructor(
    private val gameService: GameService,
    private val gameProviderService: GameProviderService,
    private val filterService: FilterService,
    @UpdateGameServiceStorage private val storage: SingleValueStorage<UpdateGameProgressData>,
) : UpdateGameService {
    private val log = logger()

    override val inProgressUpdate get() = storage.get()

    override fun updateGame(game: Game) = updateGames(sequenceOf(game), filter = null)

    override fun bulkUpdateGames(filter: Filter): Task<Unit> {
        val games = filterService.filter(gameService.games, filter)
        return updateGames(games, filter)
    }

    override fun bulkUpdateGames(updateData: UpdateGameProgressData): Task<Unit> {
        return updateGames(updateData.remainingGames.asSequence().mapNotNull {
            runCatching {
                gameService[it]
            }.getOrNull()
        }, updateData.filter)
    }

    private fun updateGames(gamesWithoutProviders: Sequence<Game>, filter: Filter?): Task<Unit> {
        val games: List<Pair<Game, List<ProviderHeader>>> = gamesWithoutProviders.mapNotNullTo(mutableListOf()) { game ->
            val providersToUpdate = game.providerHeaders.filter { gameProviderService.isEnabled(it.providerId) }.toList()
            if (providersToUpdate.isNotEmpty()) {
                game to providersToUpdate
            } else {
                null
            }
        }
        val isSingleUpdate = games.size == 1

        return task(
            title = "Updating ${if (isSingleUpdate) "'${games.first().first.name}'..." else "${games.size} games..."}",
            isCancellable = !isSingleUpdate
        ) {
            gameProviderService.assertHasEnabledProvider()

            successOrCancelledMessage { success ->
                "${if (success) "Done" else "Cancelled"}${if (games.size > 1) ": Updated $processedItems Games." else "."}"
            }

            val remainingGames = games.mapTo(mutableSetOf()) { it.first.id }
            if (!isSingleUpdate) {
                storage.set(UpdateGameProgressData(filter!!, remainingGames))
            }

            var shouldResetStorage = !isSingleUpdate
            val context = coroutineContext
            val sortedBy = games.sortedBy { it.first.name }
            sortedBy.forEachWithProgress { (game, providersToDownload) ->
                if (!context.isActive) {
                    // Do not reset storage on task cancellation.
                    shouldResetStorage = false
                    return@task
                }

                try {
                    val downloadedProviderData = executeSubTask(gameProviderService.fetch(game.name, game.platform, providersToDownload))

                    // Replace existing data with new data, pass-through any data that wasn't replaced.
                    val newRawGame = game.rawGame.withProviderData(downloadedProviderData)
                    executeSubTask(gameService.replace(game, newRawGame))
                    remainingGames -= game.id
                    if (!isSingleUpdate) {
                        storage.set(UpdateGameProgressData(filter!!, remainingGames))
                    }
                } catch (e: Exception) {
                    shouldResetStorage = false
                    if (!isSingleUpdate) {
                        log.error("Error updating $game", e)
                    } else {
                        throw e
                    }
                }
            }

            if (shouldResetStorage) {
                storage.reset()
            }
        }
    }

    private fun RawGame.withProviderData(providerData: List<ProviderData>): RawGame {
        val providerDataById = (this.providerData + providerData).groupBy { it.providerId }
        val mergedProviderData = providerDataById.mapValues { (_, datas) ->
            val updatedData = datas.last()
            if (datas.size > 1) {
                val initialData = datas.first()
                if (initialData != updatedData.copy(timestamp = initialData.timestamp)) {
                    updatedData.copy(timestamp = initialData.timestamp.updatedNow())
                } else {
                    initialData
                }
            } else {
                updatedData
            }
        }
        val updatedProviderData = mergedProviderData.map { (_, v) -> v }
        return copy(providerData = updatedProviderData)
    }
}

fun GameProviderService.assertHasEnabledProvider() = check(enabledProviders.isNotEmpty()) {
    "No providers are enabled! Please make sure there's at least 1 enabled provider in the settings menu."
}

@BindingAnnotation
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class UpdateGameServiceStorage