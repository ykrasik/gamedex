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
import com.gitlab.ykrasik.gamedex.javafx.JavaFxState
import com.gitlab.ykrasik.gamedex.javafx.perform
import com.gitlab.ykrasik.gamedex.javafx.size
import com.gitlab.ykrasik.gamedex.util.IsValid
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.scene.Node
import javafx.scene.control.Control
import javafx.scene.control.Tooltip
import tornadofx.stringBinding
import tornadofx.tooltip

/**
 * User: ykrasik
 * Date: 09/12/2018
 * Time: 13:35
 */
fun Node.installTooltip(tooltip: Tooltip) {
    if (this is Control) {
        this.tooltip = tooltip
    } else {
        Tooltip.install(this, tooltip)
    }
}

fun Node.uninstallTooltip(tooltip: Tooltip) {
    if (this is Control) {
        this.tooltip = null
    } else {
        Tooltip.uninstall(this, tooltip)
    }
}

inline fun Node.tooltip(text: ObservableValue<String?>, graphic: Node? = null, crossinline op: Tooltip.() -> Unit = {}) = tooltip {
    textProperty().bind(text)
    this.graphic = graphic
    op()
}

inline fun Node.nullableTooltip(text: ObservableValue<String?>, graphic: Node? = null, crossinline op: Tooltip.() -> Unit = {}): Tooltip {
    val tooltip = Tooltip().apply {
        textProperty().bind(text)
        this.graphic = graphic
        op()
    }
    text.perform {
        if (it == null) {
            uninstallTooltip(tooltip)
            tooltip.hide()
        } else {
            installTooltip(tooltip)
        }
    }
    return tooltip
}

inline fun Node.errorTooltip(text: ObservableValue<String?>, crossinline op: Tooltip.() -> Unit = {}) =
    nullableTooltip(text, Icons.validationError.size(20), op)

fun Node.errorTooltip(state: JavaFxState<IsValid, SimpleObjectProperty<IsValid>>) =
    errorTooltip(state.property.stringBinding { it?.errorText() })