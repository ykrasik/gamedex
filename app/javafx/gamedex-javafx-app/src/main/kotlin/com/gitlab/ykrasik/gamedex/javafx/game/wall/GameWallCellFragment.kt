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

package com.gitlab.ykrasik.gamedex.javafx.game.wall

import com.gitlab.ykrasik.gamedex.app.api.settings.DisplayPosition
import com.gitlab.ykrasik.gamedex.app.api.settings.ImageDisplayType
import com.gitlab.ykrasik.gamedex.app.javafx.settings.JavaFxCellDisplaySettings
import com.gitlab.ykrasik.gamedex.app.javafx.settings.JavaFxOverlayDisplaySettings
import com.gitlab.ykrasik.gamedex.javafx.fadeOnImageChange
import com.gitlab.ykrasik.gamedex.javafx.map
import com.gitlab.ykrasik.gamedex.javafx.perform
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventTarget
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.effect.DropShadow
import javafx.scene.effect.Glow
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
    cellDisplaySettings: JavaFxCellDisplaySettings,
    nameOverlayDisplaySettings: JavaFxOverlayDisplaySettings,
    metaTagOverlayDisplaySettings: JavaFxOverlayDisplaySettings,
    versionOverlayDisplaySettings: JavaFxOverlayDisplaySettings
) : Fragment() {
    private var _imageView: ImageView by singleAssign()
    private var _nameOverlay: Label by singleAssign()
    private var _metaTagOverlay: Label by singleAssign()
    private var _versionOverlay: Label by singleAssign()
    private var border: Rectangle by singleAssign()

    private lateinit var isHoverProperty: ReadOnlyBooleanProperty

    private val isSelectedProperty = SimpleBooleanProperty(false)
    var isSelected by isSelectedProperty

    val imageView get() = _imageView
    val nameOverlay get() = _nameOverlay
    val metaTagOverlay get() = _metaTagOverlay
    val versionOverlay get() = _versionOverlay

    override val root = stackpane {
        val root = this
        minWidthProperty().bind(maxWidthProperty())
        minHeightProperty().bind(maxHeightProperty())
        isHoverProperty = hoverProperty()

        val dropshadow = DropShadow().apply { input = Glow() }
        setOnMouseEntered { effect = dropshadow }
        setOnMouseExited { effect = null }

        stackpane {
            val content = this

            // TODO: Allow configuring this.
            background = Background(BackgroundFill(Color.LIGHTGRAY, CornerRadii.EMPTY, Insets.EMPTY))

            _imageView = imageview {
                fadeOnImageChange()
                isSmooth = true

                fitWidthProperty().bind(root.widthProperty())
                fitHeightProperty().bind(root.heightProperty())
            }

            minWidthProperty().bind(maxWidthProperty())
            minHeightProperty().bind(maxHeightProperty())

            cellDisplaySettings.imageDisplayTypeProperty.perform {
                if (it == ImageDisplayType.FixedSize) {
                    maxWidthProperty().cleanBind(root.widthProperty())
                    maxHeightProperty().cleanBind(root.heightProperty())
                } else {
                    maxWidthProperty().cleanBind(_imageView.boundsInParentProperty().doubleBinding { Math.floor(it!!.width) })
                    maxHeightProperty().cleanBind(_imageView.boundsInParentProperty().doubleBinding { Math.floor(it!!.height) })
                }
                content.requestLayout()
            }

            _nameOverlay = overlayLabel(nameOverlayDisplaySettings)
            _metaTagOverlay = overlayLabel(metaTagOverlayDisplaySettings)
            _versionOverlay = overlayLabel(versionOverlayDisplaySettings)

            this@GameWallCellFragment.border = rectangle {
                visibleWhen { cellDisplaySettings.showBorderProperty }

                x = 1.0
                y = 1.0
                arcWidth = 20.0
                arcHeight = 20.0
                heightProperty().bind(content.heightProperty().subtract(1))
                widthProperty().bind(content.widthProperty().subtract(1))
                fill = Color.TRANSPARENT
                stroke = Color.BLACK
            }

            clip = Rectangle().apply {
                arcWidth = 20.0
                arcHeight = 20.0
                heightProperty().bind(content.heightProperty())
                widthProperty().bind(content.widthProperty())
            }
        }
    }

    private fun EventTarget.overlayLabel(settings: JavaFxOverlayDisplaySettings) = label {
        addClass(Style.overlayText)
        visibleWhen {
            settings.enabledProperty.and(textProperty().isNotEmpty).and(
                settings.showOnlyWhenActiveProperty.and(isHoverProperty.or(isSelectedProperty)).or(settings.showOnlyWhenActiveProperty.not())
            )
        }

        val fontSettings = SimpleObjectProperty(FontSettings())
        settings.fontSizeProperty.onChange { fontSettings.value = fontSettings.value.copy(size = it) }
        settings.boldFontProperty.onChange { fontSettings.value = fontSettings.value.copy(weight = if (it) FontWeight.BOLD else null) }
        settings.italicFontProperty.onChange { fontSettings.value = fontSettings.value.copy(posture = if (it) FontPosture.ITALIC else null) }

        fontProperty().bind(fontSettings.map { it!!.toFont() })
        textFillProperty().bind(settings.textColorProperty.map { Color.valueOf(it) })
        backgroundProperty().bind(settings.backgroundColorProperty.map { Background(BackgroundFill(Color.valueOf(it), null, null)) })
        opacityProperty().bind(settings.opacityProperty)

        maxWidthProperty().bind(settings.fillWidthProperty.map { if (it!!) Double.MAX_VALUE else Region.USE_COMPUTED_SIZE })
        settings.positionProperty.perform { position -> StackPane.setAlignment(this, positions[position]!!) }
    }

    class Style : Stylesheet() {
        companion object {
            val overlayText by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            overlayText {
                padding = box(5.px)
                alignment = Pos.BASELINE_CENTER
            }
        }
    }

    private data class FontSettings(
        val size: Int = -1,
        val weight: FontWeight? = null,
        val posture: FontPosture? = null
    ) {
        fun toFont() = Font.font(null, weight, posture, size.toDouble())
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