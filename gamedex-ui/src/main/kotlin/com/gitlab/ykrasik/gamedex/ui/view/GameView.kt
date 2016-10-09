package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.ui.controller.GameController
import com.gitlab.ykrasik.gamedex.ui.model.GameSort
import com.gitlab.ykrasik.gamedex.ui.util.nonClosableTab
import com.gitlab.ykrasik.gamedex.ui.util.padding
import com.gitlab.ykrasik.gamedex.ui.util.readOnlyTextField
import com.gitlab.ykrasik.gamedex.ui.util.verticalSeparator
import javafx.scene.layout.Priority
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 22:14
 */
class GameView : View("Games") {
    private val controller: GameController by inject()

    private val gameWallView: GameWallView by inject()
    private val gameListView: GameListView by inject()

    override val root = borderpane {

        top {
            toolbar {
                prefHeight = 40.0

                gridpane {
                    hgap = 2.0
                    gridpaneConstraints {
                        hGrow = Priority.SOMETIMES
                        minWidth = 10.0
                        vGrow = Priority.SOMETIMES
                        minHeight = 10.0
                        // TODO: Not sure this is equal
                        //    <columnConstraints>
                        //    <ColumnConstraints hgrow="SOMETIMES" minWidth="10.0" />
                        //    </columnConstraints>
                        //    <rowConstraints>
                        //    <RowConstraints minHeight="10.0" vgrow="SOMETIMES" />
                        //    </rowConstraints>
                    }
                    textfield { promptText = "Search" }
                    button {
                        gridpaneConstraints { columnIndex = 1 }
                        graphic = imageview("/com/gitlab/ykrasik/gamedex/ui/image/x-small-icon.png") { isPreserveRatio = true }
                    }
                }

                verticalSeparator { padding { left = 10.0; right = 10.0 } }

                gridpane {
                    hgap = 2.0
                    gridpaneConstraints {
                        hGrow = Priority.SOMETIMES
                        minWidth = 10.0
                        vGrow = Priority.SOMETIMES
                        minHeight = 10.0
                    }
                    button("Genre Filter") { setOnAction { controller.filterGenres() } }
                    readOnlyTextField { gridpaneConstraints { columnIndex = 1 } }
                    button {
                        gridpaneConstraints { columnIndex = 2 }
                        graphic = imageview("/com/gitlab/ykrasik/gamedex/ui/image/x-small-icon.png") { isPreserveRatio = true }
                    }
                }

                verticalSeparator { padding { left = 10.0; right = 10.0 } }

                gridpane {
                    hgap = 2.0
                    gridpaneConstraints {
                        hGrow = Priority.SOMETIMES
                        minWidth = 10.0
                        vGrow = Priority.SOMETIMES
                        minHeight = 10.0
                    }
                    button("Library Filter") { setOnAction { controller.filterLibraries() } }
                    readOnlyTextField { gridpaneConstraints { columnIndex = 1 } }
                    button {
                        gridpaneConstraints { columnIndex = 2 }
                        graphic = imageview("/com/gitlab/ykrasik/gamedex/ui/image/x-small-icon.png") { isPreserveRatio = true }
                    }
                }

                verticalSeparator { padding { left = 10.0; right = 10.0 } }

                gridpane {
                    hgap = 2.0
                    gridpaneConstraints {
                        hGrow = Priority.SOMETIMES
                        minWidth = 10.0
                        vGrow = Priority.SOMETIMES
                        minHeight = 10.0
                    }
                    label("Sort:")
                    combobox<GameSort> {
                        gridpaneConstraints {
                            columnIndex = 1
                        }
                    }
                }

                verticalSeparator { padding { left = 10.0; right = 10.0 } }

                spacer()

                checkbox("Don't bother me") { isMnemonicParsing = false }

                verticalSeparator { padding { left = 10.0; right = 10.0 } }

                button("Refresh Libraries") {
                    isDefaultButton = true
                    isMnemonicParsing = false
                    minWidth = Double.NEGATIVE_INFINITY // TODO: Why?
                }
            }
        }

        center {
            tabpane {
                nonClosableTab("Wall") { content = gameWallView.root }
                nonClosableTab("List") { content = gameListView.root }
            }
        }
    }
}
