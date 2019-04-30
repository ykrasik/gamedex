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

import com.gitlab.ykrasik.gamedex.javafx.theme.GameDexStyle
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import tornadofx.addClass
import tornadofx.hgrow
import tornadofx.useMaxWidth

/**
 * User: ykrasik
 * Date: 23/12/2018
 * Time: 17:12
 */

inline fun EventTarget.customToolbar(spacing: Number = 10, crossinline op: HBox.() -> Unit) = defaultHbox(spacing) {
    addClass(GameDexStyle.customToolbar)
    useMaxWidth = true
    hgrow = Priority.ALWAYS
    alignment = Pos.CENTER_LEFT
    op()
}