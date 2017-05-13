package com.gitlab.ykrasik.gamedex.ui.fragment

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameDataOverride
import com.gitlab.ykrasik.gamedex.GameDataOverrides
import com.gitlab.ykrasik.gamedex.ImageUrls
import com.gitlab.ykrasik.gamedex.core.ImageLoader
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.ui.*
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.geometry.Pos
import javafx.scene.control.ToggleGroup
import javafx.scene.image.Image
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import org.controlsfx.glyphfont.FontAwesome
import tornadofx.*

/**
 * User: ykrasik
 * Date: 23/04/2017
 * Time: 15:33
 */
class ChangeImageFragment(
    private val game: Game,
    private val imageUrlExtractor: (ImageUrls) -> String?,
    private val imageOverrideExtractor: (GameDataOverrides) -> GameDataOverride?
) : Fragment(game.name) {
    private val imageLoader: ImageLoader by di()
    private val providerRepository: GameProviderRepository by di()

    private val toggleGroup = ToggleGroup().apply {
        selectedToggleProperty().addListener { _, oldValue, newValue ->
            if (oldValue != null && newValue == null) {
                selectToggle(oldValue)
            }
        }
    }

    private var accept = false

    override val root = borderpane {
        paddingAll = 20.0

        center {
            hbox {
                paddingTop = 20.0
                isFillHeight = true
                var needSeparator = false
                game.rawGame.providerData.forEach { providerData ->
                    imageUrlExtractor(providerData.imageUrls)?.let { imageUrl ->
                        if (needSeparator) {
                            verticalSeparator(padding = 20.0)
                        }
                        needSeparator = true
                        jfxToggleNode(group = toggleGroup) {
                            addClass(Style.choice, Style.choiceButton)
                            isSelected = imageUrlExtractor(game.imageUrls) == imageUrl
                            userData = Choice.Select(GameDataOverride.Provider(providerData.header.type))
                            graphic = vbox {
                                imageview {
                                    alignment = Pos.CENTER
                                    fitHeight = 120.0
                                    fitWidth = 120.0
                                    isPreserveRatio = true
                                    image = providerRepository.logo(providerData.header)
                                }
                                spacer()
                                imageDisplay(imageLoader.fetchImage(game.id, imageUrl, persistIfAbsent = false))
                            }
                        }
                    }
                }
                if (needSeparator) {
                    verticalSeparator(padding = 20.0)
                }
                vbox(spacing = 10) {
                    addClass(Style.choice)
                    val imageProperty = SimpleObjectProperty<Image>()
                    val imageUrlProperty = SimpleStringProperty("")
                    hbox(spacing = 10) {
                        val customUrl = textfield {
                            promptText = "Custom URL"
                            prefWidth = 240.0
                            val imageOverride = game.userData?.overrides?.let { imageOverrideExtractor(it) }
                            if (imageOverride is GameDataOverride.Custom) {
                                val url = imageOverride.data as String
                                text = url
                                imageProperty.cleanBind(imageLoader.fetchImage(game.id, url, persistIfAbsent = false))
                            }
                        }
                        jfxButton(graphic = FontAwesome.Glyph.DOWNLOAD.toGraphic()) {
                            setOnAction {
                                imageUrlProperty.value = customUrl.text
                                imageProperty.cleanBind(imageLoader.downloadImage(customUrl.text))
                            }
                        }
                    }
                    jfxToggleNode(group = toggleGroup) {
                        addClass(Style.choice, Style.choiceButton)

                        imageUrlProperty.onChange {
                            userData = Choice.Select(GameDataOverride.Custom(it!!))
                        }
                        disableProperty().bind(imageProperty.isNull)

                        graphic = vbox {
                            spacer()
                            imageDisplay(imageProperty)
                        }
                    }
                }

                verticalSeparator(padding = 20.0)

                jfxToggleNode(group = toggleGroup) {
                    addClass(Style.choice, Style.choiceButton)
                    graphic = FontAwesome.Glyph.TIMES_CIRCLE.toGraphic { size(80.0) }
                    userData = Choice.Clear
                }
            }
        }
        top {
            toolbar {
                acceptButton { setOnAction { close(accept = true) } }
                verticalSeparator()
                spacer()
                verticalSeparator()
                cancelButton { setOnAction { close(accept = false) } }
            }
        }
    }

    private fun Region.imageDisplay(image: ObservableValue<Image>) = imageview {
        paddingAll = 40.0
        fitHeight = 300.0       // TODO: Config?
        fitWidth = 200.0
        isPreserveRatio = true
        imageProperty().bind(image)
    }

    @Suppress("UNCHECKED_CAST")
    fun show(): Choice {
        openModal(block = true/*, stageStyle = StageStyle.UNDECORATED*/)
        return if (accept) {
            toggleGroup.selectedToggle.userData as Choice
        } else {
            Choice.Cancel
        }
    }

    private fun close(accept: Boolean) {
        this.accept = accept
        close()
    }

    sealed class Choice {
        data class Select(val override: GameDataOverride) : Choice()
        object Cancel : Choice()
        object Clear : Choice()
    }

    companion object {
        fun thumbnail(game: Game) = ChangeImageFragment(game, ImageUrls::thumbnailUrl, GameDataOverrides::thumbnail)
        fun poster(game: Game) = ChangeImageFragment(game, ImageUrls::posterUrl, GameDataOverrides::poster)
    }

    class Style : Stylesheet() {
        companion object {
            val choice by cssclass()
            val choiceButton by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            choice {
                maxHeight = 450.px
            }

            choiceButton {
                prefHeight = 450.px
                prefWidth = 250.px

                borderColor = multi(box(Color.LIGHTBLUE))
                borderRadius = multi(box(6.px))
                backgroundRadius = multi(box(6.px))

                and(hover) {
                    borderColor = multi(box(Color.CORNFLOWERBLUE))
                }
            }
        }
    }
}