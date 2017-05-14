package com.gitlab.ykrasik.gamedex.ui.fragment

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameDataOverride
import com.gitlab.ykrasik.gamedex.GameDataType
import com.gitlab.ykrasik.gamedex.ProviderData
import com.gitlab.ykrasik.gamedex.core.ImageLoader
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.ui.*
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.beans.value.ObservableValue
import javafx.geometry.Pos
import javafx.scene.control.TabPane
import javafx.scene.control.ToggleGroup
import javafx.scene.image.Image
import javafx.scene.layout.BorderStrokeStyle
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import org.controlsfx.glyphfont.FontAwesome
import tornadofx.*
import java.time.LocalDate

/**
 * User: ykrasik
 * Date: 13/05/2017
 * Time: 11:06
 */
class EditGameDataFragment(private val game: Game, private val initialTab: GameDataType) : Fragment("'${game.name}': Change Data") {
    private val providerRepository: GameProviderRepository by di()
    private val imageLoader: ImageLoader by di()

    // TODO: Play around with representing this as a CustomProvider in the UserData
    private var overrides = HashMap(game.rawGame.userData?.overrides ?: emptyMap())

    private var choice: Choice = Choice.Cancel

    override val root = borderpane {
        top {
            toolbar {
                acceptButton { setOnAction { close(choice = Choice.Override(overrides)) } }
                verticalSeparator()
                jfxButton(graphic = FontAwesome.Glyph.TIMES_CIRCLE.toGraphic { size(26.0) }) {
                    addClass(CommonStyle.toolbarButton)
                    tooltip("Reset to default")
                    setOnAction { close(choice = Choice.Clear) }
                }
                verticalSeparator()
                spacer()
                verticalSeparator()
                cancelButton { setOnAction { close(choice = Choice.Cancel) } }
            }
        }
        center {
            tabpane {
                choiceTab(
                    GameDataType.name_,
                    providerDataExtractor = { it.gameData.name },
                    gameDataExtractor = Game::name,
                    dataDisplay = { textDisplay(it) },
                    customDataDisplay = { customTextChoice(it, GameDataType.name_) }
                )
                choiceTab(
                    GameDataType.description,
                    providerDataExtractor = { it.gameData.description },
                    gameDataExtractor = Game::description,
                    dataDisplay = { textDisplay(it) },
                    customDataDisplay = { customTextChoice(it, GameDataType.description) }
                )
                choiceTab(
                    GameDataType.releaseDate,
                    providerDataExtractor = { it.gameData.releaseDate },
                    gameDataExtractor = Game::releaseDate,
                    dataDisplay = { textDisplay(it) },
                    customDataDisplay = { customTextChoice(it, GameDataType.releaseDate, LocalDate::parse, { it }) }
                )
                choiceTab(
                    GameDataType.criticScore,
                    providerDataExtractor = { it.gameData.criticScore },
                    gameDataExtractor = Game::criticScore,
                    dataDisplay = { textDisplay(it.toString()) },
                    customDataDisplay = { customTextChoice(it, GameDataType.criticScore, String::toDouble) }
                )
                choiceTab(
                    GameDataType.userScore,
                    providerDataExtractor = { it.gameData.userScore },
                    gameDataExtractor = Game::userScore,
                    dataDisplay = { textDisplay(it.toString()) },
                    customDataDisplay = { customTextChoice(it, GameDataType.userScore, String::toDouble) }
                )
                choiceTab(
                    GameDataType.thumbnail,
                    providerDataExtractor = { it.imageUrls.thumbnailUrl },
                    gameDataExtractor = Game::thumbnailUrl,
                    dataDisplay = { imageDisplay(it) },
                    customDataDisplay = { customImageChoice(it, GameDataType.thumbnail) }
                )
                choiceTab(
                    GameDataType.poster,
                    providerDataExtractor = { it.imageUrls.posterUrl },
                    gameDataExtractor = Game::posterUrl,
                    dataDisplay = { imageDisplay(it) },
                    customDataDisplay = { customImageChoice(it, GameDataType.poster) }
                )
            }
        }
    }

    private fun <T> TabPane.choiceTab(type: GameDataType,
                                      providerDataExtractor: (ProviderData) -> T?,
                                      gameDataExtractor: (Game) -> T?,
                                      dataDisplay: VBox.(T) -> Unit,
                                      customDataDisplay: VBox.(ToggleGroup) -> Unit) = nonClosableTab(type.displayName) {
        if (type == initialTab) {
            selectionModel.select(this)
        }

        hbox {
            paddingAll = 20.0
            isFillHeight = true
            var needSeparator = false
            val toggleGroup = togglegroup {
                game.rawGame.providerData.forEach { providerData ->
                    providerDataExtractor(providerData)?.let { data ->
                        if (needSeparator) {
                            verticalSeparator(padding = 20.0)
                        }
                        needSeparator = true
                        jfxToggleNode {
                            addClass(Style.choice, Style.choiceButton)
                            if (gameDataExtractor(game) == data) {
                                addClass(Style.currentlySelected)
                                isSelected = true
                            }
                            userData = SingleChoice.Override(GameDataOverride.Provider(providerData.header.type))
                            graphic = vbox {
                                alignment = Pos.CENTER
                                imageview {
                                    fitHeight = 120.0
                                    fitWidth = 120.0
                                    isPreserveRatio = true
                                    image = providerRepository.logo(providerData.header)
                                }
                                spacer()
                                dataDisplay(data)
                            }
                        }
                    }
                }
                if (needSeparator) {
                    verticalSeparator(padding = 20.0)
                }
                vbox(spacing = 10) {
                    addClass(Style.choice)
                    alignment = Pos.CENTER
                    customDataDisplay(this@hbox.getToggleGroup()!!)
                }

                verticalSeparator(padding = 20.0)

                jfxToggleNode {
                    addClass(Style.choice, Style.choiceButton)
                    graphic = FontAwesome.Glyph.TIMES_CIRCLE.toGraphic { size(80.0) }
                    userData = SingleChoice.Clear
                }
            }

            with(toggleGroup) {
                val initialSelection = selectedToggle!!
                val initialUserData = initialSelection.userData
                selectedToggleProperty().addListener { _, oldValue, newValue ->
                    if (oldValue != null && newValue == null) {
                        selectToggle(oldValue)
                    }
                    if (newValue != null) {
                        if (newValue != initialSelection || newValue.userData != initialUserData) {
                            val choice = newValue.userData as SingleChoice
                            when (choice) {
                                is SingleChoice.Override -> overrides.put(type, choice.override)
                                is SingleChoice.Clear -> overrides.remove(type)
                            }
                        } else {
                            overrides.remove(type)
                        }
                    }
                }
            }
        }
    }

    // TODO: This is similar to customImageChoice, can probably save some code.
    private fun VBox.customTextChoice(toggleGroup: ToggleGroup,
                                      type: GameDataType,
                                      validator: (String) -> Any = { it },
                                      converter: (String) -> Any = validator) {
        val customText = CustomTextViewModel()
        val customDataProperty = SimpleStringProperty("")
        hbox(spacing = 10) {
            textfield(customText.textProperty) {
                promptText = "Custom ${type.displayName}"
                prefWidth = 240.0
                validator {
                    if (it.isNullOrBlank()) error()
                    else {
                        val valid = try {
                            validator(it!!)
                            true
                        } catch (e: Exception) {
                            false
                        }
                        if (!valid) error("Invalid ${type.displayName}") else null
                    }
                }
            }
            jfxButton(graphic = FontAwesome.Glyph.CHECK_CIRCLE_ALT.toGraphic()) {
                enableWhen { customText.valid }
                setOnAction {
                    customDataProperty.value = customText.text
                }
            }
        }
        jfxToggleNode(group = toggleGroup) {
            addClass(Style.choice, Style.choiceButton)

            val currentOverride = game.userData?.overrides?.get(type)
            if (currentOverride is GameDataOverride.Custom) {
                isSelected = true
                text = currentOverride.data.toString()
                customDataProperty.value = text
            }

            customDataProperty.onChange {
                userData = SingleChoice.Override(GameDataOverride.Custom(converter(it!!)))
                // Need to deselect before selecting, otherwise if this toggle was previously selected it will not fire
                // the change listener on the toggle group
                isSelected = false
                isSelected = true
            }

            disableWhen { customDataProperty.isEmpty.or(customDataProperty.isNull) }

            graphic = vbox {
                alignment = Pos.CENTER
                spacer()
                textDisplay(customDataProperty)
            }
        }
        customText.validate(decorateErrors = false)
    }

    private fun VBox.textDisplay(text: String) = textDisplay(text.toProperty())

    private fun VBox.textDisplay(text: StringProperty) {
        paddingAll = 20.0
        label(text) {
            addClass(Style.textData)
            isWrapText = true
        }
    }

    private fun VBox.customImageChoice(toggleGroup: ToggleGroup, type: GameDataType) {
        val imageProperty = SimpleObjectProperty<Image>()
        val imageUrlProperty = SimpleStringProperty("")
        val currentOverride = game.userData?.overrides?.get(type)
        hbox(spacing = 10) {
            val customUrl = textfield {
                promptText = "Custom ${type.displayName} URL"
                prefWidth = 240.0
                if (currentOverride is GameDataOverride.Custom) {
                    val url = currentOverride.data as String
                    text = url
                    imageProperty.cleanBind(imageLoader.fetchImage(game.id, url, persistIfAbsent = false))
                }
            }
            jfxButton(graphic = FontAwesome.Glyph.DOWNLOAD.toGraphic()) {
                disableWhen { customUrl.textProperty().isEmpty }
                setOnAction {
                    imageUrlProperty.value = customUrl.text
                    imageProperty.cleanBind(imageLoader.downloadImage(customUrl.text))
                }
            }
        }
        jfxToggleNode(group = toggleGroup) {
            addClass(Style.choice, Style.choiceButton)

            if (currentOverride is GameDataOverride.Custom) {
                isSelected = true
            }

            imageUrlProperty.onChange {
                userData = SingleChoice.Override(GameDataOverride.Custom(it!!))
                // Need to deselect before selecting, otherwise if this toggle was previously selected it will not fire
                // the change listener on the toggle group
                isSelected = false
                isSelected = true
            }
            disableProperty().bind(imageProperty.isNull)

            graphic = vbox {
                alignment = Pos.CENTER
                spacer()
                imageDisplay(imageProperty)
            }
        }
    }

    private fun VBox.imageDisplay(url: String) = imageDisplay(imageLoader.fetchImage(game.id, url, persistIfAbsent = false))

    private fun VBox.imageDisplay(image: ObservableValue<Image>) = imageview {
        paddingAll = 20.0
        fitHeight = 300.0       // TODO: Config?
        fitWidth = 200.0
        isPreserveRatio = true
        imageProperty().bind(image)
    }

    fun show(): Choice {
        openWindow(block = true, owner = null/*, stageStyle = StageStyle.UNDECORATED*/)
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

    private class CustomTextViewModel : ViewModel() {
        val textProperty = bind { SimpleStringProperty() }
        var text by textProperty

        override fun toString() = "CustomTextViewModel(text = $text)"
    }

    class Style : Stylesheet() {
        companion object {
            val choice by cssclass()
            val choiceButton by cssclass()
            val currentlySelected by cssclass()
            val textData by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            choice {
                maxHeight = 450.px
                prefWidth = 280.px
                maxWidth = 280.px
            }

            choiceButton {
                prefHeight = 430.px

                borderColor = multi(box(Color.LIGHTBLUE))
                borderRadius = multi(box(6.px))
                backgroundRadius = multi(box(6.px))

                and(hover) {
                    borderColor = multi(box(Color.CORNFLOWERBLUE))
                }
            }

            currentlySelected {
                borderStyle = multi(BorderStrokeStyle.DOTTED)
            }

            textData {
                fontSize = 24.px
            }
        }
    }
}