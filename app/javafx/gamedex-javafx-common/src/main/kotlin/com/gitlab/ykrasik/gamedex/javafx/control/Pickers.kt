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

import com.gitlab.ykrasik.gamedex.javafx.mapBidirectional
import com.gitlab.ykrasik.gamedex.util.JavaLocalDate
import com.gitlab.ykrasik.gamedex.util.JodaLocalDate
import com.gitlab.ykrasik.gamedex.util.toJava
import com.gitlab.ykrasik.gamedex.util.toJoda
import com.jfoenix.controls.JFXColorPicker
import com.jfoenix.controls.JFXDatePicker
import javafx.beans.property.Property
import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.scene.paint.Color
import tornadofx.bind
import tornadofx.opcr

/**
 * User: ykrasik
 * Date: 09/12/2018
 * Time: 14:11
 */
inline fun EventTarget.jfxDatePicker(op: JFXDatePicker.() -> Unit = {}): JFXDatePicker =
    opcr(this, JFXDatePicker(), op)

inline fun EventTarget.jfxDatePicker(value: JavaLocalDate, op: JFXDatePicker.() -> Unit = {}): JFXDatePicker =
    jfxDatePicker {
        this.value = value
        op()
    }

inline fun EventTarget.jfxDatePicker(value: ObservableValue<JavaLocalDate>, op: JFXDatePicker.() -> Unit = {}): JFXDatePicker =
    jfxDatePicker { bind(value) }.also(op)

inline fun EventTarget.jfxDatePicker(property: Property<JodaLocalDate>, op: JFXDatePicker.() -> Unit = {}): JFXDatePicker =
    jfxDatePicker(property.mapBidirectional({ toJava() }, { toJoda() }), op)

inline fun EventTarget.jfxColorPicker(op: JFXColorPicker.() -> Unit = {}) =
    opcr(this, JFXColorPicker(), op)

inline fun EventTarget.jfxColorPicker(value: ObservableValue<Color>, op: JFXColorPicker.() -> Unit = {}) =
    jfxColorPicker { bind(value) }.also(op)

inline fun EventTarget.jfxColorPicker(property: Property<String>, op: JFXColorPicker.() -> Unit = {}) =
    jfxColorPicker(property.mapBidirectional(Color::valueOf, Any::toString), op)