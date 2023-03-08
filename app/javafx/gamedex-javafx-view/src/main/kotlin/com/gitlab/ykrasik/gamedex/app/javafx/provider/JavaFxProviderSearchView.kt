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

package com.gitlab.ykrasik.gamedex.app.javafx.provider

import com.gitlab.ykrasik.gamedex.app.api.provider.GameSearchState
import com.gitlab.ykrasik.gamedex.app.api.provider.ProviderSearchView
import com.gitlab.ykrasik.gamedex.app.api.util.broadcastFlow
import com.gitlab.ykrasik.gamedex.app.javafx.common.JavaFxCommonOps
import com.gitlab.ykrasik.gamedex.app.javafx.game.GameDetailsSummaryBuilder
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.theme.*
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.gitlab.ykrasik.gamedex.util.or
import com.jfoenix.controls.JFXButton
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.text.FontWeight
import tornadofx.*

/**
 * User: ykrasik
 * Date: 02/01/2017
 * Time: 15:54
 */
class JavaFxProviderSearchView : PresentableView(), ProviderSearchView {
    private val commonOps: JavaFxCommonOps by di()

    override val state = mutableStateFlow(GameSearchState.Null, debugName = "state")
    private val currentProviderId = state.property.typesafeStringBinding { it.currentProvider ?: "" }

    override val query = viewMutableStateFlow("", debugName = "query")
    override val searchResults = mutableStateFlow(emptyList<GameProvider.SearchResult>(), debugName = "searchResults")

    override val selectedSearchResult = viewMutableStateFlow<GameProvider.SearchResult?>(null, debugName = "selectedSearchResult")
    override val fetchSearchResultActions = broadcastFlow<GameProvider.SearchResult>()

    override val canChangeState = mutableStateFlow(IsValid.valid, debugName = "canChangeState")
    override val canSearchCurrentQuery = mutableStateFlow(IsValid.valid, debugName = "canSearchCurrentQuery")
    override val canAcceptSearchResult = mutableStateFlow(IsValid.valid, debugName = "canAcceptSearchResult")
    override val canExcludeSearchResult = mutableStateFlow(IsValid.valid, debugName = "canExcludeSearchResult")

    override val canToggleFilterPreviouslyDiscardedResults =
        mutableStateFlow(IsValid.valid, debugName = "canToggleFilterPreviouslyDiscardedResults")
    override val isFilterPreviouslyDiscardedResults = viewMutableStateFlow(false, debugName = "isFilterPreviouslyDiscardedResults")

    override val isAllowSmartChooseResults = mutableStateFlow(false, debugName = "isAllowSmartChooseResults")
    override val canSmartChooseResult = mutableStateFlow(false, debugName = "canSmartChooseResult")

    override val choiceActions = broadcastFlow<GameSearchState.ProviderSearch.Choice>()
    override val changeProviderActions = broadcastFlow<ProviderId>()

    override val canShowMoreResults = mutableStateFlow(IsValid.valid, debugName = "canShowMoreResults")
    override val showMoreResultsActions = broadcastFlow<Unit>()

    lateinit var cancelButton: JFXButton
    lateinit var skipButton: JFXButton

    private val resultsView = prettyListView(searchResults.list) {
        vgrow = Priority.ALWAYS
        minWidth = 1000.0
        enableWhen(canChangeState, wrapInErrorTooltip = false)
        prettyListCell { result ->
            text = null
            graphic = GameDetailsSummaryBuilder {
                platform = state.property.value.libraryPath.library.platform
                name = result.name
                nameOp = { maxWidth = 600.0 }
                description = result.description
                descriptionOp = { maxWidth = 600.0 }
                releaseDate = result.releaseDate
                criticScore = result.criticScore
                userScore = result.userScore
                image = commonOps.fetchImage(result.thumbnailUrl, persist = false)
                imageFitWidth = 200
            }.build()

            popoverContextMenu {
                jfxButton("Fetch", Icons.download) {
                    action(fetchSearchResultActions) { result }
                }
            }

//            val cellMinWidth = prefWidth(-1.0) + (verticalScrollbar?.width ?: 0.0) + insets.left + insets.right
//            if (cellMinWidth > this@customListView.minWidth) {
//                this@customListView.minWidth = cellMinWidth
//            }
        }

        onUserSelect(clickCount = 2) {
            if (canAcceptSearchResult.value.isSuccess) {
                choiceActions.event(GameSearchState.ProviderSearch.Choice.Accept(selectedSearchResult.v!!))
            }
        }
        searchResults.onChange {
            fade(0.6.seconds, 1.0, play = true) {
                fromValue = 0.0
            }
        }

        selectionModel.selectedItemProperty().typeSafeOnChange {
            selectedSearchResult *= it
        }
    }

    init {
        register()

        selectedSearchResult.onChange {
            resultsView.selectionModel.select(it)
        }

        state.onChange {
            cancelButton.requestFocus()
        }
    }

    override val root = vbox(spacing = 10) {
        errorTooltip(canChangeState)

        // Buttons
        prettyToolbar {
            enableWhen(canChangeState, wrapInErrorTooltip = false)
            cancelButton = cancelButton("Cancel") {
                action(choiceActions) { GameSearchState.ProviderSearch.Choice.Cancel }
            }
            spacer()
            excludeButton {
                showWhen(canExcludeSearchResult)
                textProperty().bind(currentProviderId.typesafeStringBinding { "Exclude $it" })
                tooltip(currentProviderId.typesafeStringBinding { "Exclude this path from ever syncing with $it" })
                action(choiceActions) { GameSearchState.ProviderSearch.Choice.Exclude }
            }
            gap()
            skipButton = excludeButton {
                graphic = Icons.redo
                textProperty().bind(currentProviderId.typesafeStringBinding { "Skip $it" })
                tooltip(currentProviderId.typesafeStringBinding { "Skip syncing this path with $it this time" })
                action(choiceActions) { GameSearchState.ProviderSearch.Choice.Skip }
            }
            spacer()
            acceptButton("Accept") {
                enableWhen(canAcceptSearchResult)
                action(choiceActions) { GameSearchState.ProviderSearch.Choice.Accept(selectedSearchResult.v!!) }
            }
        }

        vbox(spacing = 10) {
//            useMaxSize = true
//            vgrow = Priority.ALWAYS
            paddingAll = 10

            label(state.property.typesafeStringBinding { it.libraryPath.path.toString() }) {
                addClass(Style.pathText)
            }

            // Providers
            val progress = state.property.binding { state ->
                if (state.isFinished) 1.0 else state.progress
            }
            jfxProgressBar(progress) {
                useMaxWidth = true
            }
            hbox(spacing = 10) {
                useMaxWidth = true
                alignment = Pos.CENTER_LEFT
                state.property.typeSafeOnChange { state ->
                    val choices = state.choices.toMap()

                    replaceChildren {
                        var needSpacer = false
                        state.providerOrder.forEach { providerId ->
                            if (needSpacer) {
                                spacer()
                            }
                            needSpacer = true
                            val isCurrentProvider = state.currentProvider == providerId

                            fun providerLogoView(height: Int): Node {
                                return HBox(5.0).apply {
                                    alignment = Pos.CENTER_LEFT
                                    children += commonOps.providerLogo(providerId).toImageView(height = height) {
                                        if (!isCurrentProvider) {
                                            opacity = 0.6
                                        }
                                    }

                                    val choiceIcon = when (choices[providerId]) {
                                        is GameSearchState.ProviderSearch.Choice.Accept, is GameSearchState.ProviderSearch.Choice.Preset -> {
                                            tooltip("$providerId has an accepted result.")
                                            Icons.accept
                                        }

                                        is GameSearchState.ProviderSearch.Choice.Skip -> {
                                            tooltip("$providerId was skipped.")
                                            Icons.redo
                                        }

                                        is GameSearchState.ProviderSearch.Choice.Exclude -> {
                                            tooltip("$providerId was excluded.")
                                            Icons.warning
                                        }

                                        else -> null
                                    }
                                    if (choiceIcon != null) {
                                        children += choiceIcon.size(height)
                                    }
                                }
                            }

                            if (isCurrentProvider) {
                                children += providerLogoView(height = 100)
                            } else {
                                jfxButton {
                                    scaleOnMouseOver(duration = 0.1.seconds, target = 1.15)
                                    useMaxSize = true
                                    graphic = providerLogoView(height = 30)
                                    action(changeProviderActions) { providerId }
                                }
                            }
                        }
                    }
                }
            }

            // Search input
            defaultHbox(spacing = 10) {
                enableWhen(canChangeState, wrapInErrorTooltip = false)
                val newSearch = jfxTextField(query.property, promptText = "Enter Search Query...") {
                    addClass(Style.searchText)
                    useMaxWidth = true
                    isFocusTraversable = false
                    hgrow = Priority.ALWAYS
                }
                confirmButton("Search", Icons.search, isToolbarButton = false) {
                    minWidth = Region.USE_PREF_SIZE
                    addClass(Style.searchText)

                    val searchButtonMode = canSearchCurrentQuery.property.mapWith(canShowMoreResults.property) { canSearch, canShowMore ->
                        when {
                            canSearch.isSuccess -> SearchButtonMode.Search
                            canShowMore.isSuccess -> SearchButtonMode.ShowMore
                            else -> SearchButtonMode.Disabled
                        }
                    }

                    enableWhen(canSearchCurrentQuery.property.mapWith(canShowMoreResults.property) { canSearch, canShowMore ->
                        canShowMore or canSearch
                    })
                    textProperty().bind(searchButtonMode.typesafeStringBinding {
                        when (it) {
                            SearchButtonMode.Search -> "Search"
                            SearchButtonMode.ShowMore -> "Show More Results"
                            SearchButtonMode.Disabled -> "Search"
                            else -> kotlin.error("Invalid SearchButtonMode: $it")
                        }
                    })
                    // This becomes the new default button when the search textfield has focus.
                    defaultButtonProperty().bind(newSearch.focusedProperty())
                    prefHeightProperty().bind(newSearch.heightProperty())
                    action {
                        when (searchButtonMode.value!!) {
                            SearchButtonMode.Search -> choiceActions.event(GameSearchState.ProviderSearch.Choice.NewSearch(query.v))
                            SearchButtonMode.ShowMore -> showMoreResultsActions.event(Unit)
                            SearchButtonMode.Disabled -> kotlin.error("Cannot search or show more results!")
                        }
                    }
                }

                jfxButton {
                    showWhen { canToggleFilterPreviouslyDiscardedResults.property.typesafeBooleanBinding { it.isSuccess } }
                    graphicProperty().bind(isFilterPreviouslyDiscardedResults.property.binding { if (it) Icons.expand else Icons.collapse })
                    tooltip {
                        textProperty().bind(isFilterPreviouslyDiscardedResults.property.typesafeStringBinding { "${if (it) "Show" else "Hide"} previously discarded search results" })
                    }
                    action {
                        isFilterPreviouslyDiscardedResults.valueFromView = !isFilterPreviouslyDiscardedResults.v
                    }
                }
                header(searchResults.list.sizeProperty.typesafeStringBinding { "Results: $it" })
            }
        }
        children += resultsView
    }

    private enum class SearchButtonMode {
        Search, ShowMore, Disabled
    }

    class Style : Stylesheet() {
        companion object {
            val searchText by cssclass()
            val pathText by cssclass()

            init {
                importStylesheetSafe(Style::class)
            }
        }

        init {
            searchText {
                fontSize = 18.px
            }

            pathText {
                fontSize = 15.px
                fontWeight = FontWeight.BOLD
            }
        }
    }
}
