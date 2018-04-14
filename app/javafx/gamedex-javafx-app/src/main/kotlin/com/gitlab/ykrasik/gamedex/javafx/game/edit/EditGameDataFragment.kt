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

package com.gitlab.ykrasik.gamedex.javafx.game.edit

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.core.api.provider.GameProviderRepository
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.image.JavaFxImageRepository
import com.gitlab.ykrasik.gamedex.javafx.provider.logoImage
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.VBox
import javafx.scene.text.FontWeight
import tornadofx.*
import java.net.URL
import java.time.LocalDate

/**
 * User: ykrasik
 * Date: 13/05/2017
 * Time: 11:06
 */
// TODO: Consider allowing to delete provider data.
// TODO: Add a way to clear provider excludes.
class EditGameDataFragment(private val game: Game, private val initialTab: GameDataType) : Fragment(game.name) {
    private val providerRepository: GameProviderRepository by di()
    private val imageRepository: JavaFxImageRepository by di()

    private var tabPane: TabPane by singleAssign()

    private val navigationToggle = ToggleGroup().apply {
        disallowDeselection()
        // TODO: Move selection stuff to the end.
        selectedToggleProperty().onChange {
            tabPane.selectionModel.select(it!!.userData as Tab)
        }
    }

    // TODO: Play around with representing this as a CustomProvider in the UserData
    private var overrides = HashMap(game.rawGame.userData?.overrides ?: emptyMap())

    private var choice: Choice = Choice.Cancel

    override val root = borderpane {
        prefWidth = screenBounds.width.let { it * 2 / 3 }
        prefHeight = screenBounds.height.let { it * 3 / 4 }
        top {
            toolbar {
                acceptButton {
                    isDefaultButton = true
                    setOnAction { close(choice = Choice.Override(overrides)) }
                }
                verticalSeparator()
                spacer()
                verticalSeparator()
                toolbarButton(graphic = Theme.Icon.clear()) {
                    tooltip("Reset all to default")
                    setOnAction { close(choice = Choice.Clear) }
                }
                verticalSeparator()
                cancelButton {
                    isCancelButton = true
                    setOnAction { close(choice = Choice.Cancel) }
                }
            }
        }
        center {
            tabPane = tabpane {
                addClass(CommonStyle.hiddenTabPaneHeader)
                paddingRight = 10.0
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
            providerDataExtractor = { it.gameData.name },
            gameDataExtractor = Game::name,
            dataDisplay = ::displayText,
            deserializer = ::asText
        )
        entry(
            GameDataType.description,
            providerDataExtractor = { it.gameData.description },
            gameDataExtractor = Game::description,
            dataDisplay = displayWrappedText(maxWidth = screenBounds.width / 2),
            deserializer = ::asText
        )
        entry(
            GameDataType.releaseDate,
            providerDataExtractor = { it.gameData.releaseDate },
            gameDataExtractor = Game::releaseDate,
            dataDisplay = ::displayText,
            deserializer = ::deserializeDate
        )
        entry(
            GameDataType.criticScore,
            providerDataExtractor = { it.gameData.criticScore },
            gameDataExtractor = Game::criticScore,
            dataDisplay = ::displayScore,
            deserializer = ::deserializeScore,
            serializer = ::serializeScore
        )
        entry(
            GameDataType.userScore,
            providerDataExtractor = { it.gameData.userScore },
            gameDataExtractor = Game::userScore,
            dataDisplay = ::displayScore,
            deserializer = ::deserializeScore,
            serializer = ::serializeScore
        )
        entry(
            GameDataType.thumbnail,
            providerDataExtractor = { it.gameData.imageUrls.thumbnailUrl },
            gameDataExtractor = Game::thumbnailUrl,
            dataDisplay = ::imageDisplay,
            deserializer = ::deserializeUrl
        )
        entry(
            GameDataType.poster,
            providerDataExtractor = { it.gameData.imageUrls.posterUrl },
            gameDataExtractor = Game::posterUrl,
            dataDisplay = ::imageDisplay,
            deserializer = ::deserializeUrl
        )
    }

    private fun <T : Any> VBox.entry(type: GameDataType,
                                     providerDataExtractor: (ProviderData) -> T?,
                                     gameDataExtractor: (Game) -> T?,
                                     dataDisplay: (EventTarget, T) -> Any,
                                     deserializer: (String) -> T,
                                     serializer: (T) -> Any = Any::toString) {
        jfxToggleNode(type.displayName, group = navigationToggle) {
            useMaxWidth = true
            tabPane.tab(type.displayName) {
                this@jfxToggleNode.userData = this
                if (type == initialTab) {
                    this@jfxToggleNode.isSelected = true
                }

                val toggleGroup = ToggleGroup().apply { disallowDeselection() }
                scrollpane(fitToWidth = false) {
                    vbox(spacing = 10.0) {
                        // Existing provider data
                        game.rawGame.providerData.forEach { providerData ->
                            providerDataExtractor(providerData)?.let { data ->
                                jfxToggleNode(group = toggleGroup) {
                                    useMaxWidth = true
                                    if (gameDataExtractor(game) == data) {
                                        isSelected = true
                                    }
                                    userData = SingleChoice.Override(GameDataOverride.Provider(providerData.header.id))
                                    graphic = hbox(spacing = 10.0) {
                                        alignment = Pos.CENTER_LEFT
                                        paddingAll = 10.0
                                        children += providerRepository.provider(providerData.header.id).logoImage.toImageView(height = 120.0, width = 100.0)
                                        dataDisplay(this@hbox, data)
                                    }
                                }
                            }
                        }

                        // Custom
                        hbox {
                            val customRawDataProperty = SimpleStringProperty()
                            val viewModel = CustomTextViewModel(customRawDataProperty)
                            val customDataProperty = customRawDataProperty.map { it?.let(deserializer) }

                            jfxToggleNode(group = toggleGroup) {
                                useMaxWidth = true
                                disableWhen { customDataProperty.isNull }

                                val currentOverride = game.userData?.overrides?.get(type)
                                if (currentOverride is GameDataOverride.Custom) {
                                    isSelected = true
                                    customDataProperty.value = deserializer(currentOverride.value.toString())
                                }

                                customDataProperty.onChange { customValue ->
                                    userData = SingleChoice.Override(GameDataOverride.Custom(serializer(customValue!!)))
                                    // Need to deselect before selecting, otherwise if this toggle was previously selected it will not fire
                                    // the change listener on the toggle group
                                    isSelected = false
                                    isSelected = true
                                }

                                graphic = hbox(spacing = 10.0) {
                                    alignment = Pos.CENTER_LEFT
                                    paddingAll = 10.0

                                    customDataProperty.perform { customValue ->
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
                                    textfield(viewModel.textProperty) {
                                        minWidth = 300.0
                                        promptText = "Custom ${type.displayName}"

                                        validator {
                                            if (it.isNullOrBlank()) error("Cannot be empty!")
                                            else {
                                                try {
                                                    deserializer(it!!)
                                                    null
                                                } catch (e: Exception) {
                                                    error("Invalid ${type.displayName}")
                                                }
                                            }
                                        }
                                    }
                                    acceptButton {
                                        isDefaultButton = true
                                        enableWhen { viewModel.valid }
                                        setOnAction {
                                            popOver.hide()
                                            viewModel.commit()
                                        }
                                    }
                                }
                            }.apply { useMaxHeight = true }
                            viewModel.validate(decorateErrors = false)
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
                            userData = SingleChoice.Clear
                        }
                    }
                }

                with(toggleGroup) {
                    val initialSelection = selectedToggle
                    val initialUserData = initialSelection?.userData
                    selectedToggleProperty().onChange { newValue ->
                        if (newValue!! != initialSelection || newValue.userData != initialUserData) {
                            val choice = newValue.userData as SingleChoice
                            when (choice) {
                                is SingleChoice.Override -> overrides[type] = choice.override
                                is SingleChoice.Clear -> overrides.remove(type)
                            }
                        } else {
                            overrides.remove(type)
                        }
                    }
                }
            }
        }
        separator()
    }

    private fun deserializeScore(raw: String) = Score(raw.toDouble(), numReviews = 1)
    private fun serializeScore(score: Score) = score.score
    private fun asText(raw: String) = raw
    private fun deserializeUrl(raw: String) = run { URL(raw); raw }
    private fun deserializeDate(raw: String) = run { LocalDate.parse(raw); raw }

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
        imageProperty().bind(imageRepository.fetchImage(url, game.id, persistIfAbsent = false))
    }

    fun show(): Choice {
        openWindow(block = true)
        return choice
    }

    private fun close(choice: Choice) {
        this.choice = choice
        close()
    }

    sealed class Choice {
        data class Override(val overrides: Map<GameDataType, GameDataOverride>) : Choice()
        object Cancel : Choice()
        object Clear : Choice()
    }

    private sealed class SingleChoice {
        data class Override(val override: GameDataOverride) : SingleChoice()
        object Clear : SingleChoice()
    }

    private class CustomTextViewModel(p: StringProperty) : ViewModel() {
        val textProperty = bind { p }
        var text by textProperty

        override fun toString() = "CustomTextViewModel(text = $text)"
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