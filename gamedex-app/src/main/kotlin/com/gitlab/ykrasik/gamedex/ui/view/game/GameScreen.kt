/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 * http://www.apache.org/licenses/LICENSE-2.0                               *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ****************************************************************************/

package com.gitlab.ykrasik.gamedex.ui.view.game

import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.controller.LibraryController
import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import com.gitlab.ykrasik.gamedex.ui.theme.toLogo
import com.gitlab.ykrasik.gamedex.ui.view.GamedexScreen
import com.gitlab.ykrasik.gamedex.ui.view.game.list.GameListView
import com.gitlab.ykrasik.gamedex.ui.view.game.menu.GameFilterMenu
import com.gitlab.ykrasik.gamedex.ui.view.game.menu.GameRefreshMenu
import com.gitlab.ykrasik.gamedex.ui.view.game.menu.GameSearchMenu
import com.gitlab.ykrasik.gamedex.ui.view.game.wall.GameWallView
import javafx.event.EventTarget
import javafx.scene.control.TableColumn
import javafx.scene.control.ToolBar
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 22:14
 */
class GameScreen : GamedexScreen("Games", Theme.Icon.games()) {
    private val gameController: GameController by di()
    private val libraryController: LibraryController by di()
    private val settings: GameSettings by di()

    private val gameWallView: GameWallView by inject()
    private val gameListView: GameListView by inject()

    private val filterMenu: GameFilterMenu by inject()
    private val searchMenu: GameSearchMenu by inject()
    private val refreshMenu: GameRefreshMenu by inject()

    // FIXME: Change search -> sync, refresh maybe to download?
    override fun ToolBar.constructToolbar() {
        platformButton()
        verticalSeparator()
        items += filterMenu.root
        verticalSeparator()
        sortButton()
        verticalSeparator()

        spacer()

        verticalSeparator()
        items += searchMenu.root
        verticalSeparator()
        items += refreshMenu.root
        verticalSeparator()
    }

    override val root = stackpane()

    init {
        settings.displayTypeProperty.perform {
            root.replaceChildren(it!!.toNode())
        }
    }

    private fun EventTarget.platformButton() {
        val platformsWithLibraries = libraryController.realLibraries.mapping { it.platform }.distincting()
        popoverComboMenu(
            possibleItems = platformsWithLibraries,
            selectedItemProperty = settings.platformProperty,
            styleClass = CommonStyle.toolbarButton,
            text = { it.key },
            graphic = { it.toLogo(26.0) }
        ).apply {
            textProperty().cleanBind(gameController.sortedFilteredGames.sizeProperty().stringBinding { "Games: $it" })
            mouseTransparentWhen { platformsWithLibraries.mapProperty { it.size <= 1 } }
        }
    }

    private fun EventTarget.sortButton() {
        val possibleItems = settings.sortProperty.mapToList { sort ->
            GameSettings.SortBy.values().toList().map { sortBy ->
                GameSettings.Sort(
                    sortBy = sortBy,
                    order = if (sortBy == sort.sortBy) sort.order.toggle() else TableColumn.SortType.DESCENDING
                )
            }
        }

        popoverComboMenu(
            possibleItems = possibleItems,
            selectedItemProperty = settings.sortProperty,
            styleClass = CommonStyle.toolbarButton,
            text = { it.sortBy.key },
            graphic = { it.order.toGraphic() }
        )
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
}
