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

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.javafx.game.GameController
import com.gitlab.ykrasik.gamedex.core.game.GameSettings
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.library.LibraryController
import com.gitlab.ykrasik.gamedex.javafx.screen.GamedexScreen
import com.gitlab.ykrasik.gamedex.ui.view.game.list.GameListView
import com.gitlab.ykrasik.gamedex.ui.view.game.menu.GameFilterMenu
import com.gitlab.ykrasik.gamedex.ui.view.game.menu.GameRefreshMenu
import com.gitlab.ykrasik.gamedex.ui.view.game.menu.GameSearchMenu
import com.gitlab.ykrasik.gamedex.ui.view.game.wall.GameWallView
import javafx.event.EventTarget
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
    private val gameSettings: GameSettings by di()

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
        gameSettings.displayTypeSubject.subscribe {
            root.replaceChildren(it!!.toNode())
        }
    }

    private fun EventTarget.platformButton() {
        // TODO: Prefer doing this through rx operators.
        val platformsWithLibraries = libraryController.realLibraries.mapping { it.platform }.distincting()
        val selectedItemProperty = gameSettings.platformSubject.toPropertyCached()
        popoverComboMenu(
            possibleItems = platformsWithLibraries,
            selectedItemProperty = selectedItemProperty,
            styleClass = CommonStyle.toolbarButton,
            text = Platform::displayName,
            graphic = { it.toLogo(26.0) }
        ).apply {
            textProperty().cleanBind(gameController.sortedFilteredGames.sizeProperty.stringBinding { "Games: $it" })
            mouseTransparentWhen { platformsWithLibraries.mapProperty { it.size <= 1 } }
        }
    }

    private fun EventTarget.sortButton() {
        val sortProperty = gameSettings.sortSubject.toPropertyCached()
        val possibleItems = gameSettings.sortSubject.map { sort ->
            GameSettings.SortBy.values().map { sortBy ->
                GameSettings.Sort(
                    sortBy = sortBy,
                    order = if (sortBy == sort.sortBy) sort.order.toggle() else GameSettings.SortType.desc
                )
            }
        }.toObservableList()

        popoverComboMenu(
            possibleItems = possibleItems,
            selectedItemProperty = sortProperty,
            styleClass = CommonStyle.toolbarButton,
            text = { it.sortBy.key },
            graphic = { it.order.toGraphic() }
        )
    }

    private fun GameSettings.DisplayType.toNode() = when (this) {
        GameSettings.DisplayType.wall -> gameWallView.root
        GameSettings.DisplayType.list -> gameListView.root
    }

    private fun GameSettings.SortType.toGraphic() = when (this) {
        GameSettings.SortType.asc -> Theme.Icon.ascending()
        GameSettings.SortType.desc -> Theme.Icon.descending()
    }
}
