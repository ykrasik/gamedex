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

package com.gitlab.ykrasik.gamedex.javafx.report

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameId
import com.gitlab.ykrasik.gamedex.app.api.report.Report
import com.gitlab.ykrasik.gamedex.core.api.file.FileSystemService
import com.gitlab.ykrasik.gamedex.core.api.game.GameService
import com.gitlab.ykrasik.gamedex.core.filter.FilterContextImpl
import com.gitlab.ykrasik.gamedex.javafx.ThreadAwareDoubleProperty
import com.gitlab.ykrasik.gamedex.util.MultiMap
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.ProgressIndicator
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import tornadofx.Controller
import tornadofx.getValue
import tornadofx.setValue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 16:53
 */
// TODO: Move to tornadoFx di() and have the presenter as a dependency.
@Singleton
class ReportController @Inject constructor(
    private val gameService: GameService,
    private val fileSystemService: FileSystemService
) : Controller() {
    fun generateReport(config: Report) = OngoingReport(config)

    inner class OngoingReport(private val report: Report) {
        val resultsProperty: Property<MultiMap<Game, FilterContextImpl.AdditionalData>> = SimpleObjectProperty(emptyMap())
        val results by resultsProperty

        private var subscription: ReceiveChannel<*>? = null

        val isCalculatingProperty = SimpleBooleanProperty(false)
        private var isCalculating by isCalculatingProperty

        val progressProperty = ThreadAwareDoubleProperty(ProgressIndicator.INDETERMINATE_PROGRESS)

        fun start() {
            if (subscription != null) return

            // TODO: This feels like a task?
            subscription = gameService.games.itemsChannel.subscribe(Dispatchers.JavaFx) { games ->
                isCalculating = true
                GlobalScope.launch(Dispatchers.Default) {
                    val result = calculate(games).map { (id, additionalData) -> gameService[id] to additionalData.toList() }.toMap()
                    withContext(Dispatchers.JavaFx) {
                        resultsProperty.value = result
                        isCalculating = false
                    }
                }
            }
        }

        private suspend fun calculate(games: List<Game>): Map<GameId, Set<FilterContextImpl.AdditionalData>> {
            val context = FilterContextImpl(games, fileSystemService)
            val matchingGames = games.filterIndexed { i, game ->
                progressProperty.value = i.toDouble() / (games.size - 1)
                !report.excludedGames.contains(game.id) && report.filter.evaluate(game, context)
            }
            progressProperty.value = ProgressIndicator.INDETERMINATE_PROGRESS
            return matchingGames.map { it.id to emptySet<FilterContextImpl.AdditionalData>() }.toMap() + context.additionalData
        }

        fun stop() {
            subscription?.cancel()
            subscription = null
        }
    }
}