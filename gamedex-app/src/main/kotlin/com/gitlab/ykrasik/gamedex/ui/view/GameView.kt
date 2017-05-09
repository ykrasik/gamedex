package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.controller.LibraryController
import com.gitlab.ykrasik.gamedex.preferences.AllPreferences
import com.gitlab.ykrasik.gamedex.preferences.GameDisplayType
import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.ui.fragment.GameDetailsFragment
import javafx.beans.property.ObjectProperty
import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.control.TableColumn
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
    private val gameController: GameController by di()
    private val libraryController: LibraryController by di()
    private val preferences: AllPreferences by di()

    private val gameWallView: GameWallView by inject()
    private val gameListView: GameListView by inject()

    override fun ToolBar.constructToolbar() {
        // If I ever decide to cache the constructed toolbar, this will stop functioning correctly.
        val platformsWithLibraries = Platform.values().toList().observable().filtered { platform ->
            platform != Platform.excluded && libraryController.libraries.any { it.platform == platform }
        }

        platformComboBox(gameController.sortedFilteredGames.platformFilterProperty, platformsWithLibraries)

        verticalSeparator()

        label("Genres:")
        val possibleGenres = gameController.genres.sorted().let { listOf("") + it }
        combobox(gameController.sortedFilteredGames.genreFilterProperty, possibleGenres) {
            selectionModel.select(0)
        }

        verticalSeparator()

        val search = (TextFields.createClearableTextField() as CustomTextField).apply {
            promptText = "Search"
            left = FontAwesome.Glyph.SEARCH.toGraphic()
            gameController.sortedFilteredGames.searchQueryProperty.bind(textProperty())
        }
        items += search

        verticalSeparator()

        // TODO: This is only relevant for the game wall view, make it support adding stuff to the toolbar
        label("Sort:")
        enumComboBox(gameController.sortedFilteredGames.sortProperty)
        jfxButton {
            graphicProperty().bind(gameController.sortedFilteredGames.sortOrderProperty.mapProperty { it!!.toGraphic() })
            setOnAction {
                preferences.gameWall.sortOrderProperty.toggle()
            }
        }

        spacer()

        verticalSeparator()

        label {
            textProperty().bind(gameController.sortedFilteredGames.games.sizeProperty().asString("Games: %d"))
        }

        verticalSeparator()

        // TODO: Move this under the refresh button, as a drop down button.
        checkbox("Hands Free Mode", preferences.game.handsFreeModeProperty)

        verticalSeparator()

        button("Refresh Games") {
            isDefaultButton = true
            graphic = FontAwesome.Glyph.REFRESH.toGraphic()
            setOnAction {
                val task = gameController.refreshGames()
                disableProperty().cleanBind(task.runningProperty)
            }
        }

        verticalSeparator()

        jfxButton(graphic = FontAwesome.Glyph.ELLIPSIS_V.toGraphic { size(18.0) }) {
            prefWidth = 40.0
            withPopover(PopOver.ArrowLocation.TOP_RIGHT) {
                contentNode = vbox {
                    addClass(CommonStyle.popoverMenu)

                    jfxButton("Cleanup", graphic = FontAwesome.Glyph.TRASH.toGraphic()) {
                        addClass(CommonStyle.extraButton)
                        setOnAction {
                            this@withPopover.hide()
                            val task = gameController.cleanup()
                            disableProperty().cleanBind(task.runningProperty)
                        }
                    }

                    separator()

                    jfxButton("Re-Fetch Games", graphic = FontAwesome.Glyph.RETWEET.toGraphic()) {
                        addClass(CommonStyle.extraButton)
                        setOnAction {
                            this@withPopover.hide()
                            val task = gameController.refetchAllGames()
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
        val gameDisplayType = preferences.game.displayTypeProperty.mapProperty { it!!.toNode() }
        root.children += gameDisplayType.value
        gameDisplayType.onChange {
            root.replaceChildren(it as Node)
        }
    }

    private fun GameDisplayType.toNode() = when (this) {
        GameDisplayType.wall -> gameWallView.root
        GameDisplayType.list -> gameListView.root
    }

    private fun TableColumn.SortType.toGraphic() = when (this) {
        TableColumn.SortType.ASCENDING -> FontAwesome.Glyph.ARROW_UP.toGraphic()
        TableColumn.SortType.DESCENDING -> FontAwesome.Glyph.ARROW_DOWN.toGraphic()
    }

    private fun ObjectProperty<TableColumn.SortType>.toggle() {
        value = when (value!!) {
            TableColumn.SortType.ASCENDING -> TableColumn.SortType.DESCENDING
            TableColumn.SortType.DESCENDING -> TableColumn.SortType.ASCENDING
        }
    }

    companion object {
        inline fun EventTarget.gameContextMenu(controller: GameController, crossinline game: () -> Game) = contextmenu {
            menuitem("View Details", graphic = FontAwesome.Glyph.EYE.toGraphic()) { GameDetailsFragment(game()).show() }
            separator()
            // TODO: Find better names - refresh, update, rediscover?
            menuitem("Search Again", graphic = FontAwesome.Glyph.SEARCH.toGraphic()) { controller.searchAgain(game()) }
            menuitem("Re-fetch", graphic = FontAwesome.Glyph.RETWEET.toGraphic()) { controller.refetchGame(game()) }
            separator()
            menuitem("Change Thumbnail", graphic = FontAwesome.Glyph.FILE_IMAGE_ALT.toGraphic()) { controller.changeThumbnail(game()) }
            separator()
            menuitem("Delete", graphic = FontAwesome.Glyph.TRASH.toGraphic()) { controller.delete(game()) }
        }
    }
}
