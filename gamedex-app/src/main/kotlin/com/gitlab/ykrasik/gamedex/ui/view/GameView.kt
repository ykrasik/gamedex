package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameDataType
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.core.GameProviderService
import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.ui.theme.*
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.control.TableColumn
import javafx.scene.control.ToolBar
import javafx.scene.input.MouseEvent
import org.controlsfx.control.PopOver
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 22:14
 */
class GameView : GamedexScreen("Games") {
    private val gameController: GameController by di()
    private val settings: GameSettings by di()

    private val gameWallView: GameWallView by inject()
    private val gameListView: GameListView by inject()

    private val filterMenu: GameFilterMenu by inject()

    // FIXME: Change search -> sync, refresh maybe to download?
    override fun ToolBar.constructToolbar() {
        gamesLabel()
        verticalSeparator()
        items += filterMenu.root
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
            styleClass = CommonStyle.toolbarButton,
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

    private fun EventTarget.reportButton() = buttonWithPopover("Report", Theme.Icon.report()) {
        // TODO: Does using a form make all buttons the same size without a custom style?
        reportButton("Duplicate Games") {
            addClass(Style.reportButton)
            tooltip("Duplicate games report")
            setOnAction {
                TODO()  // FIXME: Implement - consider checking for provider apiUrl duplication.
//                    val task = gameController.refreshAllGames()
            }
        }
        separator()
        reportButton("Name-Folder Mismatch") {
            addClass(Style.reportButton)
            tooltip("Detect all games whose name doesn't match their folder name")
            setOnAction {
                TODO()  // FIXME: Implement
            }
        }
    }.apply {
        enableWhen { gameController.canRunLongTask }
    }

    private fun EventTarget.searchButton() = searchButton {
        enableWhen { gameController.canRunLongTask }
        val chooseResultsProperty = GameProviderService.SearchConstraints.ChooseResults.chooseIfNonExact.toProperty()
        val leftPopover = popOver(PopOver.ArrowLocation.RIGHT_TOP, closeOnClick = false) {
            popoverComboMenu(
                possibleItems = GameProviderService.SearchConstraints.ChooseResults.values().toList().observable(),
                selectedItemProperty = chooseResultsProperty,
                styleClass = Style.searchButton,
                itemStyleClass = Style.chooseResultsItem,
                text = GameProviderService.SearchConstraints.ChooseResults::key
            )
        }
        val downPopover = popOver {
            addEventFilter(MouseEvent.MOUSE_CLICKED) { leftPopover.hide() }
            searchButton("New Games") {
                addClass(Style.searchButton)
                tooltip("Search all libraries for new games")
                setOnAction { gameController.scanNewGames(chooseResultsProperty.value) }
            }
            separator()
            searchButton("Games without Providers") {
                addClass(Style.searchButton)
                tooltip("Search all games that don't already have all available providers")
                setOnAction { gameController.rediscoverAllGames(chooseResultsProperty.value) }
            }
            separator()
            searchButton("Filtered Games") {
                addClass(Style.searchButton)
                tooltip("Search currently filtered games that don't already have all available providers")
                setOnAction { gameController.rediscoverFilteredGames(chooseResultsProperty.value) }
            }
        }
        setOnAction { downPopover.toggle(this); leftPopover.toggle(this) }
    }

    private fun EventTarget.refreshButton() = buttonWithPopover("Refresh", Theme.Icon.refresh()) {
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
                gameController.refreshAllGames()
            }
        }
        separator()
        refreshButton("Filtered Games") {
            addClass(Style.searchButton)
            tooltip("Refresh filtered games older than") // TODO: insert currently set older than duration
            setOnAction {
                TODO()      // FIXME: Implement
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
        TableColumn.SortType.ASCENDING -> Theme.Icon.ascending()
        TableColumn.SortType.DESCENDING -> Theme.Icon.descending()
    }

    private fun TableColumn.SortType.toggle() = when (this) {
        TableColumn.SortType.ASCENDING -> TableColumn.SortType.DESCENDING
        TableColumn.SortType.DESCENDING -> TableColumn.SortType.ASCENDING
    }

    companion object {
        inline fun EventTarget.gameContextMenu(controller: GameController, crossinline game: () -> Game) = contextmenu {
            menuitem("View", graphic = Theme.Icon.view(20.0)) {
                controller.viewDetails(game())
            }
            separator()
            menuitem("Edit", graphic = Theme.Icon.edit(20.0)) {
                controller.editDetails(game())
            }
            menuitem("Change Thumbnail", graphic = Theme.Icon.thumbnail(22.0)) {
                controller.editDetails(game(), initialTab = GameDataType.thumbnail)
            }
            separator()
            menuitem("Tag", graphic = Theme.Icon.tag(20.0)) {
                controller.tag(game())
            }
            separator()
            menuitem("Refresh", graphic = Theme.Icon.refresh(20.0)) {
                controller.refreshGame(game())
            }.apply { enableWhen { controller.canRunLongTask } }
            menuitem("Search", graphic = Theme.Icon.search(20.0)) {
                controller.searchGame(game())
            }.apply { enableWhen { controller.canRunLongTask } }
            separator()
            menuitem("Delete", graphic = Theme.Icon.delete(20.0)) {
                controller.delete(game())
            }
        }
    }

    class Style : Stylesheet() {
        companion object {
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
