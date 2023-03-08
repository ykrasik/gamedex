/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.app.api.game.*
import com.gitlab.ykrasik.gamedex.app.api.util.*
import com.gitlab.ykrasik.gamedex.app.javafx.common.JavaFxCommonOps
import com.gitlab.ykrasik.gamedex.app.javafx.filter.JavaFxFilterView
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.theme.*
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableScreen
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.gitlab.ykrasik.gamedex.util.toPredicate
import com.gitlab.ykrasik.gamedex.util.toString
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXListCell
import javafx.beans.binding.Bindings
import javafx.event.EventTarget
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.ContentDisplay
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.*
import javafx.scene.text.FontWeight
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
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

    override val games = settableSortedFilteredList<Game>()

    override val sort = mutableStateFlow(Comparator.comparing(Game::name), debugName = "sort")
    override val filter = mutableStateFlow<(Game) -> Boolean>({ true }, debugName = "filter")

    override val availablePlatforms = mutableStateFlow(emptyList<AvailablePlatform>(), debugName = "availablePlatforms")
    override val currentPlatform = viewMutableStateFlow<AvailablePlatform>(AvailablePlatform.All, debugName = "currentPlatform")

    override val gameDisplayType = viewMutableStateFlow(GameDisplayType.Wall, debugName = "gameDisplayType")

    override val searchText = viewMutableStateFlow("", debugName = "searchText")
    override val canSearch = mutableStateFlow(IsValid.valid, debugName = "canSearch")
    override val searchActions = broadcastFlow<Unit>()

    override val searchSuggestions = mutableStateFlow(emptyList<Game>(), debugName = "searchSuggestions")
    private val isShowSearchSuggestions = Bindings.isNotEmpty(searchSuggestions.list)

    override val sortBy = viewMutableStateFlow(SortBy.Name, debugName = "sortBy")
    override val sortOrder = viewMutableStateFlow(SortOrder.Asc, debugName = "sortOrder")

    override val viewGameDetailsActions = broadcastFlow<ViewGameParams>()

    private val gameWallView = GameWallView(games)
    private val gameListView = GameListView(games)

    private val filterView = JavaFxFilterView {
        // Add a platform selection button
        children += Icons.computer.apply { paddingLeft = 12.0 }
        popoverDynamicComboMenu(
            possibleItems = availablePlatforms.list,
            selectedItemProperty = currentPlatform.property,
            text = { it.displayName },
            graphic = { it.logo },
            arrowLocation = PopOver.ArrowLocation.LEFT_TOP
        ).apply {
            addClass(GameDexStyle.toolbarButton, Style.platformButton)
            contentDisplay = ContentDisplay.RIGHT   // Putting this in the stylesheet causes JavaFx to behave buggily.
            mouseTransparentWhen { availablePlatforms.list.sizeProperty.lessThanOrEqualTo(1) }
        }
    }

    override val currentPlatformFilter = mutableStateFlow(Filter.Null, debugName = "currentPlatformFilter")
        .writeTo(filterView.filter) { it.fromView }

    override val currentPlatformFilterValidatedValue = viewMutableStateFlow(ValidatedValue(Filter.Null, IsValid.valid), debugName = "currentPlatformFilterValidatedValue")
        .writeFrom(filterView.filterValidatedValue) { it.fromView }

    init {
        register()

        games.sortedItems.comparatorProperty().bind(sort.property)
        games.filteredItems.predicateProperty().bind(filter.property.binding { it.toPredicate() })
    }

    override fun HBox.buildToolbar() {
        displayTypeButton()
        sortButton()
        filterButton()

        gap()

        searchField()

        spacer()

        header(games.sizeProperty.typesafeStringBinding { "Games: $it" }) { usePrefWidth = true }
        gap(5)
    }

    override val root = stackpane {
        gameDisplayType.perform { type ->
            replaceChildren {
                when (type) {
                    GameDisplayType.Wall -> addComponent(gameWallView)
                    GameDisplayType.List -> addComponent(gameListView)
                    else -> error("Invalid GameDisplayType: $type")
                }
            }
        }
    }

    private fun EventTarget.filterButton() = buttonWithPopover(
        graphic = Icons.filter,
        arrowLocation = PopOver.ArrowLocation.TOP_LEFT,
        closeOnAction = false
    ) {
        addComponent(filterView) {
            merge(addOrEditFilterActions, deleteNamedFilterActions).onEach { hide() }.launchIn(JavaFxScope)
        }
    }.apply {
        tooltip("Filter")
    }

    private fun EventTarget.sortButton() = buttonWithPopover(
        graphic = Icons.sort,
        arrowLocation = PopOver.ArrowLocation.TOP_LEFT,
        closeOnAction = false
    ) {
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
                    textProperty().bind(sortOrder.property.typesafeStringBinding { it.displayName })
                }
                action {
                    sortOrder *= sortOrder.v.toggle()
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
        buttonGraphic = Icons.view,
        arrowLocation = PopOver.ArrowLocation.TOP_LEFT
    ).apply {
        tooltip("Display Type")
    }

    private fun Pane.searchField() {
        lateinit var searchButton: JFXButton
        clearableTextField(searchText.property) {
            prefWidth = 400.0
            promptText = "Search"
            tooltip("Ctrl+f")
            shortcut("ctrl+f") {
                requestFocus()
                hideAllOverlays()
            }
            val textField = this

            popOver(PopOver.ArrowLocation.TOP_LEFT) {
                prettyListView(searchSuggestions.list) {
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

                    addEventHandler(KeyEvent.KEY_PRESSED) { e ->
                        val shouldConsume = when (e.code) {
                            KeyCode.ENTER -> {
                                if (selectedItem != null) {
                                    showGameDetails(selectedItem!!)
                                } else {
                                    searchButton.fire()
                                }
                                true
                            }
                            KeyCode.LEFT -> positionCaret(caretPosition - 1).let { true }
                            KeyCode.RIGHT -> positionCaret(caretPosition + 1).let { true }
                            KeyCode.HOME -> positionCaret(0).let { true }
                            KeyCode.END -> positionCaret(text.length).let { true }
                            else -> false
                        }
                        if (shouldConsume) {
                            e.consume()
                        }
                    }
                }
            }.apply {
                arrowSize = 0.0
                fun showUnderTextField() {
                    val bounds = textField.boundsInScreen
                    show(currentWindow, bounds.minX - 8, bounds.minY + 20)
                }

                textField.focusedProperty().combineLatest(isShowSearchSuggestions).forEachWith(textField.textProperty()) { (focused, isShowSearchSuggestions), _ ->
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
        searchButton = jfxButton(graphic = Icons.search) {
            tooltip("Search")
            action(searchActions)
            searchText.onChange {
                if (it.isEmpty()) {
                    fire()
                }
            }
            enableWhen(canSearch)
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

    private val GameDisplayType.icon
        get() = when (this) {
            GameDisplayType.Wall -> Icons.grid
            GameDisplayType.List -> Icons.list
        }

    private val AvailablePlatform.displayName
        get() = when (this) {
            is AvailablePlatform.All -> "All"
            is AvailablePlatform.SinglePlatform -> platform.displayName
        }

    private val AvailablePlatform.logo
        get() = when (this) {
            is AvailablePlatform.All -> Icons.devices
            is AvailablePlatform.SinglePlatform -> platform.logo
        }.size(26)

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
                focusTraversable = false
            }
        }
    }
}