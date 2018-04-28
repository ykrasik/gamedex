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
import com.gitlab.ykrasik.gamedex.core.api.file.FileSystemService
import com.gitlab.ykrasik.gamedex.core.api.game.GameService
import com.gitlab.ykrasik.gamedex.core.game.Filter
import com.gitlab.ykrasik.gamedex.core.report.ReportConfig
import com.gitlab.ykrasik.gamedex.core.report.ReportUserConfig
import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfigRepository
import com.gitlab.ykrasik.gamedex.javafx.ThreadAwareDoubleProperty
import com.gitlab.ykrasik.gamedex.javafx.dialog.areYouSureDialog
import com.gitlab.ykrasik.gamedex.util.MultiMap
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.ProgressIndicator
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import tornadofx.Controller
import tornadofx.getValue
import tornadofx.label
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
    private val fileSystemService: FileSystemService,
    userConfigRepository: UserConfigRepository
) : Controller() {
    private val reportUserConfig = userConfigRepository[ReportUserConfig::class]

    private val reportConfigView: ReportConfigView by inject()

    fun addReport() = editReport(ReportConfig(name = "", filter = Filter.`true`, excludedGames = emptyList()))

    fun editReport(config: ReportConfig): ReportConfig? = updateConfig(config) {
        reportConfigView.show(config) ?: return null
    }

    fun excludeGame(config: ReportConfig, game: Game): ReportConfig = updateConfig(config) {
        config.copy(excludedGames = (config.excludedGames + game.id).distinct())
    }

    private inline fun updateConfig(oldConfig: ReportConfig, newConfigFactory: () -> ReportConfig): ReportConfig {
        val newConfig = newConfigFactory()
        if (newConfig.name != oldConfig.name) {
            reportUserConfig.reports -= oldConfig.name
        }
        reportUserConfig.reports += newConfig.name to newConfig
        return newConfig
    }

    fun deleteReport(config: ReportConfig): Boolean {
        val confirm = areYouSureDialog("Delete report '${config.name}'?") { label("Rules: ${config.filter}") }
        if (confirm) {
            reportUserConfig.reports -= config.name
        }
        return confirm
    }

    fun generateReport(config: ReportConfig) = OngoingReport(config)

    inner class OngoingReport(private val config: ReportConfig) {
        val resultsProperty: Property<MultiMap<Game, Filter.AdditionalData>> = SimpleObjectProperty(emptyMap())
        val results by resultsProperty

        private var subscription: SubscriptionReceiveChannel<*>? = null

        val isCalculatingProperty = SimpleBooleanProperty(false)
        private var isCalculating by isCalculatingProperty

        val progressProperty = ThreadAwareDoubleProperty(ProgressIndicator.INDETERMINATE_PROGRESS)

        fun start() {
            if (subscription != null) return

            // TODO: This feels like a task?
            subscription = gameService.games.itemsChannel.subscribe(JavaFx) { games ->
                isCalculating = true
                launch(CommonPool) {
                    val result = calculate(games)
                    withContext(JavaFx) {
                        resultsProperty.value = result
                        isCalculating = false
                    }
                }
            }
        }

        private fun calculate(games: List<Game>): MultiMap<Game, Filter.AdditionalData> {
            val context = Filter.Context(games, fileSystemService)
            val matchingGames = games.filterIndexed { i, game ->
                progressProperty.value = i.toDouble() / (games.size - 1)
                !config.excludedGames.contains(game.id) && config.filter.evaluate(game, context)
            }
            progressProperty.value = ProgressIndicator.INDETERMINATE_PROGRESS
            return matchingGames.map { it to emptyList<Filter.AdditionalData>() }.toMap() + context.additionalData
        }

        fun stop() {
            subscription?.cancel()
            subscription = null
        }
    }
}