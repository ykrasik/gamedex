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

import com.gitlab.ykrasik.gamedex.javafx.binding
import com.gitlab.ykrasik.gamedex.javafx.forEachWith
import com.gitlab.ykrasik.gamedex.javafx.theme.minusButton
import com.gitlab.ykrasik.gamedex.javafx.theme.plusButton
import com.gitlab.ykrasik.gamedex.javafx.typeSafeOnChange
import com.gitlab.ykrasik.gamedex.javafx.typesafeStringBinding
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.gitlab.ykrasik.gamedex.util.asPercent
import com.gitlab.ykrasik.gamedex.util.roundBy
import com.jfoenix.controls.JFXSlider
import javafx.beans.property.Property
import javafx.event.EventTarget
import javafx.geometry.Orientation
import tornadofx.action
import tornadofx.label
import tornadofx.opcr

/**
 * User: ykrasik
 * Date: 09/12/2018
 * Time: 13:28
 */
inline fun EventTarget.jfxSlider(
    orientation: Orientation = Orientation.HORIZONTAL,
    indicatorPosition: JFXSlider.IndicatorPosition = JFXSlider.IndicatorPosition.LEFT,
    crossinline op: JFXSlider.() -> Unit = {},
): JFXSlider = opcr(this, JFXSlider()) {
    this.orientation = orientation
    this.indicatorPosition = indicatorPosition
    op()
}

inline fun EventTarget.jfxSlider(
    property: Property<out Number>,
    min: Number,
    max: Number,
    conflateValueChanges: Boolean = true,
    crossinline valueProcess: (Number) -> Number = { it },
    crossinline op: JFXSlider.() -> Unit = {},
) = jfxSlider {
    this.min = min.toDouble()
    this.max = max.toDouble()
    this.value = property.value.toDouble()

    var ignoreNextValueChange = false
    property.typeSafeOnChange {
        if (!ignoreNextValueChange) {
            this.value = it.toDouble()
        }
    }
    valueProperty().forEachWith(valueChangingProperty()) { currentValue, changing ->
        if (!changing || !conflateValueChanges) {
            ignoreNextValueChange = true
            property.value = valueProcess(currentValue)
            ignoreNextValueChange = false
        }
    }
    op()
}

inline fun EventTarget.plusMinusSlider(
    property: Property<out Number>,
    min: Number = 0,
    max: Number = Double.MAX_VALUE,
    step: Double = 1.0,
    conflateValueChanges: Boolean = true,
    noinline valueDisplay: ((Double) -> String)? = null,
    crossinline op: JFXSlider.() -> Unit = {},
) = defaultHbox(spacing = 2) {
    val minusButton = minusButton()
    val slider = jfxSlider(property, min, max, conflateValueChanges = conflateValueChanges, valueProcess = { it.toDouble().roundBy(step) }) {
        if (valueDisplay != null) {
            setValueFactory {
                valueProperty().typesafeStringBinding {
                    valueDisplay(it.toDouble())
                }
            }
        }
        op()
    }
    label(slider.valueProperty().typesafeStringBinding {
        val value = it.toDouble().roundBy(step)
        valueDisplay?.invoke(value) ?: value.toInt().toString()
    })
    val plusButton = plusButton()

    with(minusButton) {
        enableWhen(slider.valueProperty().binding { value ->
            IsValid {
                check(value.toDouble() - step >= min.toDouble()) { "Limit reached!" }
            }
        })
        action { slider.value = slider.value - step }
    }

    with(plusButton) {
        enableWhen(slider.valueProperty().binding { value ->
            IsValid {
                check(value.toDouble() + step <= max.toDouble()) { "Limit reached!" }
            }
        })
        action { slider.value = slider.value + step }
    }
}

inline fun EventTarget.percentSlider(
    property: Property<Double>,
    min: Double = 0.0,
    max: Double = 1.0,
    conflateValueChanges: Boolean = true,
    crossinline op: JFXSlider.() -> Unit = {},
) = plusMinusSlider(
    property,
    min,
    max,
    step = 0.01,
    conflateValueChanges = conflateValueChanges,
    valueDisplay = { it.asPercent() },
    op = op
)