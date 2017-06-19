package com.gitlab.ykrasik.gamedex.controller

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.core.ReportConfig
import com.gitlab.ykrasik.gamedex.core.ReportRule
import com.gitlab.ykrasik.gamedex.core.RuleResult
import com.gitlab.ykrasik.gamedex.repository.GameRepository
import com.gitlab.ykrasik.gamedex.settings.ReportSettings
import com.gitlab.ykrasik.gamedex.ui.ThreadAwareDoubleProperty
import com.gitlab.ykrasik.gamedex.ui.performing
import com.gitlab.ykrasik.gamedex.ui.view.dialog.areYouSureDialog
import com.gitlab.ykrasik.gamedex.ui.view.report.ReportConfigFragment
import com.gitlab.ykrasik.gamedex.util.logger
import com.gitlab.ykrasik.gamedex.util.toMultiMap
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ListChangeListener
import javafx.scene.control.ProgressIndicator
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.run
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
@Singleton
class ReportsController @Inject constructor(
    private val gameRepository: GameRepository,
    private val settings: ReportSettings
) : Controller() {
    private val logger = logger()

    fun addReport() = editReport(ReportConfig())

    fun editReport(config: ReportConfig): ReportConfig? {
        val newConfig = ReportConfigFragment(config).show() ?: return null
        if (newConfig.name != config.name) {
            settings.reports -= config.name
        }
        settings.reports += newConfig.name to newConfig
        return newConfig
    }

    fun deleteReport(config: ReportConfig): Boolean {
        // TODO: Display config content.
        return areYouSureDialog("Delete report '${config.name}'?").apply {
            if (this) {
                settings.reports -= config.name
            }
        }
    }

    fun generateReport(config: ReportConfig) = OngoingReport(config)

    inner class OngoingReport(private val config: ReportConfig) {
        val resultsProperty: Property<Map<Game, List<RuleResult.Fail>>> = SimpleObjectProperty(emptyMap())
        val results by resultsProperty

        private var reportListener: ListChangeListener<Game>? = null

        val isCalculatingProperty = SimpleBooleanProperty(false)
        private var isCalculating by isCalculatingProperty

        val progressProperty = ThreadAwareDoubleProperty(ProgressIndicator.INDETERMINATE_PROGRESS)

        fun start() {
            if (reportListener != null) return

            reportListener = gameRepository.games.performing { games ->
                isCalculating = true
                logger.info("Calculating report '${config.name}'...")
                launch(CommonPool) {
                    val result = calculate(games)
                    run(JavaFx) {
                        resultsProperty.value = result
                        logger.info("Calculating report '${config.name}': ${result.size} results.")
                        isCalculating = false
                    }
                }
            }
        }

        private fun calculate(games: List<Game>): Map<Game, List<RuleResult.Fail>> {
            // Detect candidates
            var context = ReportRule.ReportContext(games)
            val candidates = games.filter { game -> config.filters.check(game, context) == RuleResult.Pass }

            // Evaluate rules
            context = context.copy(games = candidates)
            val violations = candidates.mapIndexedNotNull { i, game ->
                progressProperty.value = i.toDouble() / (candidates.size - 1)
                val violation = config.rules.check(game, context)
                if (violation is RuleResult.Fail) game to violation else null
            }
            progressProperty.value = ProgressIndicator.INDETERMINATE_PROGRESS
            return violations.toMultiMap()
        }

        fun stop() {
            if (reportListener != null) {
                gameRepository.games.removeListener(reportListener)
                reportListener = null
            }
        }

        fun reload() {
            stop()
            start()
        }
    }
}