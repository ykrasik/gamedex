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
import javafx.scene.Node
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.image.Image
import javafx.scene.layout.Region
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
//        addClass(Styles.card)
        paddingAll = 20.0

        center {
            hbox {
                paddingTop = 20.0
                var needSeparator = false
                game.rawGame.providerData.forEach { providerData ->
                    imageUrlExtractor(providerData.imageUrls)?.let { imageUrl ->
                        if (needSeparator) {
                            verticalSeparator(padding = 20.0)
                        }
                        needSeparator = true
                        val thumbnailProperty = imageLoader.fetchImage(game.id, imageUrl, persistIfAbsent = false)
                        vbox(spacing = 40.0) {
                            imageview {
                                alignment = Pos.CENTER
                                fitHeight = 120.0
                                fitWidth = 120.0
                                image = providerRepository.logo(providerData.header)
                            }
                            toggleChoice {
                                isSelected = imageUrlExtractor(game.imageUrls) == imageUrl
                                userData = Choice.Select(GameDataOverride.Provider(providerData.header.type))
                                graphic = imageDisplay(thumbnailProperty)
                            }
                        }
                    }
                }
                if (needSeparator) {
                    verticalSeparator(padding = 20.0)
                }
                vbox(spacing = 40.0) {
                    val imageUrlProperty = SimpleStringProperty("")
                    val imageProperty = SimpleObjectProperty<Image>()
                    spacer()
                    hbox(spacing = 10.0) {
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
                    spacer()
                    toggleChoice {
                        imageUrlProperty.onChange {
                            userData = Choice.Select(GameDataOverride.Custom(it!!))
                        }
                        graphic = imageDisplay(imageProperty)
                        disableProperty().bind(imageProperty.isNull)
                    }
                }

                verticalSeparator(padding = 20.0)

                vbox {
                    spacer()
                    toggleChoice {
                        text = "Clear Overrides"
                        graphic = FontAwesome.Glyph.BAN.toGraphic()
                        userData = Choice.Clear
                    }
                    spacer()
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

    private fun Node.toggleChoice(op: (ToggleButton.() -> Unit)) = togglebutton(group = toggleGroup, op = op).apply {
        setOnMouseClicked {
            if (it.clickCount == 2) close(accept = true)
        }
    }

    private fun Region.imageDisplay(image: ObservableValue<Image>) = imageview {
        paddingAll = 40.0
        fitHeight = 300.0       // TODO: Config?
        fitWidth = 200.0
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
}