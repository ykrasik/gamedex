package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameDataType
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.controller.LibraryController
import com.gitlab.ykrasik.gamedex.core.GameProviderService
import com.gitlab.ykrasik.gamedex.core.SortedFilteredGames
import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.ui.*
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventTarget
import javafx.geometry.Pos
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

    // FIXME: Change search -> sync, refresh maybe to download?
    override fun ToolBar.constructToolbar() {
        gamesLabel()
        verticalSeparator()
        filterButton()
        verticalSeparator()
        sortButton()
        verticalSeparator()
        reportButton()
        verticalSeparator()

        spacer()

        verticalSeparator()
        searchButton()
        verticalSeparator()
        refreshButton()
        verticalSeparator()
        cleanupButton()
        verticalSeparator()
    }

    override val root = stackpane()

    init {
        settings.displayTypeProperty.perform {
            root.replaceChildren(it!!.toNode())
        }
    }

    private fun EventTarget.gamesLabel() = label {
        textProperty().bind(gameController.sortedFilteredGames.games.sizeProperty().asString("Games: %d"))
    }

    private fun EventTarget.filterButton() = buttonWithPopover("Filter", FontAwesome.Glyph.FILTER.toGraphic { size(toolbarGraphicSize) }) {
        val realLibraries = libraryController.libraries.filtered { it.platform != Platform.excluded }

        popoverContent.form {
            fieldset {
                field {
                    jfxButton("Clear all", graphic = clearGraphic()) {
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
                        left = searchGraphic(18.0)
                        gameController.sortedFilteredGames.searchQueryProperty.bindBidirectional(textProperty())
                        requestFocus()
                    }
                    children += search
                }
                separator()
                field("Platform") {
                    // SortedFilteredList because regular sortedList doesn't fire changeEvents, for some reason.
                    val platformsWithLibraries = realLibraries.mapping { it.platform }.distincting().sortedFiltered()
                    platformsWithLibraries.sortedItems.setComparator { o1, o2 -> o1.key.compareTo(o2.key) }

                    popoverComboMenu(
                        possibleItems = platformsWithLibraries,
                        selectedItemProperty = gameController.sortedFilteredGames.platformFilterProperty,
                        arrowLocation = PopOver.ArrowLocation.LEFT_TOP,
                        styleClass = Style.filterButton,
                        itemStyleClass = Style.platformItem,
                        text = Platform::key,
                        graphic = { it.toLogo() }
                    )
                }
                separator()
                vbox {
                    realLibraries.filtering(gameController.sortedFilteredGames.platformFilterProperty.toPredicateF { platform, library: Library ->
                        library.platform == platform
                    }).performing { librariesWithPlatform ->
                        replaceChildren {
                            if (librariesWithPlatform.size <= 1) return@replaceChildren

                            field("Library") {
                                val selectedLibraries = gameController.sortedFilteredGames.sourceIdsFilterProperty.mapping { sourceId ->
                                    realLibraries.find { it.id == sourceId }!!
                                }
                                popoverToggleMenu(
                                    possibleItems = librariesWithPlatform,
                                    selectedItems = selectedLibraries,
                                    arrowLocation = PopOver.ArrowLocation.LEFT_TOP,
                                    styleClass = Style.filterButton,
                                    itemStyleClass = Style.libraryItem,
                                    text = Library::name
                                )
                                selectedLibraries.onChange {
                                    gameController.sortedFilteredGames.sourceIdsPerPlatformFilter +=
                                        gameController.sortedFilteredGames.platformFilter to it!!.map { it.id }
                                }
                            }
                            separator()
                        }
                    }
                }
                field("Genre") {
                    // SortedFilteredList because regular sortedList doesn't fire changeEvents, for some reason.
                    val genres = gameController.genres.sortedFiltered()
                    genres.sortedItems.setComparator { o1, o2 -> o1.compareTo(o2) }
                    popoverComboMenu(
                        possibleItems = listOf(SortedFilteredGames.allGenres).observable().adding(genres),
                        selectedItemProperty = gameController.sortedFilteredGames.genreFilterProperty,
                        arrowLocation = PopOver.ArrowLocation.LEFT_TOP,
                        styleClass = Style.filterButton,
                        itemStyleClass = Style.genreItem,
                        text = { it },
                        menuItemOp = {
                            if (it == SortedFilteredGames.allGenres) {
                                separator()
                            }
                        }
                    )
                }
                separator()
                field("Tag") {
                    // SortedFilteredList because regular sortedList doesn't fire changeEvents, for some reason.
                    val tags = gameController.tags.sortedFiltered()
                    tags.sortedItems.setComparator { o1, o2 -> o1.compareTo(o2) }
                    popoverComboMenu(
                        possibleItems = listOf(SortedFilteredGames.allTags).observable().adding(tags),
                        selectedItemProperty = gameController.sortedFilteredGames.tagFilterProperty,
                        arrowLocation = PopOver.ArrowLocation.LEFT_TOP,
                        styleClass = Style.filterButton,
                        itemStyleClass = Style.tagItem,
                        text = { it },
                        menuItemOp = {
                            if (it == SortedFilteredGames.allTags) {
                                separator()
                            }
                        }
                    )
                }
                separator()
            }
        }
    }.apply {
        shortcut("ctrl+f")
        tooltip("Ctrl+f")
    }

    private fun EventTarget.sortButton() {
        val sortProperty = gameController.sortedFilteredGames.sortProperty
        val sortOrderProperty = gameController.sortedFilteredGames.sortOrderProperty

        fun constructPossibleItems() =
            GameSettings.Sort.values().toList().map { sort ->
                sort to (if (sort == sortProperty.value) sortOrderProperty.value.toggle() else TableColumn.SortType.DESCENDING)
            }

        val possibleItems = constructPossibleItems().observable()
        sortProperty.onChange { possibleItems.setAll(constructPossibleItems()) }
        sortOrderProperty.onChange { possibleItems.setAll(constructPossibleItems()) }

        val selectedItemProperty = SimpleObjectProperty(sortProperty.value to sortOrderProperty.value)
        popoverComboMenu(
            possibleItems = possibleItems,
            selectedItemProperty = selectedItemProperty,
            itemStyleClass = Style.sortItem,
            text = { it.first.key },
            graphic = { it.second.toGraphic() }
        )
        selectedItemProperty.onChange {
            val (sort, order) = it!!
            sortProperty.value = sort
            sortOrderProperty.value = order
        }
    }

    private fun EventTarget.reportButton() = buttonWithPopover("Report", reportGraphic()) {
        // TODO: Does using a form make all buttons the same size without a custom style?
        popoverContent.apply {
            reportButton("Duplicate Games") {
                addClass(Style.reportButton)
                tooltip("Duplicate games report")
                setOnAction {
                    this@buttonWithPopover.hide()
                    TODO()  // FIXME: Implement - consider checking for provider apiUrl duplication.
//                    val task = gameController.refreshAllGames()
                }
            }
            separator()
            reportButton("Name-Folder Mismatch") {
                addClass(Style.reportButton)
                tooltip("Detect all games whose name doesn't match their folder name")
                setOnAction {
                    this@buttonWithPopover.hide()
                    TODO()  // FIXME: Implement
                }
            }
        }
    }.apply {
        enableWhen { gameController.canRunLongTask }
    }

    private fun EventTarget.searchButton() = buttonWithPopover("Search", searchGraphic()) {
        popoverContent.apply {
            val chooseResultsProperty = GameProviderService.SearchConstraints.ChooseResults.chooseIfNonExact.toProperty()
            popoverComboMenu(
                possibleItems = GameProviderService.SearchConstraints.ChooseResults.values().toList().observable(),
                selectedItemProperty = chooseResultsProperty,
                arrowLocation = PopOver.ArrowLocation.RIGHT_TOP,
                styleClass = Style.searchButton,
                itemStyleClass = Style.chooseResultsItem,
                text = GameProviderService.SearchConstraints.ChooseResults::key
            )
            separator()
            searchButton("New Games") {
                addClass(Style.searchButton)
                tooltip("Search all libraries for new games")
                setOnAction {
                    this@buttonWithPopover.hide()
                    gameController.scanNewGames(chooseResultsProperty.value)
                }
            }
            separator()
            searchButton("Games without Providers") {
                addClass(Style.searchButton)
                tooltip("Search all games that don't already have all available providers")
                setOnAction {
                    this@buttonWithPopover.hide()
                    gameController.rediscoverAllGames(chooseResultsProperty.value)
                }
            }
            separator()
            searchButton("Filtered Games") {
                addClass(Style.searchButton)
                tooltip("Search currently filtered games that don't already have all available providers")
                setOnAction {
                    this@buttonWithPopover.hide()
                    TODO()      // FIXME: Implement
//                    val task = gameController.rediscoverAllGames(chooseResultsProperty.value)
                }
            }
        }
    }.apply {
        enableWhen { gameController.canRunLongTask }
    }

    private fun EventTarget.refreshButton() = buttonWithPopover("Refresh", refreshGraphic()) {
        popoverContent.apply {
            // TODO: Instead, display a "Games Older Than"
//            val chooseResultsProperty = GameProviderService.SearchConstraints.ChooseResults.chooseIfNonExact.toProperty()
//            popoverComboMenu(
//                possibleItems = GameProviderService.SearchConstraints.ChooseResults.values().toList().observable(),
//                selectedItemProperty = chooseResultsProperty,
//                arrowLocation = PopOver.ArrowLocation.RIGHT_TOP,
//                styleClass = Style.searchButton,
//                itemStyleClass = Style.chooseResultsItem,
//                text = GameProviderService.SearchConstraints.ChooseResults::key
//            )
            separator()
            refreshButton("All Games") {
                addClass(Style.searchButton)
                tooltip("Refresh all games older than") // TODO: insert currently set older than duration
                setOnAction {
                    this@buttonWithPopover.hide()
                    gameController.refreshAllGames()
                }
            }
            separator()
            refreshButton("Filtered Games") {
                addClass(Style.searchButton)
                tooltip("Refresh filtered games older than") // TODO: insert currently set older than duration
                setOnAction {
                    this@buttonWithPopover.hide()
                    TODO()      // FIXME: Implement
                }
            }
        }
    }.apply {
        enableWhen { gameController.canRunLongTask }
    }

    private fun EventTarget.cleanupButton() = deleteButton("Cleanup") {
        addClass(CommonStyle.toolbarButton)
        enableWhen { gameController.canRunLongTask }
        setOnAction { gameController.cleanup() }
    }

    private fun GameSettings.DisplayType.toNode() = when (this) {
        GameSettings.DisplayType.wall -> gameWallView.root
        GameSettings.DisplayType.list -> gameListView.root
    }

    private fun TableColumn.SortType.toGraphic() = when (this) {
        TableColumn.SortType.ASCENDING -> FontAwesome.Glyph.SORT_ASC.toGraphic()
        TableColumn.SortType.DESCENDING -> FontAwesome.Glyph.SORT_DESC.toGraphic()
    }.apply { size(toolbarGraphicSize) }

    private fun TableColumn.SortType.toggle() = when (this) {
        TableColumn.SortType.ASCENDING -> TableColumn.SortType.DESCENDING
        TableColumn.SortType.DESCENDING -> TableColumn.SortType.ASCENDING
    }

    companion object {
        inline fun EventTarget.gameContextMenu(controller: GameController, crossinline game: () -> Game) = contextmenu {
            menuitem("View", graphic = viewGraphic(20.0)) { controller.viewDetails(game()) }
            separator()
            menuitem("Edit", graphic = editGraphic(20.0)) { controller.editDetails(game()) }
            menuitem("Change Thumbnail", graphic = thumbnailGraphic(22.0)) { controller.editDetails(game(), initialTab = GameDataType.thumbnail) }
            separator()
            menuitem("Tag", graphic = tagGraphic(20.0)) { controller.tag(game()) }
            separator()
            menuitem("Refresh", graphic = refreshGraphic(20.0)) { controller.refreshGame(game()) }.apply { enableWhen { controller.canRunLongTask } }
            menuitem("Search", graphic = searchGraphic(20.0)) { controller.searchGame(game()) }.apply { enableWhen { controller.canRunLongTask } }
            separator()
            menuitem("Delete", graphic = deleteGraphic(20.0)) { controller.delete(game()) }
        }
    }

    class Style : Stylesheet() {
        companion object {
            val clearFiltersButton by cssclass()
            val filterButton by cssclass()
            val platformItem by cssclass()
            val libraryItem by cssclass()
            val genreItem by cssclass()
            val tagItem by cssclass()
            val sortItem by cssclass()
            val reportButton by cssclass()
            val searchButton by cssclass()
            val chooseResultsItem by cssclass()
            val refreshButton by cssclass()

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

            libraryItem {
                prefWidth = 160.px
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
                prefWidth = 140.px
                alignment = Pos.CENTER_LEFT
            }

            reportButton {
                prefWidth = 180.px
                alignment = Pos.CENTER_LEFT
            }

            searchButton {
                prefWidth = 200.px
                alignment = Pos.CENTER_LEFT
            }

            chooseResultsItem {
                prefWidth = 240.px
                alignment = Pos.CENTER_LEFT
            }

            refreshButton {
                prefWidth = 160.px
                alignment = Pos.CENTER_LEFT
            }
        }
    }
}
