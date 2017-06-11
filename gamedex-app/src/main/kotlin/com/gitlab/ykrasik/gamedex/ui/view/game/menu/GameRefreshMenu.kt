package com.gitlab.ykrasik.gamedex.ui.view.game.menu

import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.ui.popOver
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.refreshButton
import com.gitlab.ykrasik.gamedex.ui.toggle
import javafx.scene.input.MouseEvent
import org.controlsfx.control.PopOver
import tornadofx.*

/**
 * User: ykrasik
 * Date: 05/06/2017
 * Time: 10:57
 */
class GameRefreshMenu : View() {
    private val gameController: GameController by di()

    private val staleDurationMenu: GameStaleDurationMenu by inject()

    override val root = refreshButton {
        enableWhen { gameController.canRunLongTask }

        val leftPopover = popOver(PopOver.ArrowLocation.RIGHT_TOP, closeOnClick = false) {
            children += staleDurationMenu.root
        }
        val downPopover = popOver {
            addEventFilter(MouseEvent.MOUSE_CLICKED) { leftPopover.hide() }
            disableWhen { staleDurationMenu.isFocused.or(staleDurationMenu.isValid.not()) }
            refreshButton("All Stale Games") {
                addClass(CommonStyle.fillAvailableWidth)
                tooltip("Refresh all games that were last refreshed before the stale duration")
                setOnAction { gameController.refreshAllGames() }
            }
            separator()
            refreshButton("Filtered Stale Games") {
                addClass(CommonStyle.fillAvailableWidth)
                tooltip("Refresh filtered games that were last refreshed before the stale duration")
                setOnAction { setOnAction { gameController.refreshFilteredGames() } }
            }
        }
        setOnAction { downPopover.toggle(this); leftPopover.toggle(this) }
    }
}