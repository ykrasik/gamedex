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

package com.gitlab.ykrasik.gamedex.javafx

import com.jfoenix.utils.JFXNodeUtils
import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.geometry.Bounds
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Rectangle2D
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import javafx.stage.Screen
import javafx.stage.Stage
import kotlinx.coroutines.*
import tornadofx.*
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.jvm.isAccessible

/**
 * User: ykrasik
 * Date: 02/01/2017
 * Time: 20:45
 */
val screenBounds: Rectangle2D = Screen.getPrimary().bounds

private object StylesheetLock

fun <T : Stylesheet> importStylesheetSafe(stylesheetType: KClass<T>) = synchronized(StylesheetLock) { importStylesheet(stylesheetType) }

fun UIComponent.callOnDock() {
    onDock()
    onDockListeners?.forEach { it.invoke(this) }
}

fun UIComponent.callOnUndock() {
    onUndock()
    onUndockListeners?.forEach { it.invoke(this) }
}

fun Region.printSize(id: String) {
    printWidth(id)
    printHeight(id)
}

fun Region.printWidth(id: String) = printSize(id, "width", minWidthProperty(), maxWidthProperty(), prefWidthProperty(), widthProperty())
fun Region.printHeight(id: String) = printSize(id, "height", minHeightProperty(), maxHeightProperty(), prefHeightProperty(), heightProperty())

fun <S, T> TableColumnBase<S, T>.printWidth(id: String) {
    printSize(id, "width", minWidthProperty(), maxWidthProperty(), prefWidthProperty(), widthProperty())
}

fun Stage.printWidth(id: String) {
    printSize(id, "width", minWidthProperty(), maxWidthProperty(), null, widthProperty())
}

private fun printSize(
    id: String,
    sizeName: String,
    min: ObservableValue<Number>,
    max: ObservableValue<Number>,
    pref: ObservableValue<Number>?,
    actual: ObservableValue<Number>,
) {
    println("$id min-$sizeName = ${min.value}")
    println("$id max-$sizeName = ${max.value}")
    pref?.let { println("$id pref-$sizeName = ${it.value}") }
    println("$id $sizeName = ${actual.value}")

    min.printChanges("$id min-$sizeName")
    max.printChanges("$id max-$sizeName")
    pref?.printChanges("$id pref-$sizeName")
    actual.printChanges("$id $sizeName")
}

fun Region.padding(op: (InsetBuilder.() -> Unit)) {
    val builder = InsetBuilder(this)
    op(builder)
    padding = Insets(builder.top.toDouble(), builder.right.toDouble(), builder.bottom.toDouble(), builder.left.toDouble())
}

class InsetBuilder(region: Region) {
    var top: Number = region.padding.top
    var bottom: Number = region.padding.bottom
    var right: Number = region.padding.right
    var left: Number = region.padding.left
}

fun EventTarget.verticalSeparator(padding: Number? = 10, op: Separator.() -> Unit = {}) = separator(Orientation.VERTICAL, op).apply {
    padding?.let {
        padding { right = it; left = it }
    }
}

fun Node.mouseTransparentWhen(expr: () -> ObservableValue<Boolean>) {
    mouseTransparentProperty().cleanBind(expr())
}

inline fun <T : UIComponent> Parent.addComponent(component: T, f: T.() -> Unit = {}) {
    add(component.root)
    f(component)
}

fun Control.doNotConsumeMouseEvents() {
    val method = SkinBase::class.declaredMemberFunctions.find { it.name == "consumeMouseEvents" }!!
    method.isAccessible = true
    skinProperty().perform {
        if (it is SkinBase) {
            method.call(it, false)
        }
    }
}

fun Node.makeDraggable() {
    var mouseX = 0.0
    var mouseY = 0.0

    setOnMousePressed { event ->
        mouseX = event.sceneX
        mouseY = event.sceneY
    }

    setOnMouseDragged { event ->
        val deltaX = event.sceneX - mouseX
        val deltaY = event.sceneY - mouseY
        relocate(layoutX + deltaX, layoutY + deltaY)
        mouseX = event.sceneX
        mouseY = event.sceneY
    }
}

val Node.verticalScrollBar: ScrollBar?
    get() = lookupAll(".scroll-bar")
        .asSequence()
        .map { it as ScrollBar }
        .filter { it.orientation == Orientation.VERTICAL }
        .firstOrNull()

val Color.hex: String get() = JFXNodeUtils.colorToHex(this)
val String.color: Color get() = Color.valueOf(this)
fun Color.withOpacity(opacity: Double): Color = Color(red, green, blue, opacity)

val Node.boundsInScreen: Bounds get() = localToScreen(boundsInLocal)

fun Bounds.containsDelta(x: Double, y: Double, delta: Double): Boolean {
    return !isEmpty && x in (minX - delta)..(maxX + delta) && y in (minY - delta)..(maxY + delta)
}

inline fun debounce(millis: Long, crossinline f: () -> Unit): Job = JavaFxScope.launch {
    delay(millis)
    f()
}

inline fun Node.localShortcut(combination: String, crossinline action: () -> Unit) = localShortcut(KeyCombination.valueOf(combination), action)
inline fun Node.localShortcut(combination: KeyCombination, crossinline action: () -> Unit) {
    addEventFilter(KeyEvent.KEY_PRESSED) { e ->
        if (combination.match(e)) {
            action()
            e.consume()
        }
    }
}

fun UIComponent.localShortcut(button: ButtonBase, combination: String) = localShortcut(button, KeyCombination.valueOf(combination))
fun UIComponent.localShortcut(button: ButtonBase, combination: KeyCombination) {
    runLater {
        root.localShortcut(combination) { button.fire() }
    }
}

object JavaFxScope : CoroutineScope {
    override val coroutineContext = Dispatchers.Main.immediate
}