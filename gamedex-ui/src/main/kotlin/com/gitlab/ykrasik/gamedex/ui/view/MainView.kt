package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.ui.controller.MainController
import com.gitlab.ykrasik.gamedex.ui.util.*
import javafx.geometry.Orientation
import javafx.scene.control.TextArea
import tornadofx.*

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 22:44
 */
class MainView : View("Main") {
    private val controller: MainController by inject()

    private val gameView: GameView by inject()
    private val libraryView: LibraryView by inject()
    private val excludedPathView: ExcludedPathView by inject()

    private lateinit var logTextArea: TextArea

    override val root = borderpane {

        top {
            menubar {
//                borderpaneConstraints {
//                    alignment = Pos.CENTER
//                }
                menu("Game") {
                    isMnemonicParsing = false
                    menuitem("Cleanup") { controller.cleanup() }
                    separator()
                    menuitem("Re-Fetch Games") { }
                }
                menu("Settings") {
                    isMnemonicParsing = false
                    menuitem("Settings") { controller.showSettings() }
                }
            }
        }

        center {
            splitpane {
                dividerPosition = 0.98
                orientation = Orientation.VERTICAL
                tabpane {
                    nonClosableTab("Games") { content = gameView.root }
                    nonClosableTab("Libraries") { content = libraryView.root }
                    nonClosableTab("Excluded Paths") { content = excludedPathView.root }
                }
                logTextArea = readOnlyTextArea { isWrapText = true }
            }
        }

        bottom {
            statusBar {
                text = "Welcome to GameDex!"

                left {
                    togglebutton("Log") {
                        isSelected = true
                        prefWidth = 50.0
                        logTextArea.visibleProperty().bind(selectedProperty())
                    }
                    verticalSeparator { padding { left = 10.0; right = 10.0 } }
                    label("Games: 0") { padding { top = 4.0 } }
                    verticalSeparator { padding { left = 10.0; right = 10.0 } }
                    label("Libraries: 0") { padding { top = 4.0 } }
                    verticalSeparator { padding { left = 10.0; right = 10.0 } }
                }

                right {
                    progressindicator { isVisible = false }
                    button("Stop") {
                        isCancelButton = true
                        isDisable = true
                        isVisible = false
                    }
                }
            }
        }
    }
}
