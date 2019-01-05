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

import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.javafx.theme.minusButton
import com.gitlab.ykrasik.gamedex.javafx.theme.plusButton
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.gitlab.ykrasik.gamedex.util.Try
import com.gitlab.ykrasik.gamedex.util.and
import com.jfoenix.controls.JFXTextField
import com.jfoenix.skins.JFXTextFieldSkin
import com.jfoenix.skins.ValidationPane
import com.jfoenix.validation.base.ValidatorBase
import javafx.animation.FadeTransition
import javafx.beans.binding.ObjectBinding
import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.StringProperty
import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.control.Tooltip
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import org.controlsfx.control.textfield.CustomTextField
import tornadofx.*
import java.math.BigDecimal

/**
 * User: ykrasik
 * Date: 09/12/2018
 * Time: 13:26
 */
inline fun EventTarget.jfxTextField(
    text: String? = null,
    promptText: String? = null,
    isLabelFloat: Boolean = false,
    op: JFXTextField.() -> Unit = {}
): JFXTextField = opcr(this, JFXTextField()) {
    this.text = text
    this.isLabelFloat = isLabelFloat
    this.promptText = promptText
    op()
}

inline fun EventTarget.jfxTextField(
    property: ObservableValue<String>,
    promptText: String? = null,
    isLabelFloat: Boolean = false,
    op: JFXTextField.() -> Unit = {}
): JFXTextField = jfxTextField(promptText = promptText, isLabelFloat = isLabelFloat) {
    bind(property)
    op()
}

inline fun <reified T : Number> EventTarget.numberTextField(
    property: Property<T>,
    min: Number,
    max: Number,
    withButtons: Boolean = true,
    crossinline op: JFXTextField.() -> Unit = {}
): ObjectBinding<Try<T>> {
    val (parse: (String) -> T, stringify: (Double) -> String) = when (T::class) {
        Number::class -> Pair({ s: String -> s.toDouble() as T }, { d: Double -> d.toString() })
        BigDecimal::class -> Pair({ s -> BigDecimal(s) as T }, { d -> d.toString() })
        else -> when (T::class.javaPrimitiveType) {
            Int::class.javaPrimitiveType -> Pair({ s -> s.toInt() as T }, { d -> d.toInt().toString() })
            Long::class.javaPrimitiveType -> Pair({ s -> s.toLong() as T }, { d -> d.toLong().toString() })
            Double::class.javaPrimitiveType -> Pair({ s -> s.toDouble() as T }, { d -> d.toString() })
            else -> throw IllegalArgumentException("Unsupported type: ${T::class}")
        }
    }

    lateinit var value: ObjectBinding<Try<T>>
    defaultHbox(spacing = 2) {
        val minusButton = if (withButtons) minusButton() else null
        val textfield = jfxTextField(stringify(property.value.toDouble()), promptText = "Enter Value...") {
            alignment = Pos.CENTER
            op()
        }
        val plusButton = if (withButtons) plusButton() else null

        value = textfield.bindParser {
            try {
                val attempt = parse(it)
                check(min.toDouble() <= attempt.toDouble() && attempt.toDouble() <= max.toDouble())
                attempt
            } catch (_: Exception) {
                kotlin.error("Invalid value!")
            }
        }
        value.typeSafeOnChange {
            it.valueOrNull?.let { property.value = it }
        }

        minusButton?.run {
            enableWhen(value.binding { value ->
                val canDecrement = IsValid {
                    check(property.value.toDouble() - 1 >= min.toDouble()) { "Limit reached!" }
                }
                value!!.and(canDecrement)
            })
            action { textfield.text = stringify(parse(textfield.text).toDouble() - 1) }
        }
        plusButton?.run {
            enableWhen(value.binding { value ->
                val canIncrement = IsValid {
                    check(property.value.toDouble() + 1 <= max.toDouble()) { "Limit reached!" }
                }
                value!!.and(canIncrement)
            })
            action { textfield.text = stringify(parse(textfield.text).toDouble() + 1) }
        }
    }
    return value
}

inline fun EventTarget.searchTextField(component: UIComponent, textProperty: StringProperty, op: CustomTextField.() -> Unit = {}) = clearableTextField(textProperty) {
    promptText = "Search"
    left = Icons.search
    tooltip("Ctrl+f")
    component.shortcut("ctrl+f") { requestFocus() }
    op()
}

inline fun EventTarget.clearableTextField(textProperty: StringProperty, op: CustomTextField.() -> Unit = {}) = opcr(this, CustomTextField()) {
    useMaxWidth = true
    alignment = Pos.CENTER_LEFT
    textProperty.bindBidirectional(textProperty())

    val clearButton = jfxButton {
        addClass(CommonStyle.customHoverable)
        isCancelButton = true
        opacity = 0.0
        showWhen { editableProperty() }
        graphicProperty().bind(hoverProperty().objectBinding {
            (if (it!!) Icons.closeCircle else Icons.close).size(16).color(Color.BLACK)
        })
        action { clear() }
    }

    right = StackPane().apply {
        padding {
            top = 4
            bottom = 3
        }
        add(clearButton)
    }

    val fader = FadeTransition(350.millis, clearButton)
    fader.cycleCount = 1

    val setButtonVisible = { visible: Boolean ->
        fader.fromValue = if (visible) 0.0 else 1.0
        fader.toValue = if (visible) 1.0 else 0.0
        fader.play()
    }

    textProperty().onChange {
        val isTextEmpty = text.isNullOrEmpty()
        val isButtonVisible = fader.node.opacity > 0

        if (isTextEmpty && isButtonVisible) {
            setButtonVisible(false)
        } else if (!isTextEmpty && !isButtonVisible) {
            setButtonVisible(true)
        }
    }

    op()
}

inline fun <T> JFXTextField.bindParser(crossinline parser: (String) -> T): ObjectBinding<Try<T>> {
    val value = textProperty().binding { text -> Try { parser(text ?: "") } }
    validWhen(value)
    return value
}

fun JFXTextField.validWhen(isValid: JavaFxState<IsValid, SimpleObjectProperty<IsValid>>): Unit = validWhen(isValid.property)
fun <T> JFXTextField.validWhen(isValid: ObservableValue<Try<T>>) {
    skinProperty().onChangeOnce {
        // The jfx ValidationPane causes our tooltip to be displayed incorrectly.
        (it as JFXTextFieldSkin<*>).getChildren().removeAll { it is ValidationPane<*> }
    }

    val errorProperty = isValid.stringBinding { it?.errorOrNull?.message }

    val tooltip = Tooltip().apply {
        textProperty().bind(errorProperty)
        graphic = Icons.validationError.size(20)
    }

    fun showTooltip() {
        if (tooltip.isShowing) return
        val bounds = localToScreen(boundsInLocal)
        if (bounds != null) {
            tooltip.show(this, bounds.minX + 5, bounds.maxY)
        }
    }

    focusedProperty().onChange { focused ->
        if (focused && isValid.value.isError) showTooltip() else tooltip.hide()
    }

    isValid.typeSafeOnChange {
        validate()
        if (it.isError) {
            this.tooltip = tooltip
            if (isFocused) {
                showTooltip()
            }
        } else {
            this.tooltip = null
            tooltip.hide()
        }
    }

    validators += object : ValidatorBase() {
        override fun eval() {
            hasErrors.set(isValid.value.isError)
            setMessage(null)
        }
    }
}
