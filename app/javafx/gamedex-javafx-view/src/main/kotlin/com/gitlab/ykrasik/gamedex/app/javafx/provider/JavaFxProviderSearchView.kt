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

package com.gitlab.ykrasik.gamedex.app.javafx.provider

import com.gitlab.ykrasik.gamedex.app.api.provider.GameSearchState
import com.gitlab.ykrasik.gamedex.app.api.provider.ProviderSearchChoice
import com.gitlab.ykrasik.gamedex.app.api.provider.ProviderSearchView
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.common.JavaFxCommonOps
import com.gitlab.ykrasik.gamedex.app.javafx.game.details.GameDetailsPaneBuilder
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.theme.*
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.util.IsValid
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import tornadofx.*

/**
 * User: ykrasik
 * Date: 02/01/2017
 * Time: 15:54
 */
class JavaFxProviderSearchView : PresentableView(), ProviderSearchView {
    private val commonOps: JavaFxCommonOps by di()

    override val state = state(GameSearchState.Null)
    private val currentProviderId = state.property.stringBinding { it!!.currentProvider ?: "" }

    override val query = userMutableState("")
    override val searchResults = mutableListOf<ProviderSearchResult>().observable()

    override val selectedSearchResult = userMutableState<ProviderSearchResult?>(null)

    override val canChangeState = state(IsValid.valid)
    override val canSearchCurrentQuery = state(IsValid.valid)
    override val canAcceptSearchResult = state(IsValid.valid)
    override val canExcludeSearchResult = state(IsValid.valid)

    override val canToggleFilterPreviouslyDiscardedResults = state(IsValid.valid)
    override val isFilterPreviouslyDiscardedResults = userMutableState(false)

    override val isAllowSmartChooseResults = state(false)
    override val canSmartChooseResult = state(false)

    override val choiceActions = channel<ProviderSearchChoice>()
    override val changeProviderActions = channel<ProviderId>()

    private val resultsView = prettyListView(searchResults) {
//        useMaxWidth = true
//        hgrow = Priority.ALWAYS
        vgrow = Priority.ALWAYS
        enableWhen(canChangeState, wrapInErrorTooltip = false)
//        maxWidth = screenBounds.width / 2
        prettyListCell { result ->
            maxWidth = 800.0
            text = null
            graphic = GameDetailsPaneBuilder(
                name = result.name,
                description = result.description,
                releaseDate = result.releaseDate,
                criticScore = result.criticScore,
                userScore = result.userScore,
                image = commonOps.downloadImage(result.thumbnailUrl)
            ).build()

//            val cellMinWidth = prefWidth(-1.0) + (verticalScrollbar?.width ?: 0.0) + insets.left + insets.right
//            if (cellMinWidth > this@customListView.minWidth) {
//                this@customListView.minWidth = cellMinWidth
//            }
        }

        onUserSelect(clickCount = 2) {
            if (canAcceptSearchResult.value.isSuccess) {
                choiceActions.event(ProviderSearchChoice.Accept(selectedSearchResult.value!!))
            }
        }
        searchResults.onChange {
            fade(0.6.seconds, 1.0, play = true) {
                fromValue = 0.0
            }
        }

        selectionModel.selectedItemProperty().onChange {
            selectedSearchResult.valueFromView = it
        }
    }

    init {
        register()

        selectedSearchResult.onChange {
            resultsView.selectionModel.select(it)
        }
    }

    override val root = vbox(spacing = 10) {
        errorTooltip(canChangeState)

        // Buttons
        customToolbar {
            enableWhen(canChangeState, wrapInErrorTooltip = false)
            cancelButton("Cancel") {
                action(choiceActions) { ProviderSearchChoice.Cancel }
            }
            spacer()
            excludeButton {
                showWhen(canExcludeSearchResult)
                textProperty().bind(currentProviderId.stringBinding { "Exclude $it" })
                tooltip(currentProviderId.stringBinding { "Exclude this path from ever syncing with $it" })
                action(choiceActions) { ProviderSearchChoice.Exclude }
            }
            gap()
            excludeButton {
                graphic = Icons.redo
                textProperty().bind(currentProviderId.stringBinding { "Skip $it" })
                tooltip(currentProviderId.stringBinding { "Skip syncing this path with $it this time" })
                action(choiceActions) { ProviderSearchChoice.Skip }
            }
            spacer()
            acceptButton("Accept") {
                enableWhen(canAcceptSearchResult)
                action(choiceActions) { ProviderSearchChoice.Accept(selectedSearchResult.value!!) }
            }
        }

        vbox(spacing = 10) {
//            useMaxSize = true
//            vgrow = Priority.ALWAYS
            paddingAll = 10

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

                                    val choice = choices[providerId]
                                    val choiceIcon = when (choice) {
                                        is ProviderSearchChoice.Accept -> {
                                            tooltip("$providerId has an accepted result.")
                                            Icons.accept
                                        }
                                        is ProviderSearchChoice.Skip -> {
                                            tooltip("$providerId was skipped from syncing this path.")
                                            Icons.redo
                                        }
                                        is ProviderSearchChoice.Exclude -> {
                                            tooltip("$providerId is excluded from syncing this path.")
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
                                    addClass(GameDexStyle.hoverable)
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
                header("Search:")
                val newSearch = jfxTextField(query.property, promptText = "Enter Search Query...") {
                    addClass(Style.searchText)
                    useMaxWidth = true
                    isFocusTraversable = false
                    hgrow = Priority.ALWAYS
                }
                confirmButton(graphic = Icons.search) {
                    removeClass(GameDexStyle.toolbarButton)
                    minWidth = Region.USE_PREF_SIZE
                    enableWhen(canSearchCurrentQuery)
                    // This becomes the new default button when the search textfield has focus.
                    defaultButtonProperty().bind(newSearch.focusedProperty())
                    prefHeightProperty().bind(newSearch.heightProperty())
                    action(choiceActions) { ProviderSearchChoice.NewSearch(query.value) }
                }

                jfxButton {
                    showWhen { canToggleFilterPreviouslyDiscardedResults.property.booleanBinding { it!!.isSuccess } }
                    graphicProperty().bind(isFilterPreviouslyDiscardedResults.property.binding { if (it) Icons.unfoldMore else Icons.unfoldLess })
                    tooltip {
                        textProperty().bind(isFilterPreviouslyDiscardedResults.property.stringBinding { "${if (it!!) "Show" else "Hide"} previously discarded search results" })
                    }
                    setOnAction {
                        isFilterPreviouslyDiscardedResults.valueFromView = !isFilterPreviouslyDiscardedResults.valueFromView
                    }
                }
                header(searchResults.sizeProperty.stringBinding { "Results: $it" })
            }
        }
        children += resultsView
    }

    class Style : Stylesheet() {
        companion object {
            val searchText by cssclass()
            val searchResults by cssclass()
            val searchResult by cssclass()
            val descriptionText by cssclass()

            init {
                importStylesheetSafe(Style::class)
            }
        }

        init {
            searchText {
                fontSize = 18.px
            }

            searchResults {
            }

            searchResult {
//                maxWidth = 500.px
//                padding = box(5.px)
//                backgroundColor = multi(Colors.cloudyKnoxville)
//                backgroundRadius = multi(box(7.px))
            }

            descriptionText {
                wrapText = true
                maxWidth = 600.px
            }
        }
    }
}