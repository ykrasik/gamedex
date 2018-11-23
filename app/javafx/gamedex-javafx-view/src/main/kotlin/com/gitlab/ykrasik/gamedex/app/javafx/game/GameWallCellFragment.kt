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

package com.gitlab.ykrasik.gamedex.app.javafx.game

import com.gitlab.ykrasik.gamedex.app.api.settings.DisplayPosition
import com.gitlab.ykrasik.gamedex.app.api.settings.GameWallDisplaySettings
import com.gitlab.ykrasik.gamedex.app.api.settings.ImageDisplayType
import com.gitlab.ykrasik.gamedex.app.api.settings.OverlayDisplaySettings
import com.gitlab.ykrasik.gamedex.javafx.fadeOnImageChange
import com.gitlab.ykrasik.gamedex.javafx.importStylesheetSafe
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ObservableValue
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.effect.DropShadow
import javafx.scene.effect.Glow
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import tornadofx.*

/**
 * User: ykrasik
 * Date: 17/06/2017
 * Time: 10:07
 */
class GameWallCellFragment(
    gameWallDisplaySettings: GameWallDisplaySettings,
    nameOverlayDisplaySettings: OverlayDisplaySettings,
    metaTagOverlayDisplaySettings: OverlayDisplaySettings,
    versionOverlayDisplaySettings: OverlayDisplaySettings
) : Fragment() {
    private var imageView: ImageView by singleAssign()
    private var nameOverlayLabel: Label by singleAssign()
    private var metaTagOverlayLabel: Label by singleAssign()
    private var versionOverlayLabel: Label by singleAssign()

    private val isSelectedProperty = SimpleBooleanProperty(false)
    var isSelected by isSelectedProperty

    override val root = stackpane {
        val root = this
        minWidthProperty().bind(maxWidthProperty())
        minHeightProperty().bind(maxHeightProperty())

        val dropshadow = DropShadow().apply { input = Glow() }
        setOnMouseEntered { effect = dropshadow }
        setOnMouseExited { effect = null }

        stackpane {
            val content = this

            // TODO: Allow configuring this.
            background = Background(BackgroundFill(Color.LIGHTGRAY, CornerRadii.EMPTY, Insets.EMPTY))

            imageView = imageview {
                fadeOnImageChange()
                isSmooth = true

                fitWidthProperty().bind(root.widthProperty())
                fitHeightProperty().bind(root.heightProperty())
            }

            minWidthProperty().bind(maxWidthProperty())
            minHeightProperty().bind(maxHeightProperty())

            if (gameWallDisplaySettings.imageDisplayType == ImageDisplayType.FixedSize) {
                maxWidthProperty().cleanBind(root.widthProperty())
                maxHeightProperty().cleanBind(root.heightProperty())
            } else {
                maxWidthProperty().cleanBind(imageView.boundsInParentProperty().doubleBinding { Math.floor(it!!.width) })
                maxHeightProperty().cleanBind(imageView.boundsInParentProperty().doubleBinding { Math.floor(it!!.height) })
            }
            content.requestLayout()

            nameOverlayLabel = overlayLabel(nameOverlayDisplaySettings)
            metaTagOverlayLabel = overlayLabel(metaTagOverlayDisplaySettings)
            versionOverlayLabel = overlayLabel(versionOverlayDisplaySettings)

            if (gameWallDisplaySettings.showBorder) {
                rectangle {
                    x = 1.0
                    y = 1.0
                    arcWidth = 20.0
                    arcHeight = 20.0
                    heightProperty().bind(content.heightProperty().subtract(1))
                    widthProperty().bind(content.widthProperty().subtract(1))
                    fill = Color.TRANSPARENT
                    stroke = Color.BLACK
                }
            }

            clip = Rectangle().apply {
                arcWidth = 20.0
                arcHeight = 20.0
                heightProperty().bind(content.heightProperty())
                widthProperty().bind(content.widthProperty())
            }
        }
    }

    private fun Node.overlayLabel(settings: OverlayDisplaySettings) = label {
        addClass(Style.overlayText)
        visibleWhen {
            val showOnlyWhenActive = settings.showOnlyWhenActive.toProperty()
            settings.enabled.toProperty().and(textProperty().isNotEmpty).and(
                showOnlyWhenActive.and(this@overlayLabel.hoverProperty().or(isSelectedProperty)).or(showOnlyWhenActive.not())
            )
        }

        font = run {
            val weight = if (settings.boldFont) FontWeight.BOLD else null
            val posture = if (settings.italicFont) FontPosture.ITALIC else null
            Font.font(null, weight, posture, settings.fontSize.toDouble())
        }
        textFill = Color.valueOf(settings.textColor)
        background = Background(BackgroundFill(Color.valueOf(settings.backgroundColor), null, null))
        opacity = settings.opacity

        maxWidth = if (settings.fillWidth) Double.MAX_VALUE else Region.USE_COMPUTED_SIZE
        StackPane.setAlignment(this, positions[settings.position])
    }

    fun setImage(image: ObservableValue<Image>) = imageView.imageProperty().cleanBind(image)
    fun clearImage() {
        imageView.imageProperty().unbind()
        imageView.image = null
    }

    val image: Image? get() = imageView.image

    var preserveRatio: Boolean
        get() = imageView.isPreserveRatio
        set(value) {
            imageView.isPreserveRatio = value
        }

    var nameOverlay: String?
        get() = nameOverlayLabel.text
        set(value) {
            nameOverlayLabel.text = value
        }

    var metaTagOverlay: String?
        get() = metaTagOverlayLabel.text
        set(value) {
            metaTagOverlayLabel.text = value
        }

    var versionOverlay: String?
        get() = versionOverlayLabel.text
        set(value) {
            versionOverlayLabel.text = value
        }

    class Style : Stylesheet() {
        companion object {
            val overlayText by cssclass()

            init {
                importStylesheetSafe(Style::class)
            }
        }

        init {
            overlayText {
                padding = box(5.px)
                alignment = Pos.BASELINE_CENTER
            }
        }
    }

    companion object {
        private val positions = mapOf(
            DisplayPosition.TopLeft to Pos.TOP_LEFT,
            DisplayPosition.TopCenter to Pos.TOP_CENTER,
            DisplayPosition.TopRight to Pos.TOP_RIGHT,
            DisplayPosition.CenterLeft to Pos.CENTER_LEFT,
            DisplayPosition.Center to Pos.CENTER,
            DisplayPosition.CenterRight to Pos.CENTER_RIGHT,
            DisplayPosition.BottomLeft to Pos.BOTTOM_LEFT,
            DisplayPosition.BottomCenter to Pos.BOTTOM_CENTER,
            DisplayPosition.BottomRight to Pos.BOTTOM_RIGHT
        )
    }
}