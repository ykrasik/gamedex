package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.ui.controller.GameController
import com.gitlab.ykrasik.gamedex.ui.model.GameSort
import com.gitlab.ykrasik.gamedex.ui.util.nonClosableTab
import com.gitlab.ykrasik.gamedex.ui.util.readOnlyTextField
import com.gitlab.ykrasik.gamedex.ui.util.verticalSeparator
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
                    row {
                        textfield { promptText = "Search" }
                        button(graphic = imageview("/com/gitlab/ykrasik/gamedex/ui/image/x-small-icon.png"))
                    }
                }

                verticalSeparator(10.0)

                gridpane {
                    hgap = 2.0
                    row {
                        button("Genre Filter") { setOnAction { controller.filterGenres() } }
                        readOnlyTextField()
                        button(graphic = imageview("/com/gitlab/ykrasik/gamedex/ui/image/x-small-icon.png"))
                    }
                }

                verticalSeparator(10.0)

                gridpane {
                    hgap = 2.0
                    row {
                        button("Library Filter") { setOnAction { controller.filterLibraries() } }
                        readOnlyTextField()
                        button(graphic = imageview("/com/gitlab/ykrasik/gamedex/ui/image/x-small-icon.png"))
                    }
                }

                verticalSeparator(10.0)

                gridpane {
                    hgap = 2.0
                    setMinSize(10.0, 10.0)
                    row {
                        label("Sort:")
                        combobox<GameSort>()
                    }
                }

                verticalSeparator(10.0)

                spacer()

                checkbox("Don't bother me") { isMnemonicParsing = false }

                verticalSeparator(10.0)

                button("Refresh Libraries") {
                    isDefaultButton = true
                    isMnemonicParsing = false
                    minWidth = Double.NEGATIVE_INFINITY // TODO: Why?

                    setOnAction { controller.refresh() }
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
