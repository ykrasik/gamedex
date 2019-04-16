/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.app.javafx.filter.JavaFxFilterView
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.logo
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableScreen
import javafx.event.EventTarget
import javafx.scene.control.ContentDisplay
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.HBox
import javafx.scene.text.FontWeight
import org.controlsfx.control.PopOver
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 22:14
 */
class JavaFxGameScreen : PresentableScreen("Games", Icons.games),
    ViewCanSelectPlatform,
    ViewCanSearchGames,
    ViewCanChangeGameSort,
    ViewWithCurrentPlatformFilter {

    private val gameWallView: GameWallView by inject()
    private val filterView = JavaFxFilterView(onlyShowFiltersForCurrentPlatform = true)

    override val availablePlatforms = mutableListOf<Platform>().observable()

    override val currentPlatform = userMutableState(Platform.Windows)

    override val searchText = userMutableState("")
    override val autoCompleteSuggestions = state<List<String>>(emptyList())
    override val isShowAutoCompleteSuggestions = state(false)

    override val sortBy = userMutableState(SortBy.Name)
    override val sortOrder = userMutableState(SortOrder.Asc)

    override val currentPlatformFilter = filterView.externalMutations
    override val currentPlatformFilterIsValid = userMutableState(filterView.filterIsValid)

    init {
        register()
    }

    override fun HBox.buildToolbar() {
        filterButton()
        sortButton()
        searchField()

        spacer()

        platformButton()
    }

    override val root = gameWallView.root

    private fun EventTarget.platformButton() = popoverComboMenu(
        possibleItems = availablePlatforms,
        selectedItemProperty = currentPlatform.property,
        text = Platform::displayName,
        graphic = { it.logo },
        arrowLocation = PopOver.ArrowLocation.TOP_RIGHT
    ).apply {
        addClass(CommonStyle.toolbarButton, Style.platformButton)
        contentDisplay = ContentDisplay.RIGHT
        textProperty().cleanBind(gameWallView.games.sizeProperty.stringBinding { "Games: ${"%4d".format(it)}" })
        mouseTransparentWhen { availablePlatforms.sizeProperty.lessThanOrEqualTo(1) }
    }

    private fun EventTarget.sortButton() = buttonWithPopover(graphic = Icons.sort, onClickBehavior = PopOverOnClickBehavior.Ignore) {
        defaultHbox {
            paddingAll = 5
            popoverComboMenu(
                possibleItems = SortBy.values().toList().observable(),
                selectedItemProperty = sortBy.property,
                text = SortBy::displayName,
                graphic = { it.icon }
            ).apply {
                addClass(CommonStyle.toolbarButton)
            }
            jfxButton {
                graphicProperty().bind(sortOrder.property.objectBinding { if (it == SortOrder.Asc) Icons.ascending else Icons.descending })
                tooltip {
                    textProperty().bind(sortOrder.property.stringBinding { it!!.displayName })
                }
                action {
                    sortOrder.valueFromView = sortOrder.value.toggle()
                }
            }
        }
    }.apply {
        tooltip("Sort")
    }

    private fun EventTarget.filterButton() = buttonWithPopover(graphic = Icons.filter, onClickBehavior = PopOverOnClickBehavior.Ignore) {
        addComponent(filterView)
    }.apply {
        tooltip("Filter")
    }

    private fun EventTarget.searchField() = searchTextField(this@JavaFxGameScreen, searchText.property) {
        val textField = this
        val suggestions = mutableListOf<String>().observable()
        autoCompleteSuggestions.onChange {
            suggestions.setAll(it)
        }
        popOver(PopOver.ArrowLocation.LEFT_TOP) {
            customListView(suggestions) {
                maxHeight = 6 * 23.0
                prefWidth = 400.0
                selectionModel.selectedItemProperty().onChange {
                    if (it != null) {
                        text = it
                    }
                }
            }
        }.apply {
            addEventFilter(KeyEvent.KEY_PRESSED) { e ->
                if (e.code == KeyCode.ESCAPE) {
                    clear()
                }
            }
            isShowAutoCompleteSuggestions.onChange {
                if (it) {
                    show(textField)
                } else {
                    hide()
                }
            }
            textField.focusedProperty().onChange {
                if (it && suggestions.isNotEmpty()) {
                    show(textField)
                }
            }
        }
    }

    private val SortBy.icon
        get() = when (this) {
            SortBy.Name -> Icons.text
            SortBy.CriticScore -> Icons.starFull
            SortBy.UserScore -> Icons.starEmpty
            SortBy.AvgScore -> Icons.starHalf
            SortBy.MinScore -> Icons.min
            SortBy.MaxScore -> Icons.max
            SortBy.Size -> Icons.fileQuestion
            SortBy.ReleaseDate -> Icons.date
            SortBy.CreateDate -> Icons.createDate
            SortBy.UpdateDate -> Icons.updateDate
        }

    class Style : Stylesheet() {
        companion object {
            val platformButton by cssclass()

            init {
                importStylesheetSafe(Style::class)
            }
        }

        init {
            platformButton {
                fontSize = 15.px
                fontWeight = FontWeight.BOLD
            }
        }
    }
}