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

import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.gitlab.ykrasik.gamedex.util.asPercent
import com.jfoenix.controls.*
import javafx.beans.property.BooleanProperty
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.ListView
import javafx.scene.layout.*
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
    val size = itemsProperty().doubleBinding { minOf(it!!.size, numItems) * 24.1 }
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

fun ObservableValue<out Number>.asPercent() = stringBinding { (it ?: 0).toDouble().asPercent() }

fun Region.enableWhen(isValid: JavaFxObjectMutableStateFlow<IsValid>, wrapInErrorTooltip: Boolean = true): Unit =
    enableWhen(isValid.property, wrapInErrorTooltip)

fun Region.enableWhen(isValid: ObservableValue<IsValid>, wrapInErrorTooltip: Boolean = true) {
    enableWhen { isValid.typesafeBooleanBinding { it.isSuccess } }
    if (wrapInErrorTooltip) {
        useMaxWidth = true
        hgrow = Priority.ALWAYS
        wrapInHbox {
            // Using a binding here was buggy!!!! The binding wasn't always getting called.
            errorTooltip(isValid.map { it.errorText() ?: "" })
        }
    }
}

fun Field.enableWhen(isValid: JavaFxObjectMutableStateFlow<IsValid>): Unit = enableWhen(isValid.property)
fun Field.enableWhen(isValid: ObservableValue<IsValid>) {
    val enabled = isValid.typesafeBooleanBinding { it.isSuccess }
    label.enableWhen { enabled }
    inputContainer.enableWhen { enabled }
    labelContainer.errorTooltip(isValid.typesafeStringBinding { it.exceptionOrNull()?.message ?: "" })
}

fun Node.showWhen(isValid: JavaFxObjectMutableStateFlow<IsValid>): Unit =
    showWhen { isValid.property.typesafeBooleanBinding { it.isSuccess } }

fun Node.showWhen(expr: () -> ObservableValue<Boolean>) {
    val shouldShow = expr()
    managedWhen { shouldShow }
    visibleWhen { shouldShow }
}

fun <T : Node> T.visibleWhen(isValid: JavaFxObjectMutableStateFlow<IsValid>): T =
    visibleWhen { isValid.property.typesafeBooleanBinding { it.isSuccess } }

fun <T : Node> T.enableWhen(isValid: JavaFxObjectMutableStateFlow<IsValid>): T =
    enableWhen { isValid.property.typesafeBooleanBinding { it.isSuccess } }

inline fun Node.wrapInHbox(crossinline op: HBox.() -> Unit = {}) {
    val wrapper = HBox().apply {
        alignment = Pos.CENTER_LEFT
        op()
    }
    parentProperty().perform {
        if (it !is Pane || it == wrapper) return@perform

        val index = it.children.indexOf(this)
        it.children.removeAt(index)
        wrapper.children += this
        it.children.add(index, wrapper)
    }
}

fun JavaFxObjectMutableStateFlow<IsValid>.errorText(): ObservableValue<String> =
    property.typesafeStringBinding { it.errorText() ?: "" }

fun IsValid.errorText(): String? = exceptionOrNull()?.message

inline fun Parent.gap(size: Number = 20, crossinline f: Region.() -> Unit = {}) = region {
    minWidth = size.toDouble()
    properties[PopOverMenu.popOverIgnore] = true
    f()
}

inline fun Parent.verticalGap(size: Number = 10, crossinline f: Region.() -> Unit = {}) = region {
    minHeight = size.toDouble()
    properties[PopOverMenu.popOverIgnore] = true
    f()
}

fun EventTarget.defaultHbox(spacing: Number = 5, alignment: Pos = Pos.CENTER_LEFT, op: HBox.() -> Unit = {}) =
    hbox(spacing, alignment, op)

fun EventTarget.defaultVbox(spacing: Number = 5, alignment: Pos = Pos.CENTER_LEFT, op: VBox.() -> Unit = {}) =
    vbox(spacing, alignment, op)

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

inline fun EventTarget.jfxSpinner(op: JFXSpinner.() -> Unit = {}): JFXSpinner =
    opcr(this, JFXSpinner(), op)

inline fun <T> EventTarget.jfxTreeView(op: JFXTreeView<T>.() -> Unit): JFXTreeView<T> =
    opcr(this, JFXTreeView(), op)

fun <T : Node> EventTarget.add(child: T, op: T.() -> Unit) = add(child.apply { op() })