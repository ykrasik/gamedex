package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameDataType
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.controller.LibraryController
import com.gitlab.ykrasik.gamedex.core.SortedFilteredGames
import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.ui.*
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.control.ContentDisplay
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
class GameView : GamedexScreen("Games") {
    private val gameController: GameController by di()
    private val libraryController: LibraryController by di()
    private val settings: GameSettings by di()

    private val gameWallView: GameWallView by inject()
    private val gameListView: GameListView by inject()

    override fun ToolBar.constructToolbar() {
        buttonWithPopover(
            text = "Filter",
            graphic = FontAwesome.Glyph.FILTER.toGraphic { size(21.0) },
            arrowLocation = PopOver.ArrowLocation.TOP_LEFT) {

            popoverContent.form {
                fieldset {
                    field {
                        jfxButton("Clear all", graphic = FontAwesome.Glyph.TIMES_CIRCLE.toGraphic { size(20.0) }) {
                            addClass(Style.clearFiltersButton)
                            isCancelButton = true
                            isFocusTraversable = false
                            setOnAction {
                                gameController.sortedFilteredGames.clearFilters()
                                this@buttonWithPopover.hide()
                            }
                        }
                    }
                    separator()
                    field("Search") {
                        val search = (TextFields.createClearableTextField() as CustomTextField).apply {
                            addClass(Style.filterButton)
                            promptText = "Search"
                            left = FontAwesome.Glyph.SEARCH.toGraphic()
                            gameController.sortedFilteredGames.searchQueryProperty.bindBidirectional(textProperty())
                            requestFocus()
                        }
                        children += search
                    }
                    separator()
                    field("Platform") {
                        // SortedFilteredList because regular sortedList doesn't fire changeEvents, for some reason.
                        val platformsWithLibraries = SortedFilteredList(
                            libraryController.libraries.mapped { it.platform }.distincted().filtered { it != Platform.excluded }
                        )
                        platformsWithLibraries.sortedItems.setComparator { o1, o2 -> o1.key.compareTo(o2.key) }

                        popoverComboMenu(
                            items = platformsWithLibraries,
                            arrowLocation = PopOver.ArrowLocation.LEFT_TOP,
                            styleClass = Style.filterButton,
                            text = Platform::key,
                            graphic = { it.toLogo() },
                            itemStyleClass = Style.platformItem,
                            initialSelection = gameController.sortedFilteredGames.platformFilter
                        ).bindBidirectional(gameController.sortedFilteredGames.platformFilterProperty)
                    }
                    separator()
                    field("Genre") {
                        // SortedFilteredList because regular sortedList doesn't fire changeEvents, for some reason.
                        val genres = SortedFilteredList(gameController.genres)
                        genres.sortedItems.setComparator { o1, o2 -> o1.compareTo(o2) }
                        popoverComboMenu(
                            items = listOf(SortedFilteredGames.allGenres).observable().added(genres),
                            arrowLocation = PopOver.ArrowLocation.LEFT_TOP,
                            styleClass = Style.filterButton,
                            text = { it },
                            graphic = { null },
                            itemStyleClass = Style.genreItem,
                            initialSelection = gameController.sortedFilteredGames.genreFilter,
                            menuOp = {
                                if (it == SortedFilteredGames.allGenres) {
                                    separator()
                                }
                            }
                        ).bindBidirectional(gameController.sortedFilteredGames.genreFilterProperty)
                    }
                    separator()
                    field("Tag") {
                        // SortedFilteredList because regular sortedList doesn't fire changeEvents, for some reason.
                        val tags = SortedFilteredList(gameController.tags)
                        tags.sortedItems.setComparator { o1, o2 -> o1.compareTo(o2) }
                        popoverComboMenu(
                            items = listOf(SortedFilteredGames.allTags).observable().added(tags),
                            arrowLocation = PopOver.ArrowLocation.LEFT_TOP,
                            styleClass = Style.filterButton,
                            text = { it },
                            graphic = { null },
                            itemStyleClass = Style.tagItem,
                            initialSelection = gameController.sortedFilteredGames.tagFilter,
                            menuOp = {
                                if (it == SortedFilteredGames.allTags) {
                                    separator()
                                }
                            }
                        ).bindBidirectional(gameController.sortedFilteredGames.tagFilterProperty)
                    }
                    separator()
                }
            }
        }

        verticalSeparator()

        buttonWithPopover(
            graphic = FontAwesome.Glyph.SORT.toGraphic { size(21.0) },
            arrowLocation = PopOver.ArrowLocation.TOP_LEFT) {

            GameSettings.Sort.values().forEach { sort ->
                popoverMenuItem(sort.key, styleClass = Style.sortItem) {
                    val prevSort = gameController.sortedFilteredGames.sort
                    if (prevSort == sort) {
                        gameController.sortedFilteredGames.sortOrder = gameController.sortedFilteredGames.sortOrder.toggle()
                    } else {
                        gameController.sortedFilteredGames.sortOrder = TableColumn.SortType.DESCENDING
                    }
                    gameController.sortedFilteredGames.sort = sort
                }
            }
        }.apply {
            textProperty().bind(gameController.sortedFilteredGames.sortProperty.map { it!!.toString() })
            graphicProperty().bind(gameController.sortedFilteredGames.sortOrderProperty.map { it!!.toGraphic() })
            contentDisplay = ContentDisplay.LEFT
        }

        verticalSeparator()

        spacer()

        verticalSeparator()

        jfxButton("Scan New Games", graphic = FontAwesome.Glyph.REFRESH.toGraphic { size(21.0) }) {
            addClass(CommonStyle.toolbarButton)
            isDefaultButton = true
            setOnAction {
                val task = gameController.scanNewGames()
                disableWhen { task.runningProperty }
            }
            dropDownMenu {
                popoverContent.jfxToggleButton {
                    text = "Hands Free Mode"
                    selectedProperty().bindBidirectional(settings.handsFreeModeProperty)
                }
            }
        }

        verticalSeparator()

        extraMenu {
            popoverMenuItem("Cleanup", graphic = FontAwesome.Glyph.TRASH.toGraphic()) {
                val task = gameController.cleanup()
                disableWhen { task.runningProperty }
            }

            separator()

            popoverMenuItem("Refresh Games", graphic = FontAwesome.Glyph.REFRESH.toGraphic()) {
                val task = gameController.refreshAllGames()
                disableWhen { task.runningProperty }
            }

            popoverMenuItem("Rediscover Games", graphic = FontAwesome.Glyph.SEARCH.toGraphic()) {
                val task = gameController.rediscoverAllGames()
                disableWhen { task.runningProperty }
            }
        }.apply {
            textProperty().bind(gameController.sortedFilteredGames.games.sizeProperty().asString("Games: %d"))
            contentDisplay = ContentDisplay.RIGHT
            graphicTextGap = 10.0
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
        TableColumn.SortType.ASCENDING -> FontAwesome.Glyph.SORT_ASC.toGraphic()
        TableColumn.SortType.DESCENDING -> FontAwesome.Glyph.SORT_DESC.toGraphic()
    }.apply { size(23.0) }

    private fun TableColumn.SortType.toggle() = when (this) {
        TableColumn.SortType.ASCENDING -> TableColumn.SortType.DESCENDING
        TableColumn.SortType.DESCENDING -> TableColumn.SortType.ASCENDING
    }

    companion object {
        inline fun EventTarget.gameContextMenu(controller: GameController, crossinline game: () -> Game) = contextmenu {
            menuitem("View", graphic = FontAwesome.Glyph.EYE.toGraphic()) { controller.viewDetails(game()) }
            separator()
            menuitem("Edit", graphic = FontAwesome.Glyph.PENCIL.toGraphic()) { controller.editDetails(game()) }
            menuitem("Change Thumbnail", graphic = FontAwesome.Glyph.FILE_IMAGE_ALT.toGraphic()) { controller.editDetails(game(), initialTab = GameDataType.thumbnail) }
            separator()
            menuitem("Tag", graphic = FontAwesome.Glyph.TAG.toGraphic()) { controller.tag(game()) }
            separator()
            menuitem("Refresh", graphic = FontAwesome.Glyph.REFRESH.toGraphic()) { controller.refreshGame(game()) }
            menuitem("Search", graphic = FontAwesome.Glyph.SEARCH.toGraphic()) { controller.searchGame(game()) }
            separator()
            menuitem("Delete", graphic = FontAwesome.Glyph.TRASH.toGraphic()) { controller.delete(game()) }
        }
    }

    class Style : Stylesheet() {
        companion object {
            val clearFiltersButton by cssclass()
            val filterButton by cssclass()
            val platformItem by cssclass()
            val genreItem by cssclass()
            val tagItem by cssclass()
            val sortItem by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            clearFiltersButton {
                prefWidth = 220.px
                alignment = Pos.CENTER_LEFT
            }

            filterButton {
                prefWidth = 160.px
                alignment = Pos.CENTER_LEFT
            }

            platformItem {
                prefWidth = 100.px
                alignment = Pos.CENTER_LEFT
            }

            genreItem {
                prefWidth = 160.px
                alignment = Pos.CENTER_LEFT
            }

            tagItem {
                prefWidth = 160.px
                alignment = Pos.CENTER_LEFT
            }

            sortItem {
                prefWidth = 100.px
                alignment = Pos.CENTER_LEFT
            }
        }
    }
}
