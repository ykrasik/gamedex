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

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.app.api.game.*
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.common.JavaFxCommonOps
import com.gitlab.ykrasik.gamedex.app.javafx.filter.JavaFxFilterView
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.theme.GameDexStyle
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.logo
import com.gitlab.ykrasik.gamedex.javafx.theme.ratingColor
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableScreen
import com.gitlab.ykrasik.gamedex.util.toPredicate
import com.gitlab.ykrasik.gamedex.util.toString
import com.jfoenix.controls.JFXListCell
import javafx.event.EventTarget
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.ContentDisplay
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
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
    ViewWithGames,
    ViewWithPlatform,
    ViewWithGameDisplayType,
    ViewWithGameSort,
    ViewWithCurrentPlatformFilter,
    ViewCanShowGameDetails {

    private val commonOps: JavaFxCommonOps by di()

    override val games = mutableListOf<Game>().sortedFiltered()

    override val sort = state(Comparator.comparing(Game::name))
    override val filter = state { _: Game -> true }

    override val availablePlatforms = mutableListOf<Platform>().asObservable()

    override val currentPlatform = userMutableState(Platform.Windows)

    override val gameDisplayType = userMutableState(GameDisplayType.Wall)

    override val searchText = userMutableState("")
    override val searchSuggestions = mutableListOf<Game>().asObservable()
    override val isShowSearchSuggestions = state(false)

    override val sortBy = userMutableState(SortBy.Name)
    override val sortOrder = userMutableState(SortOrder.Asc)

    private val gameWallView = GameWallView(games)
    private val gameListView = GameListView(games)

    private val filterView = JavaFxFilterView()
    override val currentPlatformFilter = filterView.userMutableState
    override val currentPlatformFilterIsValid = userMutableState(filterView.filterIsValid)

    override val viewGameDetailsActions = channel<ViewGameParams>()

    init {
        register()

        games.sortedItems.comparatorProperty().bind(sort.property)
        games.filteredItems.predicateProperty().bind(filter.property.binding { it.toPredicate() })
    }

    override fun HBox.buildToolbar() {
        filterButton()
        sortButton()
        displayTypeButton()
        searchField()

        spacer()

        platformButton()
    }

    override val root = stackpane {
        gameDisplayType.perform { type ->
            replaceChildren {
                when (type!!) {
                    GameDisplayType.Wall -> addComponent(gameWallView)
                    GameDisplayType.List -> addComponent(gameListView)
                }
            }
        }
    }

    private fun EventTarget.filterButton() = buttonWithPopover(graphic = Icons.filter, closeOnAction = false) {
        addComponent(filterView) {
            addOrEditFilterActions.forEach { hide() }
            deleteNamedFilterActions.forEach { hide() }
        }
    }.apply {
        tooltip("Filter")
    }

    private fun EventTarget.sortButton() = buttonWithPopover(graphic = Icons.sort, closeOnAction = false) {
        defaultHbox {
            paddingAll = 5
            popoverComboMenu(
                possibleItems = SortBy.values().toList(),
                selectedItemProperty = sortBy.property,
                text = SortBy::displayName,
                graphic = { it.icon }
            ).apply {
                addClass(GameDexStyle.toolbarButton)
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

    private fun EventTarget.displayTypeButton() = enumComboMenu(
        selectedItemProperty = gameDisplayType.property,
        text = null,
//        text = GameDisplayType::displayName,
        graphic = { it.icon },
        buttonGraphic = Icons.view
    ).apply {
        tooltip("Display Type")
    }

    private fun EventTarget.searchField() = searchTextField(this@JavaFxGameScreen, searchText.property) {
        val textField = this
        prefWidth = 400.0

        popOver(PopOver.ArrowLocation.TOP_LEFT) {
            prettyListView(searchSuggestions) {
                maxHeight = 12 * 23.0
                prefWidth = textField.prefWidth - 10

                fun showGameDetails(game: Game) {
                    popOver.hide()
                    viewGameDetailsActions.event(ViewGameParams(game, games))
                }

                setCellFactory {
                    object : JFXListCell<Game>() {
                        init {
                            setOnMouseClicked { showGameDetails(item) }
                        }

                        override fun updateItem(game: Game?, empty: Boolean) {
                            super.updateItem(game, empty)
                            if (game == null) return
                            text = null
                            graphic = HBox().apply {
                                alignment = Pos.CENTER_LEFT
                                spacing = 10.0

                                imageview(commonOps.fetchThumbnail(game)) {
                                    fitHeight = 30.0
                                    fitWidth = 30.0
                                }
                                label(game.name) {
                                    maxWidth = 270.0
                                    isWrapText = true
                                }
                                spacer()
                                vbox {
                                    alignment = Pos.TOP_CENTER
                                    paddingAll = 5.0
                                    clipRectangle(arc = 10)
                                    background = Background(BackgroundFill(game.criticScore.ratingColor, CornerRadii.EMPTY, Insets.EMPTY))
                                    label(game.criticScore?.score?.toString(decimalDigits = 1) ?: "N/A")
                                    tooltip("Critic Score")
                                }
                            }
                        }
                    }
                }

                setOnKeyPressed {
                    when (it.code) {
                        KeyCode.ENTER -> if (selectedItem != null) showGameDetails(selectedItem!!)
                        KeyCode.LEFT -> positionCaret(caretPosition - 1)
                        KeyCode.RIGHT -> positionCaret(caretPosition + 1)
//                        KeyCode.HOME -> positionCaret(0)
//                        KeyCode.END -> positionCaret(text.length)
                        else -> Unit
                    }
                }
            }
        }.apply {
            arrowSize = 0.0
            fun showUnderTextField() {
                val bounds = textField.boundsInScreen
                show(currentWindow, bounds.minX - 8, bounds.minY + 20)
            }

            textField.focusedProperty().combineLatest(isShowSearchSuggestions.property).forEachWith(textField.textProperty()) { (focused, isShowSearchSuggestions), _ ->
                if (focused && isShowSearchSuggestions) {
                    showUnderTextField()
                } else {
                    hide()
                }
            }
            textField.setOnMouseClicked {
                if (isShowSearchSuggestions.value) {
                    showUnderTextField()
                }
            }

            addEventFilter(KeyEvent.KEY_PRESSED) { e ->
                if (e.code == KeyCode.ESCAPE) {
                    clear()
                    textField.right!!.requestFocus()
                }
            }
        }
    }

    private fun EventTarget.platformButton() = popoverComboMenu(
        possibleItems = availablePlatforms,
        selectedItemProperty = currentPlatform.property,
        text = Platform::displayName,
        graphic = { it.logo }
    ).apply {
        addClass(GameDexStyle.toolbarButton, Style.platformButton)
        contentDisplay = ContentDisplay.RIGHT
        textProperty().cleanBind(games.sizeProperty.stringBinding { "Games: ${"%4d".format(it)}" })
        mouseTransparentWhen { availablePlatforms.sizeProperty.lessThanOrEqualTo(1) }
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

    private val GameDisplayType.icon
        get() = when (this) {
            GameDisplayType.Wall -> Icons.grid
            GameDisplayType.List -> Icons.list
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