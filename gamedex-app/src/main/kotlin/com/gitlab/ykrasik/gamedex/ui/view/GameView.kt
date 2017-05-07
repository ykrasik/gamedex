package com.gitlab.ykrasik.gamedex.ui.view

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
class GameView : GamedexView("Games") {
    private val gameContorller: GameController by di()
    private val libraryContorller: LibraryController by di()
    private val preferences: GamePreferences by di()

    private val gameWallView: GameWallView by inject()
    private val gameListView: GameListView by inject()

    override fun ToolBar.constructToolbar() {
        // If I ever decide to cache the constructed toolbar, this will stop functioning correctly.
        val platformsWithLibraries = Platform.values().toList().observable().filtered { platform ->
            platform != Platform.excluded && libraryContorller.libraries.any { it.platform == platform }
        }

        platformComboBox(gameContorller.gamePlatformFilterProperty, platformsWithLibraries)

        verticalSeparator()

        label("Genres:")
        val possibleGenres = gameContorller.genres.sorted().let { listOf("") + it }
        combobox(gameContorller.gameGenreFilterProperty, possibleGenres) {
            selectionModel.select(0)
        }

        verticalSeparator()

        val search = (TextFields.createClearableTextField() as CustomTextField).apply {
            promptText = "Search"
            // TODO: Put the search icon on the right, and have it change to a 'clear' when text is typed.
            left = FontAwesome.Glyph.SEARCH.toGraphic()
            gameContorller.gameSearchQueryProperty.bind(textProperty())
        }
        items += search

        verticalSeparator()

        label("Sort:")
        enumComboBox(gameContorller.gameSortProperty)

        spacer()

        verticalSeparator()

        label {
            textProperty().bind(gameContorller.sortedFilteredGames.sizeProperty().asString("Games: %d"))
        }

        verticalSeparator()

        // TODO: Move this under the refresh button, as a drop down button.
        checkbox("Hands Free Mode", preferences.handsFreeModeProperty)

        verticalSeparator()

        button("Refresh Games") {
            isDefaultButton = true
            graphic = FontAwesome.Glyph.REFRESH.toGraphic()
            setOnAction {
                val task = gameContorller.refreshGames()
                disableProperty().cleanBind(task.runningProperty)
            }
        }

        verticalSeparator()

        jfxButton(graphic = FontAwesome.Glyph.ELLIPSIS_V.toGraphic { size(18.0) }) {
            prefWidth = 40.0
            withPopover(PopOver.ArrowLocation.TOP_RIGHT) {
                contentNode = vbox(spacing = 5.0) {
                    paddingAll = 5
                    jfxButton("Cleanup", graphic = FontAwesome.Glyph.TRASH.toGraphic()) {
                        addClass(Style.extraButton)
                        setOnAction {
                            this@withPopover.hide()
                            val task = gameContorller.cleanup()
                            disableProperty().cleanBind(task.runningProperty)
                        }
                    }

                    separator()

                    jfxButton("Re-Fetch Games", graphic = FontAwesome.Glyph.RETWEET.toGraphic()) {
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
