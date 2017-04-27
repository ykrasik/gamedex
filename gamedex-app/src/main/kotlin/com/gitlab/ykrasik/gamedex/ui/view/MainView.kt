package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.controller.MainController
import com.gitlab.ykrasik.gamedex.core.NotificationManager
import com.gitlab.ykrasik.gamedex.ui.*
import javafx.geometry.Orientation
import javafx.scene.control.TextArea
import tornadofx.*

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 22:44
 */
class MainView : View("Gamedex") {
    private val mainController: MainController by di()
    private val gameController: GameController by di()
    private val notificationManager: NotificationManager by di()

    private val gameView: GameView by inject()
    private val libraryView: LibraryView by inject()

    private var logTextArea: TextArea by singleAssign()

    override val root = borderpane {
        top {
            menubar {
                menu("Game") {
                    isMnemonicParsing = false
                    menuitem("Cleanup") { mainController.cleanup() }
                    separator()
                    menuitem("Re-Fetch Games") { gameController.refetchGames() }
                }
                menu("Settings") {
                    isMnemonicParsing = false
                    menuitem("Settings") { mainController.showSettings() }
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
                }
                logTextArea = readOnlyTextArea { isWrapText = true }
            }
        }
        bottom {
            statusBar {
                textProperty().bind(notificationManager.messageProperty)

                left {
                    togglebutton("Log") {
                        isSelected = true
                        prefWidth = 50.0
                        logTextArea.visibleProperty().bind(selectedProperty())
                    }
                    verticalSeparator(10.0)
                    label { 
                        paddingTop = 4
                        textProperty().bind(gameController.games.sizeProperty().asString("Games: %d"))
                    }
                    verticalSeparator(10.0)
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

    override fun onDock() {
        primaryStage.isMaximized = true
        root.fade(0.5.seconds, 0.0, reversed = true)
    }
}
