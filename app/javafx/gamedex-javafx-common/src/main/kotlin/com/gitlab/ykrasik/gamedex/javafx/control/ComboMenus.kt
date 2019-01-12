/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.javafx.binding
import com.gitlab.ykrasik.gamedex.javafx.perform
import com.gitlab.ykrasik.gamedex.javafx.theme.logo
import javafx.beans.property.Property
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.layout.VBox
import org.controlsfx.control.PopOver
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/12/2018
 * Time: 13:23
 */
inline fun <T> EventTarget.popoverComboMenu(
    possibleItems: List<T>,
    selectedItemProperty: Property<T>,
    arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
    itemStyleClass: CssRule? = null,
    noinline text: ((T) -> String)? = Any?::toString,
    noinline graphic: ((T) -> Node?)? = null,
    menuOp: PopOverContent.(T) -> Unit = {}
) = buttonWithPopover(arrowLocation = arrowLocation) {
    possibleItems.forEach { item ->
        jfxButton(text?.invoke(item), graphic?.invoke(item), alignment = Pos.CENTER_LEFT) {
            if (itemStyleClass != null) addClass(itemStyleClass)
            useMaxWidth = true
            action { selectedItemProperty.value = item }
        }
        menuOp(item)
    }
}.apply {
    if (text != null) textProperty().bind(selectedItemProperty.stringBinding { text(it!!) })
    if (graphic != null) graphicProperty().bind(selectedItemProperty.binding { graphic(it) })
}

inline fun <T> EventTarget.popoverComboMenu(
    possibleItems: ObservableList<T>,
    selectedItemProperty: Property<T>,
    arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
    itemStyleClass: CssRule? = null,
    noinline text: ((T) -> String)? = null,
    noinline graphic: ((T) -> Node?)? = null,
    crossinline menuOp: VBox.(T) -> Unit = {}
) = buttonWithPopover(arrowLocation = arrowLocation) {
    possibleItems.perform { items ->
        replaceChildren {
            items.forEach { item ->
                jfxButton(text?.invoke(item), graphic?.invoke(item), alignment = Pos.CENTER_LEFT) {
                    if (itemStyleClass != null) addClass(itemStyleClass)
                    useMaxWidth = true
                    action { selectedItemProperty.value = item }
                }
                menuOp(item)
            }
        }
    }
}.apply {
    if (text != null) textProperty().bind(selectedItemProperty.stringBinding { if (it != null) text(it) else null })
    if (graphic != null) graphicProperty().bind(selectedItemProperty.binding { if (it != null) graphic(it) else null })
}

inline fun <reified T : Enum<T>> EventTarget.enumComboMenu(
    selectedItemProperty: Property<T>,
    arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
    itemStyleClass: CssRule? = null,
    noinline text: ((T) -> String)? = Any?::toString,
    noinline graphic: ((T) -> Node)? = null,
    menuOp: PopOverContent.(T) -> Unit = {}
) = popoverComboMenu(
    possibleItems = T::class.java.enumConstants.asList(),
    selectedItemProperty = selectedItemProperty,
    arrowLocation = arrowLocation,
    itemStyleClass = itemStyleClass,
    text = text,
    graphic = graphic,
    menuOp = menuOp
)

fun EventTarget.platformComboBox(selected: Property<Platform>) = enumComboMenu(
    selectedItemProperty = selected,
    text = Platform::displayName,
    graphic = { it.logo }
)