package com.gitlab.ykrasik.gamedex.controller

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.core.FileSystemOps
import com.gitlab.ykrasik.gamedex.core.ReportConfig
import com.gitlab.ykrasik.gamedex.core.ReportRule
import com.gitlab.ykrasik.gamedex.repository.GameRepository
import com.gitlab.ykrasik.gamedex.settings.ReportSettings
import com.gitlab.ykrasik.gamedex.ui.ThreadAwareDoubleProperty
import com.gitlab.ykrasik.gamedex.ui.performing
import com.gitlab.ykrasik.gamedex.ui.view.dialog.areYouSureDialog
import com.gitlab.ykrasik.gamedex.ui.view.report.ReportConfigFragment
import com.gitlab.ykrasik.gamedex.util.MultiMap
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ListChangeListener
import javafx.scene.control.ProgressIndicator
import kotlinx.coroutines.experimental.CommonPool
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
@Singleton
class ReportsController @Inject constructor(
    private val gameRepository: GameRepository,
    private val settings: ReportSettings,
    private val fileSystemOps: FileSystemOps
) : Controller() {
    
    fun addReport() = editReport(ReportConfig())

    fun editReport(config: ReportConfig): ReportConfig? =
        updateConfig(config) { ReportConfigFragment(config).show() ?: return null }

    fun excludeGame(config: ReportConfig, game: Game): ReportConfig =
        updateConfig(config) { config.copy(excludedGames = (config.excludedGames + game.id).distinct()) }

    private inline fun updateConfig(oldConfig: ReportConfig, newConfigFactory: () -> ReportConfig): ReportConfig {
        val newConfig = newConfigFactory()
        if (newConfig.name != oldConfig.name) {
            settings.reports -= oldConfig.name
        }
        settings.reports += newConfig.name to newConfig
        return newConfig
    }

    fun deleteReport(config: ReportConfig): Boolean {
        return areYouSureDialog("Delete report '${config.name}'?") {
            label("Rules: ${config.rules}")
        }.apply {
            if (this) {
                settings.reports -= config.name
            }
        }
    }

    fun generateReport(config: ReportConfig) = OngoingReport(config)

    inner class OngoingReport(private val config: ReportConfig) {
        val resultsProperty: Property<MultiMap<Game, ReportRule.ReportInfo>> = SimpleObjectProperty(emptyMap())
        val results by resultsProperty

        private var reportListener: ListChangeListener<Game>? = null

        val isCalculatingProperty = SimpleBooleanProperty(false)
        private var isCalculating by isCalculatingProperty

        val progressProperty = ThreadAwareDoubleProperty(ProgressIndicator.INDETERMINATE_PROGRESS)

        fun start() {
            if (reportListener != null) return

            reportListener = gameRepository.games.performing { games ->
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

        private fun calculate(games: List<Game>): MultiMap<Game, ReportRule.ReportInfo> {
            val context = ReportRule.Context(games, fileSystemOps)
            val matchingGames = games.filterIndexed { i, game ->
                progressProperty.value = i.toDouble() / (games.size - 1)
                !config.excludedGames.contains(game.id) && config.rules.evaluate(game, context)
            }
            progressProperty.value = ProgressIndicator.INDETERMINATE_PROGRESS
            return matchingGames.map { it to emptyList<ReportRule.ReportInfo>() }.toMap() + context.additionalInfo
        }

        fun stop() {
            if (reportListener != null) {
                gameRepository.games.removeListener(reportListener)
                reportListener = null
            }
        }
    }
}