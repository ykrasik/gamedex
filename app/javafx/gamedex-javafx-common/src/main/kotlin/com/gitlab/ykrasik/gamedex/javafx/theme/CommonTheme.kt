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

package com.gitlab.ykrasik.gamedex.javafx.theme

import com.gitlab.ykrasik.gamedex.Platform
import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.control.Label
import tornadofx.addClass
import tornadofx.label
import tornadofx.toProperty

/**
 * User: ykrasik
 * Date: 05/01/2019
 * Time: 08:57
 */

val Platform.logo
    get() = when (this) {
        Platform.pc -> Icons.windows
        Platform.android -> Icons.android
        Platform.mac -> Icons.apple
        Platform.excluded -> Icons.folderRemove
        else -> kotlin.error("Unknown platform: $this")
    }.size(26)

inline fun EventTarget.header(
    text: String,
    graphic: Node? = null,
    crossinline op: Label.() -> Unit = {}
) = header(text.toProperty(), graphic?.toProperty(), op)

inline fun EventTarget.header(
    textProperty: ObservableValue<String>,
    graphic: ObservableValue<out Node>? = null,
    crossinline op: Label.() -> Unit = {}
) = label(textProperty) {
    addClass(CommonStyle.headerLabel)
    if (graphic != null) this.graphicProperty().bind(graphic)
    op(this)
}

fun Any?.toDisplayString() = this?.toString() ?: "NA"