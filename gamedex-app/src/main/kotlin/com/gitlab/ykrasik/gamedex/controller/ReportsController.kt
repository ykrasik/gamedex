package com.gitlab.ykrasik.gamedex.controller

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.core.DuplicationDetector
import com.gitlab.ykrasik.gamedex.core.GameDuplications
import com.gitlab.ykrasik.gamedex.core.GameNameFolderMismatches
import com.gitlab.ykrasik.gamedex.core.NameFolderMismatchDetector
import com.gitlab.ykrasik.gamedex.repository.GameRepository
import com.gitlab.ykrasik.gamedex.ui.performing
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
@Singleton
class ReportsController @Inject constructor(
    private val gameRepository: GameRepository,
    private val duplicationDetector: DuplicationDetector,
    private val nameFolderMismatchDetector: NameFolderMismatchDetector
) : Controller() {

    val duplications: OngoingReport<GameDuplications> = OngoingReport(emptyMap()) { games ->
        duplicationDetector.detectDuplications(games)
    }

    val nameFolderMismatches: OngoingReport<GameNameFolderMismatches> = OngoingReport(emptyMap()) { games ->
        nameFolderMismatchDetector.detectGamesWithNameFolderMismatch(games)
    }

    inner class OngoingReport<T>(emptyValue: T, private val calculate: (List<Game>) -> T) {
        val resultsProperty: Property<T> = SimpleObjectProperty(emptyValue)
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
    }
}