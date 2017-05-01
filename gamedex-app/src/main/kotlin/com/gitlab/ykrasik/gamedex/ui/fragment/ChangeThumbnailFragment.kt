package com.gitlab.ykrasik.gamedex.ui.fragment

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameProviderType
import com.gitlab.ykrasik.gamedex.core.ImageLoader
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.ui.cancelButton
import com.gitlab.ykrasik.gamedex.ui.okButton
import com.gitlab.ykrasik.gamedex.ui.verticalSeparator
import javafx.geometry.HPos
import javafx.scene.control.ToggleGroup
import tornadofx.*

/**
 * User: ykrasik
 * Date: 23/04/2017
 * Time: 15:33
 */
class ChangeThumbnailFragment(private val game: Game) : Fragment(game.name) {
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
                var needSeparator = false
                game.rawGame.rawGameData.forEach { rawGameData ->
                    rawGameData.imageUrls.thumbnailUrl?.let { thumbnailUrl ->
                        if (needSeparator) {
                            verticalSeparator(padding = 20.0)
                        }
                        needSeparator = true
                        val thumbnailProperty = imageLoader.fetchImage(game.id, thumbnailUrl, persistIfAbsent = false)
                        togglebutton(group = toggleGroup) {
                            isSelected = game.thumbnailUrl == thumbnailUrl
                            userData = Pair(rawGameData.providerData.type, thumbnailUrl)
                            graphic = gridpane {
                                paddingAll = 40.0
                                vgap = 10.0

                                row {
                                    imageview {
                                        gridpaneConstraints { hAlignment = HPos.CENTER }
                                        fitHeight = 120.0
                                        fitWidth = 120.0
                                        image = providerRepository.logo(rawGameData.providerData)
                                    }
                                }
                                row {
                                    imageview {
                                        fitHeight = 300.0       // TODO: Config?
                                        fitWidth = 200.0
                                        imageProperty().bind(thumbnailProperty)
                                    }
                                }
                            }
                            setOnMouseClicked {
                                if (it.clickCount == 2) close(accept = true)
                            }
                        }
                    }
                }
            }
        }
        bottom {
            buttonbar {
//                minHeight = 40.0
                paddingTop = 10.0
                okButton { setOnAction { close(accept = true) } }
                cancelButton { setOnAction { close(accept = false) } }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun show(): Pair<GameProviderType, String>? {
        openModal(block = true/*, stageStyle = StageStyle.UNDECORATED*/)
        // TODO: Support clearing override status, via a special button.
        return if (accept) {
            toggleGroup.selectedToggle.userData as Pair<GameProviderType, String>
        } else {
            null
        }
    }

    private fun close(accept: Boolean) {
        this.accept = accept
        close()
    }
//
//    class Style : Stylesheet() {
//        companion object {
//            val thumbnailContainer by cssclass()
//
//            init {
//                importStylesheet(Style::class)
//            }
//        }
//
//        init {
//            thumbnailContainer {
//                //                and(hover) {
////                    translateX = 1.px
////                    translateY = 1.px
////                    effect = DropShadow(BlurType.GAUSSIAN, Color.web("#0093ff"), 12.0, 0.2, 0.0, 1.0)
////                }
////                and(pressed) {
////                    backgroundColor = multi(Color.BLACK)
////                }
//                and(selected) {
//                    backgroundColor = multi(Color.BLUEVIOLET)
//                }
//            }
//        }
//    }
}