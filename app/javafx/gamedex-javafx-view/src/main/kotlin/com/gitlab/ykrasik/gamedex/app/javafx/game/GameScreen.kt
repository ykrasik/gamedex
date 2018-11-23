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

package com.gitlab.ykrasik.gamedex.app.javafx.game

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.app.api.game.*
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.filter.JavaFxMenuGameFilterView
import com.gitlab.ykrasik.gamedex.app.javafx.game.discover.JavaFxDiscoverGamesView
import com.gitlab.ykrasik.gamedex.app.javafx.game.download.JavaFxGameDownloadView
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableScreen
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventTarget
import javafx.scene.layout.HBox
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 22:14
 */
class GameScreen : PresentableScreen("Games", Icons.games), ViewCanSelectPlatform, ViewCanSearchGames, ViewCanChangeGameSort {
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

    override val sortByChanges = channel<SortBy>()
    private val sortByProperty = SimpleObjectProperty<SortBy>(SortBy.name_).eventOnChange(sortByChanges)
    override var sortBy by sortByProperty

    override val sortOrderChanges = channel<SortOrder>()
    private val sortOrderProperty = SimpleObjectProperty<SortOrder>(SortOrder.asc).eventOnChange(sortOrderChanges)
    override var sortOrder by sortOrderProperty

    init {
        viewRegistry.onCreate(this)
    }

    // FIXME: Change search -> sync, refresh maybe to download?
    override fun HBox.constructToolbar() {
        platformButton()

        gap()
        
        filterButton()
        sortButton()
        searchField(this@GameScreen, searchTextProperty)

        spacer()

        addComponent(discoverGamesView)
        gap()
        addComponent(downloadView)
    }

    override val root = gameWallView.root

    private fun EventTarget.platformButton() = popoverComboMenu(
        possibleItems = availablePlatforms,
        selectedItemProperty = currentPlatformProperty,
        styleClass = CommonStyle.toolbarButton,
        text = Platform::displayName,
        graphic = { it.logo }
    ).apply {
        textProperty().cleanBind(gameWallView.games.sizeProperty.stringBinding { "Games: $it" })
        mouseTransparentWhen { availablePlatforms.sizeProperty.lessThanOrEqualTo(1) }
    }

    private fun EventTarget.sortButton() = buttonWithPopover(graphic = Icons.sort, closeOnClick = false, styleClass = null) {
        hbox(spacing = 5) {
            paddingAll = 5
            popoverComboMenu(
                possibleItems = SortBy.values().toList().observable(),
                selectedItemProperty = sortByProperty,
                styleClass = CommonStyle.toolbarButton,
                text = { it.displayName }
            )
            jfxButton {
                graphicProperty().bind(sortOrderProperty.objectBinding { if (it == SortOrder.asc) Icons.ascending else Icons.descending })
                tooltip {
                    textProperty().bind(sortOrderProperty.stringBinding { it!!.displayName })
                }
                setOnAction {
                    sortOrder = sortOrder.toggle()
                }
            }
        }
    }.apply {
        tooltip("Sort")
    }

    private fun EventTarget.filterButton() = buttonWithPopover(graphic = Icons.filter, closeOnClick = false, styleClass = null) {
        addComponent(filterView)
    }.apply {
        tooltip("Filter")
    }
}