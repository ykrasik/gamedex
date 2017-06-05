package com.gitlab.ykrasik.gamedex.ui.view.game

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameDataType
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import com.gitlab.ykrasik.gamedex.ui.theme.deleteButton
import com.gitlab.ykrasik.gamedex.ui.theme.reportButton
import com.gitlab.ykrasik.gamedex.ui.view.GamedexScreen
import com.gitlab.ykrasik.gamedex.ui.view.game.list.GameListView
import com.gitlab.ykrasik.gamedex.ui.view.game.wall.GameWallView
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.control.TableColumn
import javafx.scene.control.ToolBar
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 22:14
 */
class GameScreen : GamedexScreen("Games") {
    private val gameController: GameController by di()
    private val settings: GameSettings by di()

    private val gameWallView: GameWallView by inject()
    private val gameListView: GameListView by inject()

    private val filterMenu: GameFilterMenu by inject()
    private val searchMenu: GameSearchMenu by inject()
    private val refreshMenu: GameRefreshMenu by inject()

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
        items += searchMenu.root
        verticalSeparator()
        items += refreshMenu.root
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
        val possibleItems = sortProperty.mapToList { sort ->
            GameSettings.SortBy.values().toList().map { sortBy ->
                GameSettings.Sort(
                    sortBy = sortBy,
                    order = if (sortBy == sort.sortBy) sort.order.toggle() else TableColumn.SortType.DESCENDING
                )
            }
        }

        popoverComboMenu(
            possibleItems = possibleItems,
            selectedItemProperty = sortProperty,
            styleClass = CommonStyle.toolbarButton,
            itemStyleClass = Style.sortItem,
            text = { it.sortBy.key },
            graphic = { it.order.toGraphic() }
        )
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
        }
    }
}
