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

package com.gitlab.ykrasik.gamedex.javafx.game

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.app.api.game.*
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.filter.JavaFxMenuGameFilterView
import com.gitlab.ykrasik.gamedex.app.javafx.game.discover.JavaFxDiscoverGamesView
import com.gitlab.ykrasik.gamedex.app.javafx.game.download.JavaFxGameDownloadView
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.game.wall.GameWallView
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableScreen
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventTarget
import javafx.geometry.VPos
import javafx.scene.control.ToolBar
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 22:14
 */
class GameScreen : PresentableScreen("Games", Theme.Icon.games()), ViewCanSelectPlatform, ViewCanSearchGames, ViewCanChangeGameSort {
    private val gameWallView: GameWallView by inject()
    private val filterView: JavaFxMenuGameFilterView by inject()
    private val discoverGamesView: JavaFxDiscoverGamesView by inject()
    private val downloadView: JavaFxGameDownloadView by inject()

    override val availablePlatforms = mutableListOf<Platform>().observable()

    override val currentPlatformChanges = channel<Platform>()
    private val currentPlatformProperty = SimpleObjectProperty<Platform>().eventOnChange(currentPlatformChanges)
    override var currentPlatform by currentPlatformProperty

    override val searchTextChanges = channel<String>()
    private val searchTextProperty = SimpleStringProperty("").eventOnChange(searchTextChanges)
    override var searchText by searchTextProperty

    override val sortChanges = channel<Sort>()
    private val sortProperty = SimpleObjectProperty<Sort>().eventOnChange(sortChanges)
    override var sort by sortProperty

    init {
        viewRegistry.register(this)
    }

    // FIXME: Change search -> sync, refresh maybe to download?
    override fun ToolBar.constructToolbar() {
        platformButton()
        verticalSeparator()
        searchField(this@GameScreen, searchTextProperty)
        verticalSeparator()
        sortFilterButton()

        spacer()

        verticalSeparator()
        addComponent(discoverGamesView)
        verticalSeparator()
        addComponent(downloadView)
        verticalSeparator()
    }

    override val root = gameWallView.root

    private fun EventTarget.platformButton() = popoverComboMenu(
        possibleItems = availablePlatforms,
        selectedItemProperty = currentPlatformProperty,
        styleClass = CommonStyle.toolbarButton,
        text = Platform::displayName,
        graphic = { it.toLogo(26.0) }
    ).apply {
        textProperty().cleanBind(gameWallView.games.sizeProperty.stringBinding { "Games: $it" })
        mouseTransparentWhen { availablePlatforms.sizeProperty.lessThanOrEqualTo(1) }
    }

    private fun EventTarget.sortButton() {
        val possibleItems = sortProperty.mapToList { sort ->
            SortBy.values().map { sortBy ->
                Sort(
                    sortBy = sortBy,
                    order = if (sortBy == sort!!.sortBy) sort.order.toggle() else SortOrder.desc
                )
            }
        }

        popoverComboMenu(
            possibleItems = possibleItems,
            selectedItemProperty = sortProperty,
            styleClass = CommonStyle.toolbarButton,
            text = { it.sortBy.displayName },
            graphic = { it.order.toGraphic() }
        )
    }

    private fun SortOrder.toGraphic() = when (this) {
        SortOrder.asc -> Theme.Icon.ascending()
        SortOrder.desc -> Theme.Icon.descending()
    }

    private fun EventTarget.sortFilterButton() = buttonWithPopover("", Theme.Icon.sliders(), closeOnClick = false, styleClass = null) {
        gridpane {
            paddingAll = 10.0
            vgap = 10.0
            hgap = 10.0
            row {
                label("Sort", Theme.Icon.sort()) { addClass(CommonStyle.boldText) }
                sortButton()
            }
            row {
                label("Filter", Theme.Icon.filter()) {
                    addClass(CommonStyle.boldText)
                    gridpaneConstraints {
                        vAlignment = VPos.TOP
                    }
                }
                addComponent(filterView)
            }
        }
    }
}