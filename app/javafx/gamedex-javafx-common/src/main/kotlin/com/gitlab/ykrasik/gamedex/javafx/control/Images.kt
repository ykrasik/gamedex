/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.javafx.control

import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.geometry.HPos
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.geometry.VPos
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.util.Duration
import tornadofx.*
import java.io.ByteArrayInputStream

/**
 * User: ykrasik
 * Date: 30/03/2017
 * Time: 08:36
 */
fun ByteArray.toImage(): Image = Image(ByteArrayInputStream(this))

inline fun Image.toImageView(op: ImageView.() -> Unit = {}): ImageView = ImageView(this).apply { op() }
inline fun Image.toImageView(
    height: Number? = null,
    width: Number? = null,
    f: ImageView.() -> Unit = {}
): ImageView = toImageView {
    height?.let { fitHeight = it.toDouble() }
    width?.let { fitWidth = it.toDouble() }
    isPreserveRatio = true
    f(this)
}

@Suppress("UNCHECKED_CAST")
inline fun EventTarget.imageview(
    image: ObservableValue<Image>,
    isPreserveRatio: Boolean = true,
    crossinline op: ImageView.() -> Unit = {}
) = imageview(image as ObservableValue<Image?>) {
    this.isPreserveRatio = isPreserveRatio
    op()
}

inline fun EventTarget.makeRoundCorners(
    imageView: ImageView,
    arc: Number = 20,
    crossinline op: VBox.() -> Unit = {}
) = vbox {
    alignment = Pos.TOP_RIGHT
    maxWidth = Region.USE_PREF_SIZE
    maxHeight = Region.USE_PREF_SIZE
    children += imageView
    clipRectangle(arc = arc)
    op()
}

fun ImageView.fadeOnImageChange(fadeInDuration: Duration = 0.2.seconds): ImageView = apply {
    imageProperty().onChange {
        fade(fadeInDuration, 1.0, play = true) {
            fromValue = 0.0
        }
    }
}

inline fun EventTarget.imageViewResizingPane(
    imageView: ImageView,
    op: ImageViewResizingPane.() -> Unit = {}
) = opcr(this, ImageViewResizingPane(imageView), op)

inline fun EventTarget.imageViewResizingPane(
    image: ObservableValue<Image>,
    op: ImageViewResizingPane.() -> Unit = {}
) = imageViewResizingPane(ImageView()) {
    imageProperty.bind(image)
    op()
}

// TODO: Distorts the image sometimes.
class ImageViewResizingPane(private val imageView: ImageView) : Pane() {
    init {
        children += imageView
    }

    val imageProperty = imageView.imageProperty()
    var image by imageProperty

    override fun layoutChildren() {
        layoutInArea(imageView, 0.0, 0.0, imageView.fitWidth, imageView.fitHeight, 0.0, HPos.CENTER, VPos.CENTER)
    }

    override fun resize(width: Double, height: Double) {
        val normalizedWidth = Math.min(width, image.width * maxEnlargeRatio)
        val normalizedHeight = Math.min(height, image.height * maxEnlargeRatio)

        imageView.fitHeight = normalizedHeight
        imageView.fitWidth = normalizedWidth
        // If there are a few tiny pixel margins between the image and the border (no idea why JavaFX does this, but it does),
        // stretch the image to fill those in.
        imageView.isPreserveRatio = !(Math.abs(normalizedWidth - width) <= 10 && Math.abs(normalizedHeight - height) <= 10)

        super.resize(normalizedWidth, normalizedHeight)
    }

    override fun computePrefWidth(availableHeight: Double): Double =
        if (availableHeight <= 0) {
            availableHeight
        } else {
            availableHeight / imageRatio
        }

    override fun computePrefHeight(availableWidth: Double): Double =
        if (availableWidth <= 0) {
            availableWidth
        } else {
            availableWidth * imageRatio
        }

    override fun computeMinWidth(availableHeight: Double): Double {
        val prefWidth = computePrefWidth(availableHeight)
        return limitByMaxWidth(prefWidth)
    }

    override fun computeMinHeight(availableWidth: Double): Double {
        val prefHeight = computePrefHeight(availableWidth)
        return limitByMaxHeight(prefHeight)
    }

    override fun getContentBias(): Orientation = Orientation.VERTICAL

    private val imageRatio: Double get() = imageView.image.run { height / width }

    private fun limitByMaxWidth(width: Double) = limitIfApplicable(width, maxWidth)
    private fun limitByMaxHeight(height: Double) = limitIfApplicable(height, maxHeight)
    private fun limitIfApplicable(value: Double, max: Double) = if (max > 0) Math.min(value, max) else value

    companion object {
        private val maxEnlargeRatio = 2    // FIXME: Property
    }
}
