package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.ui.fragment.SettingsFragment
import javafx.geometry.Pos
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.ToolBar
import javafx.scene.paint.Color
import org.controlsfx.control.PopOver
import org.controlsfx.glyphfont.FontAwesome
import tornadofx.*

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 22:44
 */
class MainView : View("Gamedex") {
    private val gameView: GameView by inject()
    private val sourceView: SourceView by inject()
    private val logView: LogView by inject()
    private val gameDetailsView: GameDetailsView by inject()

    private var tabPane: TabPane by singleAssign()
    private var toolbar: ToolBar by singleAssign()

    private lateinit var previousScreen: Tab

    // TODO: See how this can be achieved using view switching instead of a tabpane
    override val root = borderpane {
        center {
            tabPane = tabpane {
                addClass(Style.navigationTabPane)

                tab(gameView) {
                    userData = gameView
                    graphic = FontAwesome.Glyph.GAMEPAD.toGraphic { color(Color.DARKRED) }
                    previousScreen = this
                }
                tab(sourceView) {
                    userData = sourceView
                    graphic = FontAwesome.Glyph.HDD_ALT.toGraphic { color(Color.DARKGREEN) }
                }
                tab(logView) {
                    userData = logView
                    graphic = FontAwesome.Glyph.BOOK.toGraphic { color(Color.DARKBLUE) }
                }
                tab(gameDetailsView) {
                    userData = gameDetailsView
                }

                selectionModel.select(0)
            }
        }
        top {
            toolbar = toolbar {
                items.onChange {
                    fade(0.6.seconds, 1.0, play = true) {
                        fromValue = 0.0
                    }
                }
            }
        }
    }

    init {
        tabPane.selectionModel.selectedItemProperty().perform { it!!.populateToolbar() }
        tabPane.selectionModel.selectedItemProperty().addListener { _, oldValue, newValue ->
            previousScreen = oldValue
        }
    }

    private fun Tab.populateToolbar() = (userData as GamedexScreen).populateToolbar()

    private fun GamedexScreen.populateToolbar() {
        toolbar.replaceChildren {
            if (useDefaultNavigationButton) {
                buttonWithPopover(
                    graphic = FontAwesome.Glyph.BARS.toGraphic { size(21.0) },
                    arrowLocation = PopOver.ArrowLocation.TOP_LEFT) {

                    tabPane.tabs.forEach { tab ->
                        val gamedexScreen = tab.userData as GamedexScreen
                        if (gamedexScreen.useDefaultNavigationButton) {
                            popoverMenuItem(tab.text, tab.graphic, Style.navigationButton) { tabPane.selectionModel.select(tab) }
                        }
                        gamedexScreen.closeRequestedProperty.onChange {
                            if (it) {
                                gamedexScreen.closeRequestedProperty.value = false
                                selectPreviousScreen()
                            }
                        }
                    }

                    separator()

                    popoverMenuItem("Settings", FontAwesome.Glyph.COG.toGraphic { color(Color.GRAY) }, Style.navigationButton) {
                        SettingsFragment().show()
                    }

                    separator()

                    popoverMenuItem("Quit", FontAwesome.Glyph.SIGN_OUT.toGraphic(), Style.navigationButton) {
                        System.exit(0)
                    }
                }.apply {
                    textProperty().bind(tabPane.selectionModel.selectedItemProperty().map { it!!.text })
                }
            } else {
                backButton { setOnAction { selectPreviousScreen() } }
            }
            verticalSeparator()
            this.constructToolbar()
        }
    }

    private fun selectPreviousScreen() {
        (tabPane.selectionModel.selectedItem.userData as GamedexScreen).onUndock()
        tabPane.selectionModel.select(previousScreen)
    }

    override fun onDock() {
        primaryStage.isMaximized = true
        root.fade(0.5.seconds, 0.0, reversed = true)
    }

    fun showGameDetails(game: Game) {
        gameDetailsView.game = game
        tabPane.selectionModel.select(3)
    }

    companion object {
        class Style : Stylesheet() {
            companion object {
                val navigationTabPane by cssclass()
                val navigationButton by cssclass()

                init {
                    importStylesheet(Style::class)
                }
            }

            init {
                navigationTabPane {
                    tabMaxHeight = 0.px

                    s(".tab-header-area") {
                        visibility = FXVisibility.HIDDEN
                    }
                }

                navigationButton {
                    prefWidth = 100.px
                    alignment = Pos.CENTER_LEFT
                }
            }
        }
    }
}
