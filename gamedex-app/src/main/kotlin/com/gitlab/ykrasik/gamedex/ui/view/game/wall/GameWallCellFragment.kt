package com.gitlab.ykrasik.gamedex.ui.view.game.wall

import com.gitlab.ykrasik.gamedex.settings.GameWallSettings
import com.gitlab.ykrasik.gamedex.ui.fadeOnImageChange
import com.gitlab.ykrasik.gamedex.ui.map
import com.gitlab.ykrasik.gamedex.ui.perform
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
import tornadofx.*

/**
 * User: ykrasik
 * Date: 17/06/2017
 * Time: 10:07
 */
class GameWallCellFragment : Fragment() {
    private val settings: GameWallSettings by di()

    private var content: StackPane by singleAssign()
    private var _imageView: ImageView by singleAssign()
    private var _metaTagOverlay: Label by singleAssign()
    private var _versionOverlay: Label by singleAssign()
    private var border: Rectangle by singleAssign()

    val imageView get() = _imageView
    val metaTagOverlay get() = _metaTagOverlay
    val versionOverlay get() = _versionOverlay

    override val root = stackpane {
        val root = this
        minWidthProperty().bind(maxWidthProperty())
        minHeightProperty().bind(maxHeightProperty())

        val dropshadow = DropShadow().apply { input = Glow() }
        setOnMouseEntered { effect = dropshadow }
        setOnMouseExited { effect = null }

        content = stackpane {
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
            settings.cell.isFixedSizeProperty.perform { isFixedSize ->
                if (isFixedSize) {
                    maxWidthProperty().cleanBind(root.widthProperty())
                    maxHeightProperty().cleanBind(root.heightProperty())
                } else {
                    maxWidthProperty().cleanBind(_imageView.boundsInParentProperty().doubleBinding { Math.floor(it!!.width) })
                    maxHeightProperty().cleanBind(_imageView.boundsInParentProperty().doubleBinding { Math.floor(it!!.height) })
                }
                content.requestLayout()
            }

            _metaTagOverlay = overlayLabel(settings.metaTagOverlay)
            _versionOverlay = overlayLabel(settings.versionOverlay)

            this@GameWallCellFragment.border = rectangle {
                visibleWhen { settings.cell.isShowBorderProperty }

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

    // TODO: Allow configuring the overlay color / opacity.
    private fun EventTarget.overlayLabel(settings: GameWallSettings.OverlaySettings) = label {
        addClass(Style.overlayText)

        visibleWhen { settings.isShowProperty.and(textProperty().isNotNull.and(textProperty().isNotEmpty)) }
        maxWidthProperty().bind(settings.fillWidthProperty.map { if (it!!) Double.MAX_VALUE else Region.USE_COMPUTED_SIZE })
        settings.locationProperty.perform { location -> StackPane.setAlignment(this, location) }
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
                backgroundColor = multi(Color.LIGHTGRAY)
                opacity = 0.85
                padding = box(5.px)
                alignment = Pos.BASELINE_CENTER
            }
        }
    }
}