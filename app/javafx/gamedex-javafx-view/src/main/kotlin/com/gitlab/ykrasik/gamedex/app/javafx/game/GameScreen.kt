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
import com.gitlab.ykrasik.gamedex.app.javafx.filter.JavaFxGameFilterView
import com.gitlab.ykrasik.gamedex.app.javafx.game.discover.JavaFxDiscoverGamesView
import com.gitlab.ykrasik.gamedex.app.javafx.game.download.JavaFxGameDownloadView
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
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
class GameScreen : PresentableScreen("Games", Icons.games),
    ViewCanSelectPlatform, ViewCanSearchGames, ViewCanChangeGameSort, ViewWithCurrentPlatformFilter {
    private val gameWallView: GameWallView by inject()
    private val discoverGamesView: JavaFxDiscoverGamesView by inject()
    private val downloadView: JavaFxGameDownloadView by inject()
    private val filterView = JavaFxGameFilterView(onlyShowConditionsForCurrentPlatform = true)

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

    override var currentPlatformFilter by filterView.filterProperty
    override val currentPlatformFilterChanges = filterView.filterChanges

    init {
        viewRegistry.onCreate(this)
    }

    // FIXME: Change search -> sync, refresh maybe to download?
    override fun HBox.constructToolbar() {
        platformButton()

        gap()

        filterButton()
        sortButton()
        searchTextField(this@GameScreen, searchTextProperty)

        spacer()

        addComponent(discoverGamesView)
        gap()
        addComponent(downloadView)
    }

    override val root = gameWallView.root

    private fun EventTarget.platformButton() = popoverComboMenu(
        possibleItems = availablePlatforms,
        selectedItemProperty = currentPlatformProperty,
        text = Platform::displayName,
        graphic = { it.logo }
    ).apply {
        addClass(CommonStyle.toolbarButton)
        textProperty().cleanBind(gameWallView.games.sizeProperty.stringBinding { "Games: $it" })
        mouseTransparentWhen { availablePlatforms.sizeProperty.lessThanOrEqualTo(1) }
    }

    private fun EventTarget.sortButton() = buttonWithPopover(graphic = Icons.sort, closeOnClick = false) {
        defaultHbox {
            paddingAll = 5
            popoverComboMenu(
                possibleItems = SortBy.values().toList().observable(),
                selectedItemProperty = sortByProperty,
                text = { it.displayName },
                graphic = { it.icon }
            ).apply {
                addClass(CommonStyle.toolbarButton)
            }
            jfxButton {
                graphicProperty().bind(sortOrderProperty.objectBinding { if (it == SortOrder.asc) Icons.ascending else Icons.descending })
                tooltip {
                    textProperty().bind(sortOrderProperty.stringBinding { it!!.displayName })
                }
                action {
                    sortOrder = sortOrder.toggle()
                }
            }
        }
    }.apply {
        tooltip("Sort")
    }

    private fun EventTarget.filterButton() = buttonWithPopover(graphic = Icons.filter, closeOnClick = false) {
        addComponent(filterView)
    }.apply {
        tooltip("Filter")
    }

    private val SortBy.icon
        get() = when (this) {
            SortBy.name_ -> Icons.text
            SortBy.criticScore -> Icons.starFull
            SortBy.userScore -> Icons.starEmpty
            SortBy.avgScore -> Icons.starHalf
            SortBy.minScore -> Icons.min
            SortBy.maxScore -> Icons.max
            SortBy.size -> Icons.fileQuestion
            SortBy.releaseDate -> Icons.date
            SortBy.createDate -> Icons.createDate
            SortBy.updateDate -> Icons.updateDate
        }
}