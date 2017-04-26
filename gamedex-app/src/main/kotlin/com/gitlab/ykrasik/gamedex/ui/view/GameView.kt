package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.preferences.UserPreferences
import com.gitlab.ykrasik.gamedex.ui.enumComboBox
import com.gitlab.ykrasik.gamedex.ui.nonClosableTab
import com.gitlab.ykrasik.gamedex.ui.readOnlyTextField
import com.gitlab.ykrasik.gamedex.ui.verticalSeparator
import com.gitlab.ykrasik.gamedex.util.containsIgnoreCase
import com.gitlab.ykrasik.gamedex.util.sizeProperty
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 22:14
 */
// TODO: Should only be 1 view, that is changed through properties
class GameView : View("Games") {
    private val controller: GameController by inject()
    private val userPreferences: UserPreferences by di()

    private val gameWallView: GameWallView by inject()
    private val gameListView: GameListView by inject()

    private val xIcon = resources.imageview("x-small-icon.png")

    override val root = borderpane {
        top {
            toolbar {
                prefHeight = 40.0

                gridpane {
                    hgap = 2.0
                    row {
                        val search = textfield { 
                            promptText = "Search"
                            controller.games.filterWhen(textProperty(), { query, game ->
                                if (query.isEmpty()) true
                                else game.name.containsIgnoreCase(query)
                            })
                        }
                        button(graphic = xIcon) { setOnAction { search.clear() } }
                    }
                }

                verticalSeparator(10.0)

                gridpane {
                    hgap = 2.0
                    row {
                        button("Genre Filter") { setOnAction { controller.filterGenres() } }
                        readOnlyTextField()
                        button(graphic = xIcon)
                    }
                }

                verticalSeparator(10.0)

                gridpane {
                    hgap = 2.0
                    row {
                        button("Library Filter") { setOnAction { controller.filterLibraries() } }
                        readOnlyTextField()
                        button(graphic = xIcon)
                    }
                }

                verticalSeparator(10.0)

                gridpane {
                    hgap = 2.0
                    setMinSize(10.0, 10.0)
                    row {
                        label("Sort:")
                        enumComboBox(userPreferences.gameSortProperty)
                    }
                }

                verticalSeparator(10.0)

                spacer()

                checkbox("Hands Free Mode", userPreferences.handsFreeModeProperty)

                verticalSeparator(10.0)

                button("Refresh Games") {
                    isDefaultButton = true
                    setOnAction { controller.refreshGames() }
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

    val gameSizeProperty = controller.games.sizeProperty().asString("Games: %d")
}
