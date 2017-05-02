package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.controller.LibraryController
import com.gitlab.ykrasik.gamedex.preferences.GameDisplayType
import com.gitlab.ykrasik.gamedex.preferences.GamePreferences
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
    private val gameContorller: GameController by di()
    private val libraryContorller: LibraryController by di()
    private val preferences: GamePreferences by di()

    private val gameWallView: GameWallView by inject()
    private val gameListView: GameListView by inject()

    override fun ToolBar.constructToolbar() {
        val platformsWithLibraries = Platform.values().toList().observable().filtered { platform ->
            platform != Platform.excluded && libraryContorller.libraries.any { it.platform == platform }
        }
        combobox(preferences.platformProperty, platformsWithLibraries)

        verticalSeparator()

        val platformPredicate = preferences.platformProperty.toPredicate { platform, game: Game ->
            game.platform == platform
        }

        val search = (TextFields.createClearableTextField() as CustomTextField).apply {
            promptText = "Search"
            // TODO: Put the search icon on the right, and have it change to a 'clear' when text is typed.
            left = fontAwesomeGlyph(FontAwesome.Glyph.SEARCH)
        }
        items += search

        val searchPredicate = search.textProperty().toPredicate { query, game: Game ->
            query!!.isEmpty() || game.name.contains(query, ignoreCase = true)
        }

        val predicate = platformPredicate.and(searchPredicate)
        gameContorller.games.filteredItems.predicateProperty().bind(predicate)

        verticalSeparator()

        gridpane {
            hgap = 2.0
            setMinSize(10.0, 10.0)
            row {
                label("Sort:")
                enumComboBox(preferences.sortProperty)
            }
        }

        verticalSeparator()

        // TODO: Add a platform filter.

        spacer()

        checkbox("Hands Free Mode", preferences.handsFreeModeProperty)

        verticalSeparator()

        button("Refresh Games") {
            isDefaultButton = true
            graphic = fontAwesomeGlyph(FontAwesome.Glyph.REFRESH)
            setOnAction {
                val task = gameContorller.refreshGames()
                disableProperty().cleanBind(task.runningProperty)
            }
        }

        verticalSeparator()

        label {
            textProperty().bind(gameContorller.games.sizeProperty().asString("Games: %d"))
        }

        verticalSeparator()

        jfxButton(graphic = fontAwesomeGlyph(FontAwesome.Glyph.ELLIPSIS_V) { size(18.0) }) {
            prefWidth = 40.0
            withPopover(PopOver.ArrowLocation.TOP_RIGHT) {
                contentNode = vbox(spacing = 5.0) {
                    paddingAll = 5
                    jfxButton("Cleanup", graphic = fontAwesomeGlyph(FontAwesome.Glyph.TRASH)) {
                        addClass(Style.extraButton)
                        setOnAction {
                            this@withPopover.hide()
                            val task = gameContorller.cleanup()
                            disableProperty().cleanBind(task.runningProperty)
                        }
                    }

                    separator()

                    jfxButton("Re-Fetch Games", graphic = fontAwesomeGlyph(FontAwesome.Glyph.RETWEET)) {
                        addClass(Style.extraButton)
                        setOnAction {
                            this@withPopover.hide()
                            val task = gameContorller.refetchAllGames()
                            if (task != null) {
                                disableProperty().cleanBind(task.runningProperty)
                            }
                        }
                    }
                }
            }
        }

        verticalSeparator()
    }

    override val root = stackpane()

    init {
        val gameDisplayType = preferences.displayTypeProperty.mapProperty { it!!.toNode() }
        root.children += gameDisplayType.value
        gameDisplayType.onChange {
            root.replaceChildren(it as Node)
        }
    }

    private fun GameDisplayType.toNode() = when (this) {
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
