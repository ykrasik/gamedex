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
import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanSearchGames
import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanSelectPlatform
import com.gitlab.ykrasik.gamedex.app.api.util.BroadcastEventChannel
import com.gitlab.ykrasik.gamedex.app.javafx.game.discover.JavaFxDiscoverGamesView
import com.gitlab.ykrasik.gamedex.app.javafx.game.download.JavaFxGameDownloadView
import com.gitlab.ykrasik.gamedex.core.game.GameUserConfig
import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfigRepository
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.game.filter.JavaFxMenuGameFilterView
import com.gitlab.ykrasik.gamedex.javafx.game.wall.GameWallView
import com.gitlab.ykrasik.gamedex.javafx.screen.PresentableScreen
import javafx.animation.FadeTransition
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.control.ToolBar
import javafx.scene.layout.StackPane
import javafx.util.Duration
import org.controlsfx.control.textfield.CustomTextField
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 22:14
 */
class GameScreen : PresentableScreen("Games", Theme.Icon.games()), ViewCanSelectPlatform, ViewCanSearchGames {
    private val userConfigRepository: UserConfigRepository by di()
    private val gameUserConfig = userConfigRepository[GameUserConfig::class]

    private val gameWallView: GameWallView by inject()
    private val filterView: JavaFxMenuGameFilterView by inject()
    private val discoverGamesView: JavaFxDiscoverGamesView by inject()
    private val downloadView: JavaFxGameDownloadView by inject()

    override val availablePlatforms = mutableListOf<Platform>().observable()

    override val currentPlatformChanges = BroadcastEventChannel<Platform>()
    private val currentPlatformProperty = SimpleObjectProperty<Platform>().eventOnChange(currentPlatformChanges)
    override var currentPlatform by currentPlatformProperty

    override val searchTextChanges = BroadcastEventChannel<String>()
    private val searchTextProperty = SimpleStringProperty("").eventOnChange(searchTextChanges)
    override var searchText by searchTextProperty

    init {
        viewRegistry.register(this)
    }

    // FIXME: Change search -> sync, refresh maybe to download?
    override fun ToolBar.constructToolbar() {
        platformButton()
        verticalSeparator()
        sortButton()
        verticalSeparator()
        filterButton()
        verticalSeparator()
        searchField()
        verticalSeparator()

        spacer()

        verticalSeparator()
        items += discoverGamesView.root
        verticalSeparator()
        items += downloadView.root
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
        val sortProperty = gameUserConfig.sortSubject.toPropertyCached()
        val possibleItems = gameUserConfig.sortSubject.map { sort ->
            GameUserConfig.SortBy.values().map { sortBy ->
                GameUserConfig.Sort(
                    sortBy = sortBy,
                    order = if (sortBy == sort.sortBy) sort.order.toggle() else GameUserConfig.SortType.desc
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

    private fun GameUserConfig.SortType.toGraphic() = when (this) {
        GameUserConfig.SortType.asc -> Theme.Icon.ascending()
        GameUserConfig.SortType.desc -> Theme.Icon.descending()
    }

    private fun EventTarget.filterButton() = buttonWithPopover("Filter", Theme.Icon.filter(), closeOnClick = false) {
        // FIXME: This is off-center
        children += filterView.root
    }

    private fun EventTarget.searchField() {
        val search = CustomTextField().apply {
            setupClearButtonField()
            useMaxWidth = true
            alignment = Pos.CENTER_LEFT
            promptText = "Search"
            left = Theme.Icon.search(18.0)
            text = searchText
            searchTextProperty.bindBidirectional(textProperty())
            focusedProperty().onChange {
                javaFx {
                    selectAll()
                }
            }
            tooltip("Ctrl+f")
        }
        addChildIfPossible(search)
        shortcut("ctrl+f") {
            search.requestFocus()
        }
    }

    private fun CustomTextField.setupClearButtonField() {
        val clearButton = jfxButton(graphic = Theme.Icon.clear(size = 14.0)) {
            isCancelButton = true
            opacity = 0.0
            cursor = Cursor.DEFAULT
            managedProperty().bind(editableProperty())
            visibleProperty().bind(editableProperty())
            setOnAction {
                requestFocus()
                clear()
            }
        }

        right = StackPane().apply {
            padding {
                top = 4
                bottom = 3
            }
            addChildIfPossible(clearButton)
        }

        val fader = FadeTransition(Duration.millis(350.0), clearButton)
        fader.cycleCount = 1
        fun setButtonVisible(visible: Boolean) {
            fader.fromValue = if (visible) 0.0 else 1.0
            fader.toValue = if (visible) 1.0 else 0.0
            fader.play()
        }

        textProperty().onChange {
            val isTextEmpty = text == null || text.isEmpty()
            val isButtonVisible = fader.node.opacity > 0

            if (isTextEmpty && isButtonVisible) {
                setButtonVisible(false)
            } else if (!isTextEmpty && !isButtonVisible) {
                setButtonVisible(true)
            }
        }
    }
}
