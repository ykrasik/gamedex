package com.gitlab.ykrasik.gamedex.ui.view.game.menu

import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.ui.popOver
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.searchButton
import com.gitlab.ykrasik.gamedex.ui.toggle
import javafx.scene.input.MouseEvent
import org.controlsfx.control.PopOver
import tornadofx.*

/**
 * User: ykrasik
 * Date: 05/06/2017
 * Time: 10:54
 */
class GameSearchMenu : View() {
    private val gameController: GameController by di()

    override val root = searchButton {
        enableWhen { gameController.canRunLongTask }
        val leftPopover = popOver(PopOver.ArrowLocation.RIGHT_TOP, closeOnClick = false) {
            ChooseSearchResultsToggleMenu().install(this)
        }
        val downPopover = popOver {
            addEventFilter(MouseEvent.MOUSE_CLICKED) { leftPopover.hide() }
            searchButton("New Games") {
                addClass(CommonStyle.fillAvailableWidth)
                tooltip("Search all libraries for new games")
                setOnAction { gameController.scanNewGames() }
            }
            separator()
            searchButton("All Games Without All Providers") {
                addClass(CommonStyle.fillAvailableWidth)
                tooltip("Search all games that don't already have all available providers")
                setOnAction { gameController.rediscoverAllGamesWithoutAllProviders() }
            }
            separator()
            searchButton("Filtered Games Without All Providers") {
                addClass(CommonStyle.fillAvailableWidth)
                tooltip("Search currently filtered games that don't already have all available providers")
                setOnAction { gameController.rediscoverFilteredGamesWithoutAllProviders() }
            }
        }
        setOnAction { downPopover.toggle(this); leftPopover.toggle(this) }
    }
}