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

package com.gitlab.ykrasik.gamedex.javafx.theme

import com.gitlab.ykrasik.gamedex.javafx.control.PopOverContent
import com.gitlab.ykrasik.gamedex.javafx.control.buttonWithPopover
import com.gitlab.ykrasik.gamedex.javafx.control.jfxButton
import com.jfoenix.controls.JFXButton
import javafx.event.EventTarget
import javafx.scene.Node
import tornadofx.addClass
import tornadofx.tooltip

/**
 * User: ykrasik
 * Date: 05/01/2019
 * Time: 08:50
 */

inline fun EventTarget.toolbarButton(
    text: String? = null,
    graphic: Node? = null,
    crossinline op: JFXButton.() -> Unit = {}
) = jfxButton(text, graphic) {
    addClass(GameDexStyle.toolbarButton)
    op()
}

inline fun EventTarget.confirmButton(
    text: String? = null,
    graphic: Node? = null,
    isToolbarButton: Boolean = true,
    crossinline op: JFXButton.() -> Unit = {}
) = jfxButton(text, graphic) {
    addClass(GameDexStyle.confirmButton)
    if (isToolbarButton) addClass(GameDexStyle.toolbarButton)
    op()
}

inline fun EventTarget.warningButton(
    text: String? = null,
    graphic: Node? = null,
    isToolbarButton: Boolean = true,
    crossinline op: JFXButton.() -> Unit = {}
) = jfxButton(text, graphic) {
    addClass(GameDexStyle.warningButton)
    if (isToolbarButton) addClass(GameDexStyle.toolbarButton)
    op()
}

inline fun EventTarget.dangerButton(
    text: String? = null,
    graphic: Node? = null,
    isToolbarButton: Boolean = true,
    crossinline op: JFXButton.() -> Unit = {}
) = jfxButton(text, graphic) {
    addClass(GameDexStyle.dangerButton)
    if (isToolbarButton) addClass(GameDexStyle.toolbarButton)
    op()
}

inline fun EventTarget.infoButton(
    text: String? = null,
    graphic: Node? = null,
    isToolbarButton: Boolean = true,
    crossinline op: JFXButton.() -> Unit = {}
) = jfxButton(text, graphic) {
    addClass(GameDexStyle.infoButton)
    if (isToolbarButton) addClass(GameDexStyle.toolbarButton)
    op()
}

inline fun EventTarget.acceptButton(
    text: String? = null,
    isToolbarButton: Boolean = true,
    crossinline op: JFXButton.() -> Unit = {}
) = confirmButton(text, Icons.accept, isToolbarButton) {
    isDefaultButton = true
    tooltip("Accept")
    op()
}

inline fun EventTarget.cancelButton(
    text: String? = null,
    isToolbarButton: Boolean = true,
    crossinline op: JFXButton.() -> Unit = {}
) = dangerButton(text, Icons.cancel, isToolbarButton) {
    isCancelButton = true
    tooltip("Cancel")
    op()
}

inline fun EventTarget.backButton(
    text: String? = null,
    isToolbarButton: Boolean = true,
    crossinline op: JFXButton.() -> Unit = {}
) = jfxButton(text, Icons.arrowLeft) {
    isCancelButton = true
    if (isToolbarButton) addClass(GameDexStyle.toolbarButton)
    tooltip("Back")
    op()
}

inline fun EventTarget.resetToDefaultButton(
    text: String? = "Reset to Default",
    isToolbarButton: Boolean = true,
    crossinline op: JFXButton.() -> Unit = {}
) = warningButton(text, Icons.resetToDefault, isToolbarButton) {
    tooltip("Reset to Default")
    op()
}

inline fun EventTarget.addButton(
    text: String? = null,
    isToolbarButton: Boolean = true,
    crossinline op: JFXButton.() -> Unit = {}
) = confirmButton(text, Icons.add, isToolbarButton) {
    tooltip("Add")
    op()
}

inline fun EventTarget.deleteButton(
    text: String? = null,
    isToolbarButton: Boolean = true,
    crossinline op: JFXButton.() -> Unit = {}
) = dangerButton(text, Icons.delete, isToolbarButton) {
    tooltip("Delete")
    op()
}

inline fun EventTarget.plusButton(
    text: String? = null,
    isToolbarButton: Boolean = false,
    crossinline op: JFXButton.() -> Unit = {}
) = confirmButton(text, Icons.plus.size(22), isToolbarButton, op)

inline fun EventTarget.minusButton(
    text: String? = null,
    isToolbarButton: Boolean = false,
    crossinline op: JFXButton.() -> Unit = {}
) = dangerButton(text, Icons.minus.size(22), isToolbarButton, op)

inline fun EventTarget.excludeButton(
    text: String = "Exclude",
    isToolbarButton: Boolean = true,
    crossinline op: JFXButton.() -> Unit = {}
) = warningButton(text, Icons.warning, isToolbarButton) {
    tooltip(text)
    op()
}

inline fun EventTarget.executeButton(
    text: String? = "Execute",
    isToolbarButton: Boolean = true,
    crossinline op: JFXButton.() -> Unit = {}
) = jfxButton(text, Icons.play) {
    if (isToolbarButton) addClass(GameDexStyle.toolbarButton)
    op()
}

inline fun EventTarget.editButton(
    text: String? = null,
    isToolbarButton: Boolean = true,
    crossinline op: JFXButton.() -> Unit = {}
) = jfxButton(text, Icons.edit) {
    if (isToolbarButton) addClass(GameDexStyle.toolbarButton)
    op()
}

inline fun EventTarget.browseButton(
    text: String? = "Browse",
    isToolbarButton: Boolean = false,
    crossinline op: JFXButton.() -> Unit = {}
) = jfxButton(text, Icons.folderOpen.size(24)) {
    if (isToolbarButton) addClass(GameDexStyle.toolbarButton)
    op()
}

inline fun EventTarget.extraMenu(op: PopOverContent.() -> Unit = {}) =
    buttonWithPopover(graphic = Icons.dots) {
//        popOver.isAutoFix = false
        op()
    }