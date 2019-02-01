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

import com.gitlab.ykrasik.gamedex.javafx.screenBounds
import com.gitlab.ykrasik.gamedex.javafx.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.size
import javafx.beans.property.Property
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
class PopOverContent(val popOver: PopOver) : VBox() {
    fun close() = popOver.hide()
}

inline fun popOver(
    arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
    onClickBehavior: PopOverOnClickBehavior = PopOverOnClickBehavior.Close(),
    op: PopOverContent.() -> Unit = {}
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
        if (onClickBehavior is PopOverOnClickBehavior.Close) {
            addEventFilter(MouseEvent.MOUSE_CLICKED) {
                onClickBehavior.listener?.invoke()
                popover.hide()
            }
        }
        addEventHandler(KeyEvent.KEY_PRESSED) { if (it.code === KeyCode.ESCAPE) popover.hide() }

        content = PopOverContent(popover).apply {
            addClass(CommonStyle.popoverMenu)
            op()

//            if (closeOnClick) {
//                val consumedProperty = SimpleBooleanProperty(false).apply {
//                    onChange { popover.hide() }
//                }
//
//                addEventFilter(MouseEvent.MOUSE_CLICKED) { e ->
//                    if (e is ObservableMouseEvent) return@addEventFilter
//
//                    val observableMouseEvent = ObservableMouseEvent(e, consumedProperty)
//                    e.consume()
//                    fireEvent(observableMouseEvent)
//                }
//
//                addEventHandler(MouseEvent.MOUSE_CLICKED) { popover.hide() }
//            }
        }
    }
}

val PopOver.content: PopOverContent get() = (contentNode as ScrollPane).content as PopOverContent

class ObservableMouseEvent(e: MouseEvent, private val consumedProperty: Property<Boolean>) : MouseEvent(
    e.source, e.target, e.eventType, e.x, e.y, e.screenX, e.screenY, e.button, e.clickCount, e.isShiftDown, e.isControlDown, e.isAltDown, e.isMetaDown, e.isPrimaryButtonDown,
    e.isMiddleButtonDown, e.isSecondaryButtonDown, e.isSynthesized, e.isPopupTrigger, e.isStillSincePress, e.pickResult
) {
    override fun consume() {
        super.consume()
        consumedProperty.value = true
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
    onClickBehavior: PopOverOnClickBehavior = PopOverOnClickBehavior.Close(),
    op: PopOverContent.() -> Unit = {}
): PopOver {
    val popover = popOver(arrowLocation, onClickBehavior) {
        popOver.isAutoFix = false
        popOver.isAutoHide = true
        op()
    }
    setOnContextMenuRequested { e -> popover.show(this@popoverContextMenu, e.screenX, e.screenY) }
    return popover
}

inline fun Node.dropDownMenu(
    arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
    onClickBehavior: PopOverOnClickBehavior = PopOverOnClickBehavior.Close(),
    op: PopOverContent.() -> Unit = {}
): PopOver {
    val popover = popOver(arrowLocation, onClickBehavior) {
        setOnMouseExited { close() }
        op(this)
    }

    setOnMouseEntered {
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

inline fun PopOverContent.subMenu(
    text: String? = null,
    graphic: Node? = null,
    arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.LEFT_TOP,
    contentDisplay: ContentDisplay = ContentDisplay.LEFT,
    onClickBehavior: PopOverOnClickBehavior = PopOverOnClickBehavior.Close(),
    crossinline op: PopOverContent.() -> Unit = {}
) = hbox(alignment = Pos.CENTER_LEFT) {
    addClass(CommonStyle.subMenu, CommonStyle.jfxHoverable)
    label(text ?: "", graphic) {
        useMaxWidth = true
        hgrow = Priority.ALWAYS
        this.contentDisplay = contentDisplay
    }
    add(Icons.chevronRight.size(14))

    val onClick = if (onClickBehavior is PopOverOnClickBehavior.Close) {
        onClickBehavior.also { this@subMenu.close() }
    } else {
        onClickBehavior
    }
    dropDownMenu(arrowLocation, onClick, op)
}

sealed class PopOverOnClickBehavior {
    class Close(val listener: (() -> Unit)? = null) : PopOverOnClickBehavior() {
        fun also(listener: () -> Unit): Close = Close {
            this.listener?.invoke()
            listener()
        }
    }

    object Ignore : PopOverOnClickBehavior()
}