package com.gitlab.ykrasik.gamedex.controller

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.core.DuplicationDetector
import com.gitlab.ykrasik.gamedex.core.GameDuplications
import com.gitlab.ykrasik.gamedex.repository.GameRepository
import com.gitlab.ykrasik.gamedex.ui.performing
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ListChangeListener
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
    private val duplicationDetector: DuplicationDetector
) : Controller() {

    val gameDuplications: Property<GameDuplications> = SimpleObjectProperty(emptyMap())
    private var duplicationListener: ListChangeListener<Game>? = null

    val calculatingDuplicationsProperty = SimpleBooleanProperty(false)
    private var calculatingDuplications by calculatingDuplicationsProperty

    fun startDetectingDuplications() {
        if (duplicationListener != null) return

        duplicationListener = gameRepository.games.performing { games ->
            calculatingDuplications = true
            launch(CommonPool) {
                val duplications = duplicationDetector.detectDuplications(games)
                run(JavaFx) { 
                    gameDuplications.value = duplications
                    calculatingDuplications = false
                }
            }
        }
    }

    fun stopDetectingDuplications() {
        if (duplicationListener != null) {
            gameRepository.games.removeListener(duplicationListener)
            duplicationListener = null
        }
    }
}