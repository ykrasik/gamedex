package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.ui.jfxToggleNode
import com.gitlab.ykrasik.gamedex.ui.popOver
import com.gitlab.ykrasik.gamedex.ui.theme.searchButton
import com.gitlab.ykrasik.gamedex.ui.toggle
import javafx.geometry.Pos
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
                        addClass(Style.chooseResultsItem)
                        graphic = label(item.key) { addClass(Style.chooseResultsItemText) }
                        isSelected = chooseResultsProperty.value == item
                        selectedProperty().onChange { if (it) chooseResultsProperty.value = item }
                    }
                }
            }
        }
        val downPopover = popOver {
            addEventFilter(MouseEvent.MOUSE_CLICKED) { leftPopover.hide() }
            searchButton("New Games") {
                addClass(Style.searchButton)
                tooltip("Search all libraries for new games")
                setOnAction { gameController.scanNewGames() }
            }
            separator()
            searchButton("Games without Providers") {
                addClass(Style.searchButton)
                tooltip("Search all games that don't already have all available providers")
                setOnAction { gameController.rediscoverGamesWithoutProviders() }
            }
            separator()
            searchButton("Filtered Games") {
                addClass(Style.searchButton)
                tooltip("Search currently filtered games that don't already have all available providers")
                setOnAction { gameController.rediscoverFilteredGames() }
            }
        }
        setOnAction { downPopover.toggle(this); leftPopover.toggle(this) }
    }

    class Style : Stylesheet() {
        companion object {
            val searchButton by cssclass()
            val chooseResultsItem by cssclass()
            val chooseResultsItemText by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            searchButton {
                maxWidth = Double.MAX_VALUE.px
                alignment = Pos.CENTER_LEFT
            }

            chooseResultsItem {
                maxWidth = Double.MAX_VALUE.px
                backgroundColor = multi(Color.TRANSPARENT)
                and(hover) {
                    backgroundColor = multi(Color.LIGHTBLUE)
                }
            }

            chooseResultsItemText {
                maxWidth = Double.MAX_VALUE.px
                alignment = Pos.CENTER_LEFT
                padding = box(vertical = 0.px, horizontal = 5.px)
            }
        }
    }
}