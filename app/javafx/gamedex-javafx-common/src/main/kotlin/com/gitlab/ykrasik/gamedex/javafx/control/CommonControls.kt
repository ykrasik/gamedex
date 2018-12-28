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

package com.gitlab.ykrasik.gamedex.javafx.control

import com.gitlab.ykrasik.gamedex.javafx.JavaFxState
import com.gitlab.ykrasik.gamedex.javafx.doNotConsumeMouseEvents
import com.gitlab.ykrasik.gamedex.javafx.map
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.gitlab.ykrasik.gamedex.util.asPercent
import com.jfoenix.controls.JFXListView
import com.jfoenix.controls.JFXProgressBar
import com.jfoenix.controls.JFXTabPane
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.ListView
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.shape.Rectangle
import org.controlsfx.control.MaskerPane
import tornadofx.*

/**
 * User: ykrasik
 * Date: 16/05/2017
 * Time: 09:33
 */
inline fun Node.clipRectangle(op: Rectangle.() -> Unit) {
    clip = Rectangle().apply(op)
}

fun Region.clipRectangle(arc: Number) = clipRectangle {
    arcHeight = arc.toDouble()
    arcWidth = arc.toDouble()
    heightProperty().bind(this@clipRectangle.heightProperty())
    widthProperty().bind(this@clipRectangle.widthProperty())
}

fun <T> ListView<T>.fitAtMost(numItems: Int) {
    val size = itemsProperty().map { minOf(it!!.size, numItems) * 24.1 }
    minHeightProperty().bind(size)
    maxHeightProperty().bind(size)
}

inline fun StackPane.maskerPane(op: MaskerPane.() -> Unit = {}) = opcr(this, MaskerPane(), op)
inline fun StackPane.maskerPane(visible: BooleanProperty, op: MaskerPane.() -> Unit = {}) = maskerPane {
    visibleWhen { visible }
    op()
}

inline fun View.skipFirstTime(op: () -> Unit) {
    val skip = properties.getOrDefault("Gamedex.skipFirstTime", true) as Boolean
    if (skip) {
        properties["Gamedex.skipFirstTime"] = false
    } else {
        op()
    }
}

@Suppress("UNCHECKED_CAST")
fun EventTarget.imageview(image: ObservableValue<Image>, op: ImageView.() -> Unit = {}) =
    imageview(image as ObservableValue<Image?>, op)

fun ObservableValue<out Number>.asPercent() = stringBinding { (it ?: 0).toDouble().asPercent() }

fun Node.enableWhen(isValid: JavaFxState<IsValid, SimpleObjectProperty<IsValid>>): Unit = enableWhen(isValid.property)
fun Node.enableWhen(isValid: ObservableValue<IsValid>) {
    enableWhen { isValid.booleanBinding { it?.isSuccess ?: false } }
    wrapInErrorTooltip(isValid.stringBinding { it?.errorOrNull?.message })
}

fun Node.showWhen(expr: () -> ObservableValue<Boolean>) {
    val shouldShow = expr()
    managedWhen { shouldShow }
    visibleWhen { shouldShow }
}

inline fun Parent.gap(size: Number = 20, f: Region.() -> Unit = {}) =
    region { minWidth = size.toDouble() }.also(f)

inline fun Parent.verticalGap(size: Number = 10, f: Region.() -> Unit = {}) =
    region { minHeight = size.toDouble() }.also(f)

fun EventTarget.defaultHbox(spacing: Number = 5, alignment: Pos = Pos.CENTER_LEFT, op: HBox.() -> Unit = {}) =
    hbox(spacing, alignment, op)

inline fun Fieldset.horizontalField(text: String? = null, forceLabelIndent: Boolean = false, crossinline op: Field.() -> Unit = {}) =
    field(text, forceLabelIndent = forceLabelIndent) {
        (inputContainer as HBox).alignment = Pos.CENTER_LEFT
        op()
    }

inline fun EventTarget.jfxTabPane(op: JFXTabPane.() -> Unit = {}): JFXTabPane =
    opcr(this, JFXTabPane()) {
        doNotConsumeMouseEvents()
        op()
    }

inline fun <T> EventTarget.jfxListView(values: ObservableList<T>, op: JFXListView<T>.() -> Unit = {}) =
    opcr(this, JFXListView<T>()) {
        if (values is SortedFilteredList<T>) {
            values.bindTo(this)
        } else {
            items = values
        }
        op()
    }

inline fun EventTarget.jfxProgressBar(op: JFXProgressBar.() -> Unit = {}): JFXProgressBar =
    opcr(this, JFXProgressBar(), op)

inline fun EventTarget.jfxProgressBar(progress: ObservableValue<out Number>, op: JFXProgressBar.() -> Unit = {}): JFXProgressBar =
    jfxProgressBar {
        progressProperty().bind(progress)
        op()
    }