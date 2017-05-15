package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameDataType
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.controller.LibraryController
import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.ui.*
import javafx.beans.property.ObjectProperty
import javafx.event.EventTarget
import javafx.scene.control.TableColumn
import javafx.scene.control.ToolBar
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
    private val settings: GameSettings by di()

    private val gameWallView: GameWallView by inject()
    private val gameListView: GameListView by inject()

    override fun ToolBar.constructToolbar() {
        // If I ever decide to cache the constructed toolbar, this will stop functioning correctly.
        val platformsWithLibraries = Platform.values().toList().observable().filtered { platform ->
            platform != Platform.excluded && libraryController.libraries.any { it.platform == platform }
        }

        // TODO: Combine all filters into a filter menu
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
            graphicProperty().bind(gameController.sortedFilteredGames.sortOrderProperty.map { it!!.toGraphic() })
            setOnAction {
                settings.sortOrderProperty.toggle()
            }
        }

        spacer()

        verticalSeparator()

        label {
            textProperty().bind(gameController.sortedFilteredGames.games.sizeProperty().asString("Games: %d"))
        }

        verticalSeparator()

        button("Scan New Games", graphic = FontAwesome.Glyph.REFRESH.toGraphic()) {
            isDefaultButton = true
            setOnAction {
                val task = gameController.scanNewGames()
                disableWhen { task.runningProperty }
            }
            dropDownMenu {
                checkmenuitem("Hands Free Mode") {
                    selectedProperty().bindBidirectional(settings.handsFreeModeProperty)
                }
            }
        }

        verticalSeparator()

        extraMenu {
            extraMenuItem("Cleanup", graphic = FontAwesome.Glyph.TRASH.toGraphic()) {
                val task = gameController.cleanup()
                disableWhen { task.runningProperty }
            }

            separator()

            extraMenuItem("Refresh Games", graphic = FontAwesome.Glyph.REFRESH.toGraphic()) {
                val task = gameController.refreshAllGames()
                disableWhen { task.runningProperty }
            }

            extraMenuItem("Rediscover Games", graphic = FontAwesome.Glyph.SEARCH.toGraphic()) {
                val task = gameController.rediscoverAllGames()
                disableWhen { task.runningProperty }
            }
        }

        verticalSeparator()
    }

    override val root = stackpane()

    init {
        settings.displayTypeProperty.perform {
            root.replaceChildren(it!!.toNode())
        }
    }

    private fun GameSettings.DisplayType.toNode() = when (this) {
        GameSettings.DisplayType.wall -> gameWallView.root
        GameSettings.DisplayType.list -> gameListView.root
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
            menuitem("View", graphic = FontAwesome.Glyph.EYE.toGraphic()) { controller.viewDetails(game()) }
            separator()
            menuitem("Edit", graphic = FontAwesome.Glyph.PENCIL.toGraphic()) { controller.editDetails(game()) }
            menuitem("Change Thumbnail", graphic = FontAwesome.Glyph.FILE_IMAGE_ALT.toGraphic()) { controller.editDetails(game(), initialTab = GameDataType.thumbnail) }
            separator()
            menuitem("Refresh", graphic = FontAwesome.Glyph.REFRESH.toGraphic()) { controller.refreshGame(game()) }
            menuitem("Rediscover", graphic = FontAwesome.Glyph.SEARCH.toGraphic()) { controller.rediscoverGame(game()) }
            separator()
            menuitem("Delete", graphic = FontAwesome.Glyph.TRASH.toGraphic()) { controller.delete(game()) }
        }
    }
}
