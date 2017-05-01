package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.preferences.GameDisplayType
import com.gitlab.ykrasik.gamedex.preferences.UserPreferences
import com.gitlab.ykrasik.gamedex.ui.*
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.ContentDisplay
import javafx.scene.control.ToolBar
import org.controlsfx.control.PopOver
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
class GameView : GamedexView("Games") {
    private val controller: GameController by di()
    private val userPreferences: UserPreferences by di()

    private val gameWallView: GameWallView by inject()
    private val gameListView: GameListView by inject()

    override fun ToolBar.constructToolbar() {
        gridpane {
            val search = (TextFields.createClearableTextField() as CustomTextField).apply {
                promptText = "Search"
                // TODO: Put the search icon on the right, and have it change to a 'clear' when text is typed.
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

        verticalSeparator(10.0)

        label {
            textProperty().bind(controller.games.sizeProperty().asString("Games: %d"))
        }

        verticalSeparator(10.0)

        jfxButton(graphic = fontAwesomeGlyph(FontAwesome.Glyph.ELLIPSIS_V) { size(18.0) }) {
            prefWidth = 40.0
            withPopover(PopOver.ArrowLocation.TOP_RIGHT) {
                contentNode = vbox(spacing = 5.0) {
                    paddingAll = 5
                    jfxButton("Cleanup", graphic = fontAwesomeGlyph(FontAwesome.Glyph.TRASH)) {
                        addClass(Style.extraButton)
                        setOnAction {
                            this@withPopover.hide()
                            val task = controller.cleanup()
                            disableProperty().cleanBind(task.runningProperty)
                        }
                    }

                    separator()
                    
                    jfxButton("Re-Fetch Games", graphic = fontAwesomeGlyph(FontAwesome.Glyph.RETWEET)) {
                        addClass(Style.extraButton)
                        setOnAction {
                            this@withPopover.hide()
                            val task = controller.refetchGames()
                            if (task != null) {
                                disableProperty().cleanBind(task.runningProperty)
                            }
                        }
                    }
                }
            }
        }

        verticalSeparator(10.0)
    }

    override val root = stackpane()

    init {
        val gameDisplayType = userPreferences.gameDisplayTypeProperty.mapProperty { it!!.toNode() }
        root.children += gameDisplayType.value
        gameDisplayType.onChange {
            root.replaceChildren(it as Node)
        }
    }

    private fun GameDisplayType.toNode() = when(this) {
        GameDisplayType.wall -> gameWallView.root
        GameDisplayType.list -> gameListView.root
    }

    companion object {
        class Style : Stylesheet() {
            companion object {
                val extraButton by cssclass()

                init {
                    importStylesheet(Style::class)
                }
            }

            init {
                extraButton {
                    prefWidth = 140.px
                    contentDisplay = ContentDisplay.RIGHT
                    alignment = Pos.CENTER_RIGHT
                    graphicTextGap = 6.px
                }
            }
        }
    }
}
