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

import com.gitlab.ykrasik.gamedex.javafx.Icons
import com.gitlab.ykrasik.gamedex.javafx.perform
import com.gitlab.ykrasik.gamedex.javafx.size
import javafx.beans.value.ObservableValue
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Tooltip
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import tornadofx.onChange
import tornadofx.tooltip

/**
 * User: ykrasik
 * Date: 09/12/2018
 * Time: 13:35
 */
inline fun Node.tooltip(text: ObservableValue<String?>, graphic: Node? = null, crossinline op: Tooltip.() -> Unit = {}) = tooltip {
    textProperty().bind(text)
    this.graphic = graphic
    op(this)
}

inline fun Node.errorTooltip(text: ObservableValue<String?>, crossinline op: Tooltip.() -> Unit = {}) =
    tooltip(text, Icons.validationError.size(20), op)

inline fun Node.wrapInErrorTooltip(text: ObservableValue<String?>, crossinline op: Tooltip.() -> Unit = {}) {
    val wrapper = HBox().apply {
        alignment = Pos.CENTER_LEFT
        val tooltip = errorTooltip(text, op)
        text.onChange {
            if (it == null) tooltip.hide()
        }
    }
    parentProperty().perform {
        if (it !is Pane || it == wrapper) return@perform

        val index = it.children.indexOf(this)
        it.children.removeAt(index)
        wrapper.children += this
        it.children.add(index, wrapper)
    }
}