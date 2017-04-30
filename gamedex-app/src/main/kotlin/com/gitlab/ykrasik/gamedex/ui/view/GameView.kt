package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.preferences.UserPreferences
import com.gitlab.ykrasik.gamedex.ui.*
import org.controlsfx.control.textfield.CustomTextField
import org.controlsfx.control.textfield.TextFields
import org.controlsfx.glyphfont.FontAwesome
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 22:14
 */
// TODO: Should only be 1 view (wall / table), view type is decided by settings
class GameView : View("Games") {
    private val controller: GameController by di()
    private val userPreferences: UserPreferences by di()

    private val gameWallView: GameWallView by inject()
    private val gameListView: GameListView by inject()

    private val xIcon = resources.imageview("x-small-icon.png")

    override val root = borderpane {
        top {
            toolbar {
                prefHeight = 40.0

                gridpane {
                    val search = (TextFields.createClearableTextField() as CustomTextField).apply {
                        promptText = "Search"
                        left = fontAwesomeGlyph(FontAwesome.Glyph.SEARCH)
                    }
                    TextFields.bindAutoCompletion(search) { request ->
                        controller.games.filter { game ->
                            val query = request.userText
                            if (query.isEmpty()) false
                            else game.name.startsWith(query, ignoreCase = true)
                        }.map { it.name }
                    }
                    children += search
                    controller.games.filterWhen(search.textProperty(), { query, game ->
                        if (query.isEmpty()) true
                        else game.name.contains(query, ignoreCase = true)
                    })
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

                // TODO: Add a platform filter.

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

                spacer()

                checkbox("Hands Free Mode", userPreferences.handsFreeModeProperty)

                verticalSeparator(10.0)

                button("Refresh Games") {
                    isDefaultButton = true
                    graphic = fontAwesomeGlyph(FontAwesome.Glyph.REFRESH)
                    setOnAction {
                        val task = controller.refreshGames()
                        disableProperty().cleanBind(task.runningProperty)
                    }
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
