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

package com.gitlab.ykrasik.gamedex.app.javafx.game.edit

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.app.api.game.EditGameView
import com.gitlab.ykrasik.gamedex.app.api.game.FetchThumbnailRequest
import com.gitlab.ykrasik.gamedex.app.api.game.GameDataOverrideViewModel
import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.app.api.image.ViewWithProviderLogos
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.image.ImageLoader
import com.gitlab.ykrasik.gamedex.app.javafx.image.image
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.VBox
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import tornadofx.*

/**
 * User: ykrasik
 * Date: 13/05/2017
 * Time: 11:06
 */
// TODO: Consider allowing to delete provider data.
// TODO: Add a way to clear provider excludes.
class JavaFxEditGameView : PresentableView(), EditGameView, ViewWithProviderLogos {
    private val imageLoader: ImageLoader by di()

    private var tabPane: TabPane by singleAssign()

    override var providerLogos = emptyMap<ProviderId, Image>()

    private val initialScreenProperty = SimpleObjectProperty(GameDataType.name_)
    override var initialScreen by initialScreenProperty

    private val gameProperty = SimpleObjectProperty<Game>()
    override var game by gameProperty

    override val fetchThumbnailRequests = channel<FetchThumbnailRequest>()

    // TODO: Consider representing this as a CustomProvider in the UserData
    private val nameOverrideProperty = overrideProperty<String>()
    override var nameOverride by nameOverrideProperty

    private val descriptionOverrideProperty = overrideProperty<String>()
    override var descriptionOverride by descriptionOverrideProperty

    private val releaseDateOverrideProperty = overrideProperty<String>()
    override var releaseDateOverride by releaseDateOverrideProperty

    private val userScoreOverrideProperty = overrideProperty<Score>()
    override var userScoreOverride by userScoreOverrideProperty

    private val criticScoreOverrideProperty = overrideProperty<Score>()
    override var criticScoreOverride by criticScoreOverrideProperty

    private val thumbnailUrlOverrideProperty = overrideProperty<String>()
    override var thumbnailUrlOverride by thumbnailUrlOverrideProperty

    private val posterUrlOverrideProperty = overrideProperty<String>()
    override var posterUrlOverride by posterUrlOverrideProperty

    private fun <T> overrideProperty() =
        SimpleObjectProperty<GameDataOverrideViewModel<T>>(GameDataOverrideViewModel())

    override val providerOverrideSelectionChanges = channel<Triple<GameDataType, ProviderId, Boolean>>()
    override val customOverrideSelectionChanges = channel<Pair<GameDataType, Boolean>>()
    override val clearOverrideSelectionChanges = channel<Pair<GameDataType, Boolean>>()

    override val customOverrideValueChanges = channel<Pair<GameDataType, String>>()
    override val customOverrideValueAcceptActions = channel<GameDataType>()
    override val customOverrideValueRejectActions = channel<GameDataType>()

    override val acceptActions = channel<Unit>()
    override val clearActions = channel<Unit>()
    override val cancelActions = channel<Unit>()

    private val navigationToggle = ToggleGroup().apply {
        disallowDeselection()
        selectedToggleProperty().onChange {
            tabPane.selectionModel.select(it!!.userData as Tab)
        }
    }

    init {
        titleProperty.bind(gameProperty.stringBinding { it?.name })
        viewRegistry.onCreate(this)
    }

    override val root = borderpane {
        prefWidth = screenBounds.width.let { it * 2 / 3 }
        prefHeight = screenBounds.height.let { it * 3 / 4 }
        top {
            toolbar {
                cancelButton { eventOnAction(cancelActions) }
                gap()
                resetToDefaultButton { eventOnAction(clearActions) }
                spacer()
                acceptButton { eventOnAction(acceptActions) }
            }
        }
        center {
            tabPane = jfxTabPane {
                addClass(CommonStyle.hiddenTabPaneHeader)
                paddingAll = 5.0
                initialScreenProperty.onChange { type ->
                    navigationToggle.selectToggle(navigationToggle.toggles.find { (it.userData as Tab).userData as GameDataType == type })
                }
            }
        }
        left {
            mainContent()
        }
    }

    private fun EventTarget.mainContent() = vbox(spacing = 5) {
        paddingAll = 5.0
        header("Properties")

        entry(
            GameDataType.name_,
            nameOverrideProperty,
            icon = Icons.text,
            providerDataExtractor = GameData::name,
            gameDataExtractor = Game::name,
            dataDisplay = ::displayText
        )
        entry(
            GameDataType.description,
            descriptionOverrideProperty,
            icon = Icons.textbox,
            providerDataExtractor = GameData::description,
            gameDataExtractor = Game::description,
            dataDisplay = displayWrappedText(maxWidth = screenBounds.width / 2)
        )
        entry(
            GameDataType.releaseDate,
            releaseDateOverrideProperty,
            icon = Icons.date,
            providerDataExtractor = GameData::releaseDate,
            gameDataExtractor = Game::releaseDate,
            dataDisplay = ::displayText
        )
        entry(
            GameDataType.criticScore,
            criticScoreOverrideProperty,
            icon = Icons.starFull,
            providerDataExtractor = GameData::criticScore,
            gameDataExtractor = Game::criticScore,
            dataDisplay = ::displayScore
        )
        entry(
            GameDataType.userScore,
            userScoreOverrideProperty,
            icon = Icons.starEmpty,
            providerDataExtractor = GameData::userScore,
            gameDataExtractor = Game::userScore,
            dataDisplay = ::displayScore
        )
        entry(
            GameDataType.thumbnail,
            thumbnailUrlOverrideProperty,
            icon = Icons.thumbnail,
            providerDataExtractor = GameData::thumbnailUrl,
            gameDataExtractor = Game::thumbnailUrl,
            dataDisplay = ::imageDisplay
        )
        entry(
            GameDataType.poster,
            posterUrlOverrideProperty,
            icon = Icons.poster,
            providerDataExtractor = GameData::posterUrl,
            gameDataExtractor = Game::posterUrl,
            dataDisplay = ::imageDisplay
        )
    }

    private fun <T : Any> VBox.entry(
        type: GameDataType,
        overrideViewModelProperty: ObjectProperty<GameDataOverrideViewModel<T>>,
        icon: Node,
        providerDataExtractor: (GameData) -> T?,
        gameDataExtractor: (Game) -> T?,
        dataDisplay: (EventTarget, T) -> Any
    ) {
        jfxToggleNode(type.displayName, graphic = icon, group = navigationToggle) {
            useMaxWidth = true
            tabPane.tab(type.displayName) {
                this@jfxToggleNode.userData = this
                userData = type
                val toggleGroup = ToggleGroup().disallowDeselection()
                scrollpane(fitToWidth = false) {
                    paddingAll = 0
                    vbox(spacing = 10.0) {
                        header(type.displayName) {
                            style {
                                fontSize = 24.px
                            }
                        }

                        // Existing provider data
                        vbox(spacing = 10.0) {
                            gameProperty.onChange { game ->
                                replaceChildren {
                                    game!!.rawGame.providerData.sortedBy { it.header.id }.forEach { providerData ->
                                        providerDataExtractor(providerData.gameData)?.let { data ->
                                            jfxToggleNode(group = toggleGroup) {
                                                useMaxWidth = true
                                                isSelected = gameDataExtractor(game) == data
                                                selectedProperty().eventOnChange(providerOverrideSelectionChanges) {
                                                    Triple(type, providerData.header.id, it)
                                                }
                                                graphic = defaultHbox {
                                                    paddingAll = 10.0
                                                    children += logo(providerData.header.id).toImageView(height = 120.0, width = 100.0)
                                                    dataDisplay(this, data)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Custom
                        hbox {
                            val customToggleNode = jfxToggleNode(group = toggleGroup) {
                                useMaxWidth = true
                                enableWhen { overrideViewModelProperty.map { it!!.customValue }.isNotNull }

                                overrideViewModelProperty.perform {
                                    isSelected = it.override != null && it.override is GameDataOverride.Custom
                                }

                                selectedProperty().eventOnChange(customOverrideSelectionChanges) { type to it }

                                graphic = defaultHbox {
                                    paddingAll = 10.0

                                    overrideViewModelProperty.map { it!!.customValue }.perform { customValue ->
                                        replaceChildren {
                                            text("Custom") { addClass(Style.textData) }
                                            if (customValue != null) {
                                                dataDisplay(this, customValue)
                                            }
                                        }
                                    }
                                }
                            }
                            spacer()
                            buttonWithPopover(graphic = Icons.enterText, closeOnClick = false) { popOver ->
                                defaultHbox {
                                    val customValueText = SimpleStringProperty("").eventOnChange(customOverrideValueChanges) { type to it }
                                    overrideViewModelProperty.onChange {
                                        customValueText.value = it!!.rawCustomValue
                                    }
                                    val isValid = overrideViewModelProperty.map { it!!.isCustomValueValid }
                                    jfxTextField(customValueText, promptText = "Custom ${type.displayName}") {
                                        minWidth = 300.0
                                        validWhen(isValid)
                                    }
                                    cancelButton {
                                        removeClass(CommonStyle.toolbarButton)
                                        eventOnAction(customOverrideValueRejectActions) {
                                            popOver.hide()
                                            type
                                        }
                                    }
                                    acceptButton {
                                        removeClass(CommonStyle.toolbarButton)
                                        enableWhen(isValid)
                                        action {
                                            popOver.hide()
                                            customOverrideValueAcceptActions.event(type)
                                            customToggleNode.isSelected = true
                                        }
                                    }
                                }
                            }.apply { useMaxHeight = true }
                        }

                        // Reset button
                        jfxToggleNode(group = toggleGroup) {
                            useMaxWidth = true
                            graphic = defaultHbox {
                                paddingAll = 10.0
                                children += Icons.resetToDefault
                                text("Reset to Default") { addClass(Style.textData) }
                            }
                            selectedProperty().eventOnChange(clearOverrideSelectionChanges) { type to it }
                        }
                    }
                }
            }
        }
    }

    private fun displayScore(target: EventTarget, score: Score) =
        displayText(target, "${score.score} Based on ${score.numReviews} reviews.")

    private fun displayWrappedText(maxWidth: Double) = { target: EventTarget, text: String ->
        displayText0(target, text, maxWidth)
    }

    private fun displayText(target: EventTarget, text: String) = displayText0(target, text, maxWidth = null)
    private fun displayText0(target: EventTarget, text: String, maxWidth: Double? = null) = target.text(text) {
        addClass(Style.textData)
        maxWidth?.let { wrappingWidth = it }
    }

    private fun imageDisplay(target: EventTarget, url: String) = target.imageview {
        fitHeight = 300.0       // TODO: Config?
        fitWidth = 200.0
        isPreserveRatio = true
        imageProperty().bind(imageLoader.loadImage {
            val response = CompletableDeferred<Deferred<com.gitlab.ykrasik.gamedex.app.api.image.Image>>()
            fetchThumbnailRequests.offer(FetchThumbnailRequest(url, response))
            response
        })
    }

    private fun logo(id: ProviderId) = providerLogos[id]!!.image

    private inner class CustomTextViewModel(type: GameDataType) : ViewModel() {
        val textProperty = presentableStringProperty(customOverrideValueChanges) { type to it }
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