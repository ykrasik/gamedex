package com.gitlab.ykrasik.gamedex.ui.view.game

import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.ui.jfxToggleNode
import com.gitlab.ykrasik.gamedex.ui.popOver
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.searchButton
import com.gitlab.ykrasik.gamedex.ui.toggle
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import org.controlsfx.control.PopOver
import tornadofx.*

/**
 * User: ykrasik
 * Date: 05/06/2017
 * Time: 10:54
 */
class GameSearchMenu : View() {
    private val gameController: GameController by di()
    private val settings: GameSettings by di()

    override val root = searchButton {
        enableWhen { gameController.canRunLongTask }
        val chooseResultsProperty = settings.chooseResultsProperty
        val leftPopover = popOver(PopOver.ArrowLocation.RIGHT_TOP, closeOnClick = false) {
            togglegroup {
                GameSettings.ChooseResults.values().forEach { item ->
                    jfxToggleNode {
                        addClass(CommonStyle.fillAvailableWidth, Style.chooseResultsItem)
                        graphic = label(item.key) { addClass(CommonStyle.fillAvailableWidth, Style.chooseResultsItemText) }
                        isSelected = chooseResultsProperty.value == item
                        selectedProperty().onChange { if (it) chooseResultsProperty.value = item }
                    }
                }
            }
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

    class Style : Stylesheet() {
        companion object {
            val chooseResultsItem by cssclass()
            val chooseResultsItemText by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            chooseResultsItem {
                backgroundColor = multi(Color.TRANSPARENT)
                and(hover) {
                    backgroundColor = multi(Color.LIGHTBLUE)
                }
            }

            chooseResultsItemText {
                padding = box(vertical = 0.px, horizontal = 5.px)
            }
        }
    }
}