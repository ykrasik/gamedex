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

import com.gitlab.ykrasik.gamedex.javafx.CommonStyle
import com.gitlab.ykrasik.gamedex.javafx.Icons
import com.gitlab.ykrasik.gamedex.javafx.screenBounds
import com.gitlab.ykrasik.gamedex.javafx.size
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.ContentDisplay
import javafx.scene.control.ScrollPane
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import org.controlsfx.control.PopOver
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/12/2018
 * Time: 13:34
 */
inline fun popOver(
    arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
    closeOnClick: Boolean = true,
    op: VBox.(PopOver) -> Unit = {}
): PopOver = PopOver().apply {
    val popover = this
    this.arrowLocation = arrowLocation
    isAnimated = false  // A ton of exceptions start getting thrown if closing a window with an open popover without this.
    isDetachable = false
    isAutoFix = true

    contentNode = ScrollPane()
    with(contentNode as ScrollPane) {
        maxHeight = screenBounds.height * 3 / 4
        isFitToWidth = true
        isFitToHeight = true
        hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        if (closeOnClick) addEventFilter(MouseEvent.MOUSE_CLICKED) { popover.hide() }
        addEventHandler(KeyEvent.KEY_PRESSED) { if (it.code === KeyCode.ESCAPE) popover.hide() }

        content = VBox().apply {
            addClass(CommonStyle.popoverMenu)
            op(popover)
        }
    }
}

fun PopOver.determineArrowLocation(x: Double, y: Double) = apply {
    val leftBound = screenBounds.maxX / 3
    val rightBound = leftBound * 2
    val topBound = screenBounds.maxY / 3
    val bottomBound = topBound * 2

    val isLeft = x < leftBound
    val isRight = x > rightBound
    val isTop = y < topBound
    val isBottom = y > bottomBound

    arrowLocation = when {
        isLeft -> when {
            isTop -> PopOver.ArrowLocation.TOP_LEFT
            isBottom -> PopOver.ArrowLocation.BOTTOM_LEFT
            else -> PopOver.ArrowLocation.LEFT_CENTER
        }
        isRight -> when {
            isTop -> PopOver.ArrowLocation.TOP_RIGHT
            isBottom -> PopOver.ArrowLocation.BOTTOM_RIGHT
            else -> PopOver.ArrowLocation.RIGHT_CENTER
        }
        else -> when {
            isTop -> PopOver.ArrowLocation.TOP_CENTER
            isBottom -> PopOver.ArrowLocation.BOTTOM_CENTER
            else -> PopOver.ArrowLocation.TOP_LEFT
        }
    }
}

fun PopOver.replaceContent(content: Node) {
    (contentNode as ScrollPane).content = content
}

fun PopOver.toggle(parent: Node) = if (isShowing) hide() else show(parent)


inline fun Node.popoverContextMenu(
    arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
    closeOnClick: Boolean = true,
    op: VBox.(PopOver) -> Unit = {}
): PopOver {
    val popover = popOver(arrowLocation, closeOnClick, op).apply { isAutoFix = false; isAutoHide = true }
    addEventHandler(MouseEvent.MOUSE_CLICKED) { popover.hide() }
    setOnContextMenuRequested { e -> popover.show(this@popoverContextMenu, e.screenX, e.screenY) }
    return popover
}

inline fun Node.dropDownMenu(
    arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
    closeOnClick: Boolean = true,
    op: VBox.() -> Unit = {}
): PopOver {
    val popover = popOver(arrowLocation, closeOnClick) { popover ->
        addEventHandler(MouseEvent.MOUSE_EXITED) { popover.hide() }
        op(this)
    }

    addEventHandler(MouseEvent.MOUSE_ENTERED) {
        if (!popover.isShowing) popover.show(this@dropDownMenu)
    }
    setOnMouseExited {
        if (!(it.screenX >= popover.x && it.screenX <= popover.x + popover.width &&
                it.screenY >= popover.y && it.screenY <= popover.y + popover.height)
        ) {
            popover.hide()
        }
    }
    return popover
}

inline fun EventTarget.subMenu(
    text: String? = null,
    graphic: Node? = null,
    arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.LEFT_TOP,
    contentDisplay: ContentDisplay = ContentDisplay.LEFT,
    closeOnClick: Boolean = true,
    crossinline op: VBox.() -> Unit = {}
) = hbox(alignment = Pos.CENTER_LEFT) {
    addClass(CommonStyle.subMenu, CommonStyle.jfxHoverable)
    label(text ?: "") {
        useMaxWidth = true
        hgrow = Priority.ALWAYS
        this.graphic = graphic
        this.contentDisplay = contentDisplay
    }
    add(Icons.chevronRight.size(14))
    dropDownMenu(arrowLocation, closeOnClick, op)
}