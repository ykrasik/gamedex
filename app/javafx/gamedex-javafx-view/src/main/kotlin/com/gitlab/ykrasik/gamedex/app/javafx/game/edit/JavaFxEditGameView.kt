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
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.image.ImageLoader
import com.gitlab.ykrasik.gamedex.app.javafx.image.JavaFxImage
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.screen.PresentableView
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.VBox
import javafx.scene.text.FontWeight
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.Deferred
import tornadofx.*

/**
 * User: ykrasik
 * Date: 13/05/2017
 * Time: 11:06
 */
// TODO: Consider allowing to delete provider data.
// TODO: Add a way to clear provider excludes.
class JavaFxEditGameView : PresentableView(), EditGameView {
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
        viewRegistry.register(this)
    }

    override val root = borderpane {
        prefWidth = screenBounds.width.let { it * 2 / 3 }
        prefHeight = screenBounds.height.let { it * 3 / 4 }
        top {
            toolbar {
                acceptButton {
                    isDefaultButton = true
                    eventOnAction(acceptActions)
                }
                verticalSeparator()
                spacer()
                verticalSeparator()
                resetToDefaultButton { eventOnAction(clearActions) }
                verticalSeparator()
                cancelButton {
                    isCancelButton = true
                    eventOnAction(cancelActions)
                }
            }
        }
        center {
            tabPane = jfxTabPane {
                addClass(CommonStyle.hiddenTabPaneHeader)
                paddingRight = 10.0
                initialScreenProperty.onChange { type ->
                    navigationToggle.selectToggle(navigationToggle.toggles.find { (it.userData as Tab).userData as GameDataType == type })
                }
            }
        }
        left {
            hbox(spacing = 5.0) {
                paddingAll = 5.0
                mainContent()
                verticalSeparator(padding = 0.0)
            }
        }
    }

    private fun EventTarget.mainContent() = vbox(spacing = 5.0) {
        label("Attributes") { addClass(Style.navigationLabel) }
        separator()

        entry(
            GameDataType.name_,
            nameOverrideProperty,
            providerDataExtractor = GameData::name,
            gameDataExtractor = Game::name,
            dataDisplay = ::displayText
        )
        entry(
            GameDataType.description,
            descriptionOverrideProperty,
            providerDataExtractor = GameData::description,
            gameDataExtractor = Game::description,
            dataDisplay = displayWrappedText(maxWidth = screenBounds.width / 2)
        )
        entry(
            GameDataType.releaseDate,
            releaseDateOverrideProperty,
            providerDataExtractor = GameData::releaseDate,
            gameDataExtractor = Game::releaseDate,
            dataDisplay = ::displayText
        )
        entry(
            GameDataType.criticScore,
            criticScoreOverrideProperty,
            providerDataExtractor = GameData::criticScore,
            gameDataExtractor = Game::criticScore,
            dataDisplay = ::displayScore
        )
        entry(
            GameDataType.userScore,
            userScoreOverrideProperty,
            providerDataExtractor = GameData::userScore,
            gameDataExtractor = Game::userScore,
            dataDisplay = ::displayScore
        )
        entry(
            GameDataType.thumbnail,
            thumbnailUrlOverrideProperty,
            providerDataExtractor = GameData::thumbnailUrl,
            gameDataExtractor = Game::thumbnailUrl,
            dataDisplay = ::imageDisplay
        )
        entry(
            GameDataType.poster,
            posterUrlOverrideProperty,
            providerDataExtractor = GameData::posterUrl,
            gameDataExtractor = Game::posterUrl,
            dataDisplay = ::imageDisplay
        )
    }

    private fun <T : Any> VBox.entry(type: GameDataType,
                                     overrideViewModelProperty: ObjectProperty<GameDataOverrideViewModel<T>>,
                                     providerDataExtractor: (GameData) -> T?,
                                     gameDataExtractor: (Game) -> T?,
                                     dataDisplay: (EventTarget, T) -> Any) {
        jfxToggleNode(type.displayName, group = navigationToggle) {
            useMaxWidth = true
            tabPane.tab(type.displayName) {
                this@jfxToggleNode.userData = this
                userData = type
                val toggleGroup = ToggleGroup().apply { disallowDeselection() }
                scrollpane(fitToWidth = false) {
                    vbox(spacing = 10.0) {
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
                                                graphic = hbox(spacing = 10.0) {
                                                    alignment = Pos.CENTER_LEFT
                                                    paddingAll = 10.0
                                                    children += logo(providerData.header.id).toImageView(height = 120.0, width = 100.0)
                                                    dataDisplay(this@hbox, data)
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

                                graphic = hbox(spacing = 10.0) {
                                    alignment = Pos.CENTER_LEFT
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
                            buttonWithPopover(graphic = Theme.Icon.edit(), closeOnClick = false, styleClass = null) { popOver ->
                                hbox(spacing = 5.0) {
                                    alignment = Pos.CENTER_LEFT
                                    val viewModel = CustomTextViewModel(type)
                                    overrideViewModelProperty.onChange {
                                        viewModel.textProperty.value = it!!.rawCustomValue
                                    }
                                    textfield(viewModel.textProperty) {
                                        minWidth = 300.0
                                        promptText = "Custom ${type.displayName}"
                                        validatorFrom(viewModel, overrideViewModelProperty.map { it!!.customValueValidationError })
                                    }
                                    acceptButton {
                                        isDefaultButton = true
                                        enableWhen { viewModel.valid }
                                        setOnAction {
                                            popOver.hide()
                                            customOverrideValueAcceptActions.event(type)
                                            customToggleNode.isSelected = true
                                        }
                                    }
                                    cancelButton {
                                        isCancelButton = true
                                        eventOnAction(customOverrideValueRejectActions) {
                                            popOver.hide()
                                            type
                                        }
                                    }
                                }
                            }.apply { useMaxHeight = true }
                        }

                        // Reset button
                        jfxToggleNode(group = toggleGroup) {
                            useMaxWidth = true
                            graphic = hbox(spacing = 10.0) {
                                alignment = Pos.CENTER_LEFT
                                paddingAll = 10.0
                                children += Theme.Icon.timesCircle(40.0)
                                text("Reset to default") { addClass(Style.textData) }
                            }
                            selectedProperty().eventOnChange(clearOverrideSelectionChanges) { type to it }
                        }
                    }
                }
            }
        }
        separator()
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

    private fun logo(id: ProviderId) = (providerLogos[id]!! as JavaFxImage).image

    private inner class CustomTextViewModel(type: GameDataType) : ViewModel() {
        val textProperty = presentableStringProperty(customOverrideValueChanges) { type to it }
    }

    class Style : Stylesheet() {
        companion object {
            val textData by cssclass()
            val navigationLabel by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            textData {
                fontSize = 18.px
            }

            navigationLabel {
                fontSize = 14.px
                fontWeight = FontWeight.BOLD
            }
        }
    }
}