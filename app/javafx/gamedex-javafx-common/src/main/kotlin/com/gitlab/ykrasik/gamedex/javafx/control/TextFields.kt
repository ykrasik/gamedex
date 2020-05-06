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

import com.gitlab.ykrasik.gamedex.javafx.JavaFxObjectStatefulChannel
import com.gitlab.ykrasik.gamedex.javafx.binding
import com.gitlab.ykrasik.gamedex.javafx.padding
import com.gitlab.ykrasik.gamedex.javafx.theme.*
import com.gitlab.ykrasik.gamedex.javafx.typeSafeOnChange
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.gitlab.ykrasik.gamedex.util.Try
import com.gitlab.ykrasik.gamedex.util.and
import com.jfoenix.controls.JFXTextField
import com.jfoenix.skins.JFXTextFieldSkin
import com.jfoenix.skins.PromptLinesWrapper
import com.jfoenix.skins.ValidationPane
import com.jfoenix.validation.base.ValidatorBase
import com.sun.javafx.scene.text.HitInfo
import javafx.animation.FadeTransition
import javafx.beans.binding.ObjectBinding
import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.StringProperty
import javafx.beans.value.ObservableValue
import javafx.css.PseudoClass
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Tooltip
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import tornadofx.*
import java.math.BigDecimal
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

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
                val canDecrement = Try {
                    check(property.value.toDouble() - 1 >= min.toDouble()) { "Limit reached!" }
                }
                value!! and canDecrement
            })
            action { textfield.text = stringify(parse(textfield.text).toDouble() - 1) }
        }
        plusButton?.run {
            enableWhen(value.binding { value ->
                val canIncrement = Try {
                    check(property.value.toDouble() + 1 <= max.toDouble()) { "Limit reached!" }
                }
                value!! and canIncrement
            })
            action { textfield.text = stringify(parse(textfield.text).toDouble() + 1) }
        }
    }
    return value
}

inline fun EventTarget.searchTextField(
    component: UIComponent,
    textProperty: StringProperty,
    promptText: String = "Search",
    op: CustomJFXTextField.() -> Unit = {}
) = clearableTextField(textProperty) {
    prefWidth = 300.0
    this.promptText = promptText
    left = Icons.search.size(20)
    tooltip("Ctrl+f")
    component.shortcut("ctrl+f") { requestFocus() }
    op()
}

inline fun EventTarget.clearableTextField(textProperty: StringProperty, op: CustomJFXTextField.() -> Unit = {}) = customTextField(textProperty) {
    useMaxWidth = true

    val clearButton = jfxButton {
        addClass(GameDexStyle.clearButton)
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

    addEventFilter(KeyEvent.KEY_PRESSED) { e ->
        if (e.code == KeyCode.ESCAPE) {
            clear()
            clearButton.requestFocus()
            e.consume()
        }
    }

    op()
}

inline fun EventTarget.customTextField(text: String = "", op: CustomJFXTextField.() -> Unit = {}): CustomJFXTextField =
    customTextField(text.toProperty(), op)

inline fun EventTarget.customTextField(text: Property<String>, op: CustomJFXTextField.() -> Unit = {}): CustomJFXTextField =
    opcr(this, CustomJFXTextField()) {
        alignment = Pos.CENTER_LEFT
        textProperty().bindBidirectional(text)
        op()
    }

/**
 * A copy of [com.jfoenix.controls.JFXTextField] which allows setting a left & right node, like [org.controlsfx.control.textfield.CustomTextField]
 */
class CustomJFXTextField : JFXTextField() {
    val leftProperty = SimpleObjectProperty<Node>(this, "left") //$NON-NLS-1$
    var left: Node? by leftProperty

    val rightProperty = SimpleObjectProperty<Node>(this, "right") //$NON-NLS-1$
    var right: Node? by rightProperty

    override fun createDefaultSkin() = CustomTextFieldSkin()

    inner class CustomTextFieldSkin : JFXTextFieldSkin<CustomJFXTextField>(this) {
        private var leftPane: StackPane? = null
        private var rightPane: StackPane? = null

        private val linesWrapper = JFXTextFieldSkin::class.declaredMemberProperties.find { it.name == "linesWrapper" }!!.apply {
            isAccessible = true
        }.get(this) as PromptLinesWrapper<*>

        init {
            updateChildren()

            registerChangeListener(leftProperty, "LEFT_NODE") //$NON-NLS-1$
            registerChangeListener(rightProperty, "RIGHT_NODE") //$NON-NLS-1$
            registerChangeListener(focusedProperty(), "FOCUSED") //$NON-NLS-1$
        }

        override fun handleControlPropertyChanged(p: String?) {
            super.handleControlPropertyChanged(p)

            if (p === "LEFT_NODE" || p === "RIGHT_NODE") { //$NON-NLS-1$ //$NON-NLS-2$
                updateChildren()
            }
        }

        private fun updateChildren() {
            val newLeft = left
            if (newLeft != null) {
                children.remove(leftPane)
                leftPane = StackPane(newLeft)
                leftPane!!.alignment = Pos.CENTER_LEFT
                leftPane!!.styleClass.add("left-pane") //$NON-NLS-1$
                children.add(leftPane)
                left = newLeft
            }

            val newRight = right
            if (newRight != null) {
                children.remove(rightPane)
                rightPane = StackPane(newRight)
                rightPane!!.alignment = Pos.CENTER_RIGHT
                rightPane!!.styleClass.add("right-pane") //$NON-NLS-1$
                children.add(rightPane)
                right = newRight
            }

            pseudoClassStateChanged(HAS_LEFT_NODE, left != null)
            pseudoClassStateChanged(HAS_RIGHT_NODE, right != null)
            pseudoClassStateChanged(HAS_NO_SIDE_NODE, left == null && right == null)
        }

        override fun layoutChildren(x: Double, y: Double, w: Double, h: Double) {
            val fullHeight = h + snappedTopInset() + snappedBottomInset()

            val leftWidth = if (leftPane == null) 0.0 else snapSize(leftPane!!.prefWidth(fullHeight))
            val rightWidth = if (rightPane == null) 0.0 else snapSize(rightPane!!.prefWidth(fullHeight))

            val textFieldStartX = snapPosition(x) + snapSize(leftWidth)
            val textFieldWidth = w - snapSize(leftWidth) - snapSize(rightWidth)

            super.layoutChildren(textFieldStartX, 0.0, textFieldWidth, fullHeight)
            linesWrapper.focusedLine.resizeRelocate(x, skinnable.height, w, linesWrapper.focusedLine.prefHeight(-1.0))
            linesWrapper.line.resizeRelocate(x, skinnable.height, w, linesWrapper.line.prefHeight(-1.0))

            if (leftPane != null) {
                val leftStartX = 0.0
                leftPane!!.resizeRelocate(leftStartX, 0.0, leftWidth, fullHeight)
            }

            if (rightPane != null) {
                val rightStartX = if (rightPane == null) 0.0 else w - rightWidth + snappedLeftInset()
                rightPane!!.resizeRelocate(rightStartX, 0.0, rightWidth, fullHeight)
            }
        }

        override fun getIndex(x: Double, y: Double): HitInfo {
            /**
             * This resolves https://bitbucket.org/controlsfx/controlsfx/issue/476
             * when we have a left Node and the click point is badly returned
             * because we weren't considering the shift induced by the leftPane.
             */
            val leftWidth = if (leftPane == null) 0.0 else snapSize(leftPane!!.prefWidth(skinnable.height))
            return super.getIndex(x - leftWidth, y)
        }

        override fun computePrefWidth(h: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
            val pw = super.computePrefWidth(h, topInset, rightInset, bottomInset, leftInset)
            val leftWidth = if (leftPane == null) 0.0 else snapSize(leftPane!!.prefWidth(h))
            val rightWidth = if (rightPane == null) 0.0 else snapSize(rightPane!!.prefWidth(h))

            return pw + leftWidth + rightWidth
        }

        override fun computePrefHeight(w: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
            val ph = super.computePrefHeight(w, topInset, rightInset, bottomInset, leftInset)
            val leftHeight = if (leftPane == null) 0.0 else snapSize(leftPane!!.prefHeight(-1.0))
            val rightHeight = if (rightPane == null) 0.0 else snapSize(rightPane!!.prefHeight(-1.0))

            return Math.max(ph, Math.max(leftHeight, rightHeight))
        }


        private val HAS_NO_SIDE_NODE = PseudoClass.getPseudoClass("no-side-nodes") //$NON-NLS-1$
        private val HAS_LEFT_NODE = PseudoClass.getPseudoClass("left-node-visible") //$NON-NLS-1$
        private val HAS_RIGHT_NODE = PseudoClass.getPseudoClass("right-node-visible") //$NON-NLS-1$
    }
}


inline fun <T> JFXTextField.bindParser(crossinline parser: (String) -> T): ObjectBinding<Try<T>> {
    val value = textProperty().binding { text -> Try { parser(text ?: "") } }
    validWhen(value)
    return value
}

fun JFXTextField.validWhen(isValid: JavaFxObjectStatefulChannel<IsValid>): Unit = validWhen(isValid.property)
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