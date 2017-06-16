package com.gitlab.ykrasik.gamedex.ui.widgets

import com.gitlab.ykrasik.gamedex.ui.jfxButton
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import com.jfoenix.controls.JFXButton
import javafx.beans.property.Property
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.event.EventTarget
import tornadofx.*

/**
 * User: ykrasik
 * Date: 05/06/2017
 * Time: 11:41
 */
inline fun <reified T : Number> EventTarget.adjustableTextField(property: Property<T>, name: String, min: T, max: T) {
    val (parse, stringify) = when(T::class) {
        Number::class ->  Pair({ s: String -> s.toDouble() as T }, { d: Double -> d.toString() })
        else -> when (T::class.javaPrimitiveType) {
            Int::class.javaPrimitiveType -> Pair({ s -> s.toInt() as T }, { d -> d.toInt().toString() })
            Long::class.javaPrimitiveType -> Pair({ s -> s.toLong() as T }, { d -> d.toLong().toString() })
            Double::class.javaPrimitiveType -> Pair({ s -> s.toDouble() as T }, { d -> d.toString() })
            else -> error("Unsupported type: ${T::class}")
        }
    }
    val textStringProperty = SimpleStringProperty(stringify(property.value.toDouble()))
    textStringProperty.onChange {
        // TODO: For some really strange reason, doing textStringProperty.map { ... } doesn't always fire change events.
        property.value = parse(it!!)
    }

    val viewModel = AdjustableTextfieldViewModel(textStringProperty)
    viewModel.textProperty.onChange { viewModel.commit() }
    val textfield = textfield(viewModel.textProperty) {
        validator {
            val valid = try {
                val value = parse(it!!).toDouble()
                !(value < min.toDouble() || value > max.toDouble())
            } catch (e: Exception) {
                false
            }
            if (!valid) error("Invalid $name value!") else null
        }

    }

    jfxButton(graphic = Theme.Icon.plus(20.0), type = JFXButton.ButtonType.RAISED) {
        val canUse = property.booleanBinding { it!!.toDouble() + 1 <= max.toDouble() }
        enableWhen { viewModel.valid.and(canUse) }
        setOnAction { textfield.text = stringify(parse(textfield.text).toDouble() + 1) }
    }

    jfxButton(graphic = Theme.Icon.minus(20.0), type = JFXButton.ButtonType.RAISED) {
        val canUse = property.booleanBinding { it!!.toDouble() - 1 >= min.toDouble() }
        enableWhen { viewModel.valid.and(canUse) }
        setOnAction { textfield.text = stringify(parse(textfield.text).toDouble() - 1) }
    }

    viewModel.validate(decorateErrors = true)
}

class AdjustableTextfieldViewModel(p: StringProperty) : ViewModel() {
    val textProperty = bind { p }
    var text by textProperty
}