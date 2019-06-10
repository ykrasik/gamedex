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
import com.gitlab.ykrasik.gamedex.GameDataType
import com.gitlab.ykrasik.gamedex.Score
import com.gitlab.ykrasik.gamedex.app.api.game.EditGameView
import com.gitlab.ykrasik.gamedex.app.api.game.GameDataOverrideState
import com.gitlab.ykrasik.gamedex.app.api.game.OverrideSelectionType
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.common.JavaFxCommonOps
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.theme.*
import com.gitlab.ykrasik.gamedex.javafx.view.ConfirmationWindow
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.jfoenix.controls.JFXTabPane
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Tab
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import tornadofx.*

/**
 * User: ykrasik
 * Date: 13/05/2017
 * Time: 11:06
 */
// TODO: Consider allowing to delete provider data.
// TODO: Add a way to clear provider excludes.
class JavaFxEditGameView : ConfirmationWindow(icon = Icons.edit), EditGameView {
    private val commonOps: JavaFxCommonOps by di()

    private val initialViewProperty = SimpleObjectProperty(GameDataType.Name)
    var initialView: GameDataType by initialViewProperty

    private val gameProperty = SimpleObjectProperty(Game.Null)
    override var game by gameProperty

    // TODO: Consider representing this as a CustomProvider in the UserData
    override val nameOverride = JavaFxGameDataOverrideState<String>(GameDataType.Name, Icons.text)
    override val descriptionOverride = JavaFxGameDataOverrideState<String>(GameDataType.Description, Icons.textbox)
    override val releaseDateOverride = JavaFxGameDataOverrideState<String>(GameDataType.ReleaseDate, Icons.date)
    override val criticScoreOverride = JavaFxGameDataOverrideState<Score>(GameDataType.CriticScore, Icons.starFull)
    override val userScoreOverride = JavaFxGameDataOverrideState<Score>(GameDataType.UserScore, Icons.starEmpty)
    override val thumbnailUrlOverride = JavaFxGameDataOverrideState<String>(GameDataType.Thumbnail, Icons.thumbnail)
    override val posterUrlOverride = JavaFxGameDataOverrideState<String>(GameDataType.Poster, Icons.poster)

    override val resetAllToDefaultActions = channel<Unit>()

    private val tabPane: JFXTabPane = jfxTabPane {
        addClass(GameDexStyle.hiddenTabPaneHeader)
        paddingAll = 5
        initialViewProperty.onChange { type ->
            navigationToggle.selectToggle(navigationToggle.toggles.find { (it.userData as Tab).userData as GameDataType == type })
        }
    }

    private val navigationToggle = ToggleGroup().apply {
        disallowDeselection()
        selectedToggleProperty().onChange {
            tabPane.selectionModel.select(it!!.userData as Tab)
        }
    }

    init {
        titleProperty.bind(gameProperty.stringBinding { "Edit '${it!!.name}'" })
        register()
    }

    override val root = borderpane {
        top = confirmationToolbar {
            gap()
            centeredWindowHeader {
                resetToDefaultButton("Reset all to Default") {
                    action(resetAllToDefaultActions)
                    stackpaneConstraints { alignment = Pos.CENTER_LEFT }
                }
            }
            gap()
        }
        left = vbox(spacing = 5) {
            paddingAll = 5
            paddingTop = 15
            header("Properties")

            entry(nameOverride, "") { displayText(it) }
            entry(descriptionOverride, "") { displayText(it, maxWidth = screenBounds.width / 2) }
            entry(releaseDateOverride, "") { displayText(it) }
            entry(criticScoreOverride, Score(0.0, 0)) { displayScore(it) }
            entry(userScoreOverride, Score(0.0, 0)) { displayScore(it) }
            entry(thumbnailUrlOverride, "") { imageDisplay(it) }
            entry(posterUrlOverride, "") { imageDisplay(it) }
        }
        center = tabPane
    }

    private fun <T : Any> VBox.entry(
        state: JavaFxGameDataOverrideState<T>,
        defaultValue: T,
        dataDisplay: EventTarget.(ObservableValue<T>) -> Node
    ) = jfxToggleNode(state.type.displayName, graphic = state.icon, group = navigationToggle) {
        useMaxWidth = true
        tabPane.tab(state.type.displayName) {
            this@jfxToggleNode.userData = this
            userData = state.type
            val toggleGroup = togglegroup {
                disallowDeselection()
                bind(state.selection.property)
            }
            scrollpane(fitToWidth = false) {
                paddingAll = 10
                vbox(spacing = 10) {
                    defaultHbox {
                        header(state.type.displayName) {
                            style {
                                fontSize = 24.px
                            }
                        }
                        spacer()
                        resetToDefaultButton {
                            alignment = Pos.CENTER_LEFT
                            addClass(Style.textData)
                            action(state.resetToDefaultActions)
                        }
                    }

                    // Custom
                    defaultHbox {
                        errorTooltip(state.canSelectCustomOverride.errorText())
                        jfxToggleNode(group = toggleGroup) {
                            useMaxWidth = true
                            hgrow = Priority.ALWAYS
                            enableWhen(state.canSelectCustomOverride, wrapInErrorTooltip = false)

                            graphic = defaultHbox {
                                paddingAll = 10
                                text("Custom") { addClass(Style.textData) }
                                gap(size = 40)
                                dataDisplay(state.customValue.property.binding { it ?: defaultValue })
                            }
                            toggleValue(OverrideSelectionType.Custom)
                        }
                        buttonWithPopover(graphic = Icons.enterText, closeOnAction = false) {
                            defaultHbox {
                                jfxTextField(state.rawCustomValue.property, promptText = "Enter ${state.type}...") {
                                    minWidth = 300.0
                                    validWhen(state.isCustomValueValid)
                                }
                                cancelButton(isToolbarButton = false) {
                                    action(state.customValueRejectActions) {
                                        popOver.hide()
                                    }
                                }
                                acceptButton(isToolbarButton = false) {
                                    enableWhen(state.isCustomValueValid)
                                    action {
                                        popOver.hide()
                                        state.customValueAcceptActions.event(Unit)
                                    }
                                }
                            }
                        }.apply { useMaxHeight = true }
                    }

                    // Existing provider data
                    commonOps.providerLogos.forEach { (providerId, logo) ->
                        val providerValueProperty = state.providerValues.property.binding { it[providerId] }
                        jfxToggleNode(group = toggleGroup) {
                            useMaxWidth = true
                            visibleWhen { providerValueProperty.isNotNull }
                            graphic = defaultHbox(spacing = 10) {
                                paddingAll = 10
                                children += logo.toImageView(height = 120, width = 100)
                                dataDisplay(providerValueProperty.map { it ?: defaultValue })
                            }
                            toggleValue(OverrideSelectionType.Provider(providerId))
                        }
                    }
                }
            }
        }
    }

    private fun EventTarget.displayScore(score: ObservableValue<Score>) =
        displayText(score.stringBinding { "${it?.score} Based on ${it?.numReviews} reviews." })

    private fun EventTarget.displayText(text: ObservableValue<String>, maxWidth: Double? = null) = text(text) {
        addClass(Style.textData)
        maxWidth?.let { wrappingWidth = it }
    }

    private fun EventTarget.imageDisplay(url: ObservableValue<String>) = imageview {
        fitHeight = 300.0       // TODO: Config?
        fitWidth = 200.0
        isPreserveRatio = true
        imageProperty().bind(url.flatMap { url ->
            commonOps.fetchImage(if (url.isNotEmpty()) url else null, persist = false)
        })
    }

    class JavaFxGameDataOverrideState<T>(override val type: GameDataType, val icon: Node) : GameDataOverrideState<T> {
        override val selection = userMutableState<OverrideSelectionType?>(null)
        override val customValue = state<T?>(null)
        override val providerValues = state<Map<ProviderId, T>>(emptyMap())
        override val canSelectCustomOverride = state(IsValid.valid)
        override val rawCustomValue = userMutableState("")
        override val isCustomValueValid = state(IsValid.valid)
        override val customValueAcceptActions = channel<Unit>()
        override val customValueRejectActions = channel<Unit>()
        override val resetToDefaultActions = channel<Unit>()
    }

    class Style : Stylesheet() {
        companion object {
            val textData by cssclass()

            init {
                importStylesheetSafe(Style::class)
            }
        }

        init {
            textData {
                fontSize = 18.px
            }
        }
    }
}