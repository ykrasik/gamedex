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

import com.gitlab.ykrasik.gamedex.javafx.Icons
import com.gitlab.ykrasik.gamedex.javafx.control.PopOverContent
import com.gitlab.ykrasik.gamedex.javafx.control.buttonWithPopover
import com.gitlab.ykrasik.gamedex.javafx.control.jfxButton
import com.gitlab.ykrasik.gamedex.javafx.size
import com.jfoenix.controls.JFXButton
import javafx.event.EventTarget
import javafx.scene.Node
import org.controlsfx.control.PopOver
import tornadofx.addClass
import tornadofx.removeClass
import tornadofx.tooltip

/**
 * User: ykrasik
 * Date: 05/01/2019
 * Time: 08:50
 */

inline fun EventTarget.toolbarButton(text: String? = null, graphic: Node? = null, crossinline op: JFXButton.() -> Unit = {}) =
    jfxButton(text, graphic) {
        addClass(CommonStyle.toolbarButton)
        op()
    }

inline fun EventTarget.confirmButton(text: String? = null, graphic: Node? = null, crossinline op: JFXButton.() -> Unit = {}) =
    toolbarButton(text, graphic) {
        addClass(CommonStyle.confirmButton)
        op()
    }

inline fun EventTarget.warningButton(text: String? = null, graphic: Node? = null, crossinline op: JFXButton.() -> Unit = {}) =
    toolbarButton(text, graphic) {
        addClass(CommonStyle.warningButton)
        op()
    }

inline fun EventTarget.dangerButton(text: String? = null, graphic: Node? = null, crossinline op: JFXButton.() -> Unit = {}) =
    toolbarButton(text, graphic) {
        addClass(CommonStyle.dangerButton)
        op()
    }

inline fun EventTarget.infoButton(text: String? = null, graphic: Node? = null, crossinline op: JFXButton.() -> Unit = {}) =
    toolbarButton(text, graphic) {
        addClass(CommonStyle.infoButton)
        op()
    }

inline fun EventTarget.acceptButton(text: String? = null, crossinline op: JFXButton.() -> Unit = {}) =
    confirmButton(text, Icons.accept) {
        isDefaultButton = true
        tooltip("Accept")
        op()
    }

inline fun EventTarget.cancelButton(text: String? = null, crossinline op: JFXButton.() -> Unit = {}) =
    dangerButton(text, Icons.cancel) {
        isCancelButton = true
        tooltip("Cancel")
        op()
    }

inline fun EventTarget.backButton(text: String? = null, crossinline op: JFXButton.() -> Unit = {}) =
    toolbarButton(text, Icons.arrowLeft) {
        isCancelButton = true
        tooltip("Back")
        op()
    }

inline fun EventTarget.resetToDefaultButton(text: String? = "Reset to Default", crossinline op: JFXButton.() -> Unit = {}) =
    warningButton(text, Icons.resetToDefault) {
        tooltip("Reset to Default")
        op()
    }

inline fun EventTarget.addButton(text: String? = null, crossinline op: JFXButton.() -> Unit = {}) =
    confirmButton(text, Icons.add) {
        tooltip("Add")
        op()
    }

inline fun EventTarget.deleteButton(text: String? = null, crossinline op: JFXButton.() -> Unit = {}) =
    dangerButton(text, Icons.delete) {
        tooltip("Delete")
        op()
    }

inline fun EventTarget.plusButton(text: String? = null, crossinline op: JFXButton.() -> Unit = {}) =
    confirmButton(text, Icons.plus.size(22)) {
        removeClass(CommonStyle.toolbarButton)
        op()
    }

inline fun EventTarget.minusButton(text: String? = null, crossinline op: JFXButton.() -> Unit = {}) =
    dangerButton(text, Icons.minus.size(22)) {
        removeClass(CommonStyle.toolbarButton)
        op()
    }

inline fun EventTarget.excludeButton(text: String = "Exclude", crossinline op: JFXButton.() -> Unit = {}) =
    toolbarButton(text, Icons.warning) {
        addClass(CommonStyle.warningButton)
        tooltip(text)
        op()
    }

inline fun EventTarget.editButton(text: String? = null, crossinline op: JFXButton.() -> Unit = {}) =
    toolbarButton(text, Icons.edit, op)

inline fun EventTarget.extraMenu(op: PopOverContent.() -> Unit = {}) = buttonWithPopover(
    graphic = Icons.dots,
    arrowLocation = PopOver.ArrowLocation.TOP_RIGHT,
    op = op
)