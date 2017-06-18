package com.gitlab.ykrasik.gamedex.controller

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.core.*
import com.gitlab.ykrasik.gamedex.repository.GameRepository
import com.gitlab.ykrasik.gamedex.settings.ReportSettings
import com.gitlab.ykrasik.gamedex.ui.performing
import com.gitlab.ykrasik.gamedex.ui.view.dialog.areYouSureDialog
import com.gitlab.ykrasik.gamedex.ui.view.report.ReportConfigFragment
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
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
// TODO: This class doesn't know what it does.
@Singleton
class ReportsController @Inject constructor(
    private val gameRepository: GameRepository,
    private val settings: ReportSettings,
    private val violationsReportGenerator: ViolationsReportGenerator,
    private val gameDuplicationReportGenerator: GameDuplicationReportGenerator,
    private val nameFolderDiffReportGenerator: NameFolderDiffReportGenerator
) : Controller() {

//    val violations: OngoingReport = OngoingReport { games ->
//        violationsReportGenerator.generateReport(games, settings.config)
//    }

//    val duplications: OngoingReport<GameDuplication> = OngoingReport { games ->
//        gameDuplicationReportGenerator.detectDuplications(games)
//    }
//
//    val nameFolderDiffs: OngoingReport<GameNameFolderDiff> = OngoingReport { games ->
//        nameFolderDiffReportGenerator.detectGamesWithNameFolderDiff(games)
//    }

    init {
//        settings.reportsProperty.onChange { violations.reload() }
    }

    fun addReport() = editReport(ReportConfig())

    fun editReport(config: ReportConfig): ReportConfig? {
        return ReportConfigFragment(config).show()
//        settings.reports -= config.name
    }

    fun delete(config: ReportConfig): Boolean {
        // TODO: Display report content.
        return areYouSureDialog("Delete report '${config.name}'?")
    }

    fun generateReport(config: ReportConfig) = OngoingReport(config) { games ->
        violationsReportGenerator.generateReport(games, config)
    }
//
//    fun generateReport(config: ReportConfig): OngoingReport {
//        // Detect candidates
//        var context = ReportRule.ReportContext(games)
//        val candidates = games.filter { game -> config.filters.check(game, context) == RuleResult.Pass }
//
//        // Evaluate rules
//        context = context.copy(games = candidates)
//        val violations = candidates.mapNotNull { game ->
//            val violation = config.rules.check(game, context)
//            if (violation is RuleResult.Fail) game to violation else null
//        }
//        return violations.toMultiMap()
//    }

    inner class OngoingReport(private val config: ReportConfig, private val calculate: (List<Game>) -> Report<RuleResult.Fail>) {
        val resultsProperty: Property<Report<RuleResult.Fail>> = SimpleObjectProperty(emptyMap())
        private var duplicationListener: ListChangeListener<Game>? = null

        val isCalculatingProperty = SimpleBooleanProperty(false)
        private var isCalculating by isCalculatingProperty

        val progressProperty = SimpleDoubleProperty(ProgressIndicator.INDETERMINATE_PROGRESS)

        fun start() {
            if (duplicationListener != null) {
                println("${this@OngoingReport} already started.")
                return
            }

            duplicationListener = gameRepository.games.performing { games ->
                isCalculating = true
                launch(CommonPool) {
                    println("${this@OngoingReport} Calculating: $config")
                    val result = calculate(games)
                    run(JavaFx) {
                        resultsProperty.value = result
                        isCalculating = false
                    }
                }
            }
        }

        fun stop() {
            if (duplicationListener != null) {
                println("${this@OngoingReport} Stopping: $config")
                gameRepository.games.removeListener(duplicationListener)
                duplicationListener = null
            }
        }

        fun reload() {
            stop()
            start()
        }
    }
}