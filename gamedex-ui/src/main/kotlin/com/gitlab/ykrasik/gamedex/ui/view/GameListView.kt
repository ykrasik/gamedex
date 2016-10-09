package com.gitlab.ykrasik.gamedex.ui.view

import com.github.ykrasik.gamedex.datamodel.Game
import com.gitlab.ykrasik.gamedex.ui.controller.GameController
import com.gitlab.ykrasik.gamedex.ui.util.dividerPosition
import com.gitlab.ykrasik.gamedex.ui.util.padding
import com.gitlab.ykrasik.gamedex.ui.util.readOnlyTextArea
import com.gitlab.ykrasik.gamedex.ui.util.readOnlyTextField
import javafx.geometry.HPos
import javafx.geometry.Pos
import javafx.geometry.VPos
import javafx.scene.layout.Priority
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 15:06
 */
class GameListView : View("Game List") {
    private val controller: GameController by inject()

    override val root = borderpane {
        center {
            splitpane {
                dividerPosition = 0.69
//                borderpaneConstraints {
//                    alignment = Pos.CENTER
//                }
                tableview<Game> {
                    isEditable = false
                    column("Name", Game::name) {
                        minWidth = 260.0
                        prefWidth = -1.0
                        isSortable = false
                    }
                    column("Critic Score", Game::criticScore) {
                        minWidth = 68.0
                        prefWidth = -1.0
                        isSortable = false
                    }
                    column("User Score", Game::userScore) {
                        minWidth = 64.0
                        prefWidth = -1.0
                        isSortable = false
                    }
                    column("Release Date", Game::releaseDate) {
                        minWidth = 77.0
                        prefWidth = -1.0
                        isSortable = false
                    }
                    column("Path", Game::path) {
                        minWidth = 400.0
                        prefWidth = -1.0
                        isSortable = false
                    }

                    contextmenu {
                        menuitem("Delete") { controller.deleteGame() }
                    }
                }
            }

            gridpane {
                gridpaneConstraints {
                    hGrow = Priority.ALWAYS
                    minWidth = 10.0

                    vGrow = Priority.ALWAYS
                    vAlignment = VPos.BOTTOM    // Or center?
                    minHeight = 10.0
                    fillHeight = false
                }
                gridpane {
                    hgap = 3.0
                    vgap = 3.0
                    alignment = Pos.BOTTOM_CENTER
                    gridpaneConstraints {
                        columnIndex = 1

                        hAlignment = HPos.LEFT  // Or Right?
                        hGrow = Priority.SOMETIMES
                        fillWidth = false
                        minWidth = Double.NEGATIVE_INFINITY

                        vGrow = Priority.SOMETIMES  // Or Always?
                        fillHeight = false
                        minHeight = Double.NEGATIVE_INFINITY
                        maxHeight = 60.0
                    }

                    label("Path:") { gridpaneConstraints { rowIndex = 0 } }
                    readOnlyTextField { gridpaneConstraints { rowIndex = 0; columnIndex = 1 } }

                    label("Name:") { gridpaneConstraints { rowIndex = 1 } }
                    readOnlyTextField { gridpaneConstraints { rowIndex = 1; columnIndex = 1 } }

                    label("Description:") { gridpaneConstraints { rowIndex = 2 } }
                    readOnlyTextArea { isWrapText = true; gridpaneConstraints { rowIndex = 2; columnIndex = 1 } }

                    label("Release Date:") { gridpaneConstraints { rowIndex = 3 } }
                    readOnlyTextField { gridpaneConstraints { rowIndex = 3; columnIndex = 1 } }

                    label("Critic Score:") { gridpaneConstraints { rowIndex = 4 } }
                    readOnlyTextField { gridpaneConstraints { rowIndex = 4; columnIndex = 1 } }

                    label("User Score:") { gridpaneConstraints { rowIndex = 5 } }
                    readOnlyTextField { gridpaneConstraints { rowIndex = 5; columnIndex = 1 } }

                    label("Genres:") { gridpaneConstraints { rowIndex = 6 } }
                    readOnlyTextField { gridpaneConstraints { rowIndex = 6; columnIndex = 1 } }

                    label("URL:") { gridpaneConstraints { rowIndex = 7 } }
                    hyperlink { gridpaneConstraints { rowIndex = 7; columnIndex = 1 } }
                }

                imageview {
                    isPreserveRatio = true
                    isPickOnBounds = true

                    padding { bottom = 5.0; left = 5.0; right = 5.0 }
                }
            }
        }
    }
}