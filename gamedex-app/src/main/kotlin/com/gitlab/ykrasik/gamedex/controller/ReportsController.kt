package com.gitlab.ykrasik.gamedex.controller

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.core.*
import com.gitlab.ykrasik.gamedex.repository.GameRepository
import com.gitlab.ykrasik.gamedex.ui.performing
import com.gitlab.ykrasik.gamedex.ui.view.report.ViolationRulesFragment
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
import tornadofx.onChange
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
    private val violationsReportGenerator: ViolationsReportGenerator,
    private val gameDuplicationReportGenerator: GameDuplicationReportGenerator,
    private val nameFolderDiffReportGenerator: NameFolderDiffReportGenerator
) : Controller() {

    private val violationRules = SimpleObjectProperty<ReportRule>(ReportRule.Nop())

    val violations: OngoingReport<RuleResult.Fail> = OngoingReport { games ->
        violationsReportGenerator.generateReport(games, violationRules.value)
    }

    val duplications: OngoingReport<GameDuplication> = OngoingReport { games ->
        gameDuplicationReportGenerator.detectDuplications(games)
    }

    val nameFolderDiffs: OngoingReport<GameNameFolderDiff> = OngoingReport { games ->
        nameFolderDiffReportGenerator.detectGamesWithNameFolderDiff(games)
    }

    init {
        violationRules.onChange { violations.reload() }
    }

    fun editViolationRules() {
        val rules = ViolationRulesFragment(violationRules.value).show() ?: return
        violationRules.value = rules
    }

    inner class OngoingReport<T>(private val calculate: (List<Game>) -> Report<T>) {
        val resultsProperty: Property<Report<T>> = SimpleObjectProperty(emptyMap())
        private var duplicationListener: ListChangeListener<Game>? = null

        val isCalculatingProperty = SimpleBooleanProperty(false)
        private var isCalculating by isCalculatingProperty

        val progressProperty = SimpleDoubleProperty(ProgressIndicator.INDETERMINATE_PROGRESS)

        fun start() {
            if (duplicationListener != null) return

            duplicationListener = gameRepository.games.performing { games ->
                isCalculating = true
                launch(CommonPool) {
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