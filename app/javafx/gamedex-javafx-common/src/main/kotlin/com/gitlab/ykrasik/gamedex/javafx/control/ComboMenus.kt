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
import com.jfoenix.controls.JFXButton
import javafx.beans.property.Property
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.layout.VBox
import tornadofx.action
import tornadofx.replaceChildren
import tornadofx.stringBinding
import tornadofx.useMaxWidth
import kotlin.toString

/**
 * User: ykrasik
 * Date: 09/12/2018
 * Time: 13:23
 */
inline fun <T> EventTarget.popoverComboMenu(
    possibleItems: List<T>,
    selectedItemProperty: Property<out T?>,
    noinline text: ((T) -> String)? = Any?::toString,
    noinline graphic: ((T) -> Node?)? = null,
    buttonGraphic: Node? = null,
    crossinline itemOp: JFXButton.() -> Unit = {},
    menuOp: PopOverContent.(T) -> Unit = {}
) = buttonWithPopover {
    possibleItems.forEach { item ->
//        customListView(possibleItems.observable()) {
//            customListCell { item ->
//                this.text = if (text != null) text(item) else null
//                this.graphic = if (graphic != null) graphic(item) else null
//            }
//            selectionModel.selectedItemProperty().onChange { selectedItemProperty.value = it }
//        }
        jfxButton(text?.invoke(item), graphic?.invoke(item), alignment = Pos.CENTER_LEFT) {
            useMaxWidth = true
            action { selectedItemProperty.value = item }
            itemOp()
        }
        menuOp(item)
    }
}.apply {
    if (text != null) textProperty().bind(selectedItemProperty.stringBinding { it?.let(text) })
    when {
        buttonGraphic != null -> this.graphic = buttonGraphic
        graphic != null -> graphicProperty().bind(selectedItemProperty.binding { it?.let(graphic) })
    }
}

inline fun <T> EventTarget.popoverDynamicComboMenu(
    possibleItems: ObservableList<T>,
    selectedItemProperty: Property<T>,
    noinline text: ((T) -> String)? = Any?::toString,
    noinline graphic: ((T) -> Node?)? = null,
    buttonGraphic: Node? = null,
    crossinline itemOp: JFXButton.() -> Unit = {},
    crossinline menuOp: VBox.(T) -> Unit = {}
) = buttonWithPopover {
    possibleItems.perform { items ->
        replaceChildren {
            items.forEach { item ->
                jfxButton(text?.invoke(item), graphic?.invoke(item), alignment = Pos.CENTER_LEFT) {
                    useMaxWidth = true
                    action { selectedItemProperty.value = item }
                    itemOp()
                }
                menuOp(item)
            }
        }
    }
}.apply {
    if (text != null) textProperty().bind(selectedItemProperty.stringBinding { it?.let(text) })
    when {
        buttonGraphic != null -> this.graphic = buttonGraphic
        graphic != null -> graphicProperty().bind(selectedItemProperty.binding { it?.let(graphic) })
    }
}

inline fun <reified T : Enum<T>> EventTarget.enumComboMenu(
    selectedItemProperty: Property<out T?>,
    noinline text: ((T) -> String)? = Any?::toString,
    noinline graphic: ((T) -> Node)? = null,
    buttonGraphic: Node? = null,
    menuOp: PopOverContent.(T) -> Unit = {}
) = popoverComboMenu(
    possibleItems = T::class.java.enumConstants.asList(),
    selectedItemProperty = selectedItemProperty,
    text = text,
    graphic = graphic,
    buttonGraphic = buttonGraphic,
    menuOp = menuOp
)

fun EventTarget.platformComboBox(selected: Property<out Platform?>) = enumComboMenu(
    selectedItemProperty = selected,
    text = Platform::displayName,
    graphic = { it.logo }
)