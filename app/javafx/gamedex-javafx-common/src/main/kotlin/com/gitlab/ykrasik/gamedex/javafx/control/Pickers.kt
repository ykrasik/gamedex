/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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

import com.gitlab.ykrasik.gamedex.javafx.bindBidirectional
import com.gitlab.ykrasik.gamedex.javafx.color
import com.gitlab.ykrasik.gamedex.javafx.hex
import com.gitlab.ykrasik.gamedex.javafx.typeSafeOnChange
import com.gitlab.ykrasik.gamedex.util.JodaLocalDate
import com.gitlab.ykrasik.gamedex.util.java
import com.gitlab.ykrasik.gamedex.util.joda
import com.jfoenix.controls.JFXColorPicker
import com.jfoenix.controls.JFXDatePicker
import com.jfoenix.skins.JFXColorPickerSkin
import javafx.beans.property.Property
import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.geometry.Insets
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import tornadofx.bind
import tornadofx.onChangeOnce
import tornadofx.opcr
import kotlin.reflect.jvm.isAccessible

/**
 * User: ykrasik
 * Date: 09/12/2018
 * Time: 14:11
 */
inline fun EventTarget.jfxDatePicker(op: JFXDatePicker.() -> Unit = {}): JFXDatePicker =
    opcr(this, JFXDatePicker(), op)

inline fun EventTarget.jfxDatePicker(property: Property<JodaLocalDate>, op: JFXDatePicker.() -> Unit = {}): JFXDatePicker =
    jfxDatePicker {
        // TODO: Bidirectional bindings didn't work here, for some reason.
        this.value = property.value.java
        valueProperty().typeSafeOnChange { property.value = it.joda }
        op()
    }

inline fun EventTarget.jfxColorPicker(op: JFXColorPicker.() -> Unit = {}) =
    opcr(this, JFXColorPicker()) {
        skinProperty().onChangeOnce {
            setDefaultBackground()
        }
        op()
    }

inline fun EventTarget.jfxColorPicker(value: ObservableValue<Color>, op: JFXColorPicker.() -> Unit = {}) =
    jfxColorPicker { bind(value) }.also(op)

inline fun EventTarget.jfxColorPicker(property: Property<String>, op: JFXColorPicker.() -> Unit = {}) =
    jfxColorPicker {
        property.bindBidirectional(valueProperty(), Color::hex, String::color)
        op()
    }

// Fix for bug in JfxColorPicker that doesn't display the initial color value.
fun JFXColorPicker.setDefaultBackground() {
    val colorBoxField = JFXColorPickerSkin::class.members.find { it.name == "colorBox" }!!
    colorBoxField.isAccessible = true
    val colorBox = colorBoxField.call(skin as JFXColorPickerSkin) as Pane
    colorBox.background = Background(BackgroundFill(Color.WHITE, CornerRadii(3.0), Insets.EMPTY))
}