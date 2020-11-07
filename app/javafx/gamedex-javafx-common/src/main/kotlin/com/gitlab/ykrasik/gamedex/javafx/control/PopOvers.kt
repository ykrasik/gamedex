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

import com.gitlab.ykrasik.gamedex.javafx.boundsInScreen
import com.gitlab.ykrasik.gamedex.javafx.debounce
import com.gitlab.ykrasik.gamedex.javafx.screenBounds
import com.gitlab.ykrasik.gamedex.javafx.theme.GameDexStyle
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.size
import javafx.beans.property.Property
import javafx.event.EventHandler
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.ButtonBase
import javafx.scene.control.ScrollPane
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import kotlinx.coroutines.Job
import org.controlsfx.control.PopOver
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/12/2018
 * Time: 13:34
 */
open class PopOverContent(val popOver: PopOver) : VBox() {
    fun hide() = popOver.hide()
}

inline fun popOverWith(op: PopOver.() -> Unit = {}): PopOver = PopOver().apply {
    isAnimated = false  // A ton of exceptions start getting thrown if closing a window with an open popover without this.
    isDetachable = false
    isAutoFix = true
    isAutoHide = true
    op()
}

inline fun popOver(
    arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
    closeOnAction: Boolean = true,
    op: PopOverContent.() -> Unit = {},
): PopOver = popOverWith {
    val popover = this
    this.arrowLocation = arrowLocation

    contentNode = PrettyScrollPane()
    with(contentNode as PrettyScrollPane) {
        maxHeight = screenBounds.height / 2
        isFitToWidth = true
        isFitToHeight = true
        hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        if (closeOnAction) {
            addEventFilter(MouseEvent.MOUSE_CLICKED) { e ->
                if (e.target.let { it is ButtonBase && !it.isIgnore }) {
                    popover.hide()
                }
            }
        }
        addEventHandler(KeyEvent.KEY_PRESSED) { if (it.code === KeyCode.ESCAPE) popover.hide() }

        content = PopOverContent(popover).apply {
            addClass(GameDexStyle.popOverMenu)
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

fun PopOver.determineArrowLocation(parent: Node, preferTop: Boolean = true, preferLeft: Boolean = true) = apply {
    val bounds = parent.boundsInScreen
    return determineArrowLocation(
        x = bounds.minX + bounds.width / 2,
        y = bounds.minY + bounds.height / 2,
        preferTop = preferTop,
        preferLeft = preferLeft
    )
}

fun PopOver.determineArrowLocation(x: Double, y: Double, preferTop: Boolean = true, preferLeft: Boolean = true) = apply {
    val leftBound = screenBounds.maxX / 3 * (if (preferLeft) 2 else 1)
    val rightBound = if (preferLeft) leftBound else leftBound * 2
    val topBound = screenBounds.maxY / 3 * (if (preferTop) 2 else 1)
    val bottomBound = if (preferTop) topBound else topBound * 2

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
    closeOnAction: Boolean = true,
    op: PopOverContent.() -> Unit = {},
): PopOver {
    val popover = popOver(arrowLocation, closeOnAction) {
        popOver.isAutoFix = false
        popOver.isAutoHide = true
        op()
    }
    setOnContextMenuRequested { e -> popover.show(this@popoverContextMenu, e.screenX, e.screenY) }
    return popover
}

inline fun Node.dropDownMenu(
    arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
    closeOnAction: Boolean = true,
    op: PopOverContent.() -> Unit = {},
): PopOver {
    val popover = popOver(arrowLocation, closeOnAction) {
        setOnMouseExited { hide() }
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

inline fun EventTarget.popOverMenu(
    text: String? = null,
    graphic: Node? = null,
    closeOnAction: Boolean = true,
    op: PopOverMenu.() -> Unit = {},
) = jfxButton(text = text, graphic = graphic, alignment = Pos.CENTER_LEFT) {
    val popOver = popOverWith {
        contentNode = PrettyScrollPane().apply {
            maxHeight = screenBounds.height * 2
            content = createPopOverMenu(closeOnAction, owner = this@jfxButton, parentMenu = null, op = op)
        }
    }
    action { popOver.determineArrowLocation(this).toggle(this) }
}

inline fun PopOverMenu.popOverSubMenu(
    text: String? = null,
    graphic: Node? = null,
    closeOnAction: Boolean = true,
    crossinline op: PopOverMenu.() -> Unit = {},
) = hbox(alignment = Pos.CENTER_LEFT) {
    addClass(GameDexStyle.popOverSubMenu, GameDexStyle.jfxHoverable)
    label(text ?: "", graphic) {
        useMaxWidth = true
        hgrow = Priority.ALWAYS
    }
    add(Icons.chevronRight.size(14))

    val popOver = popOverWith {
        contentNode = PrettyScrollPane().apply {
            maxHeight = screenBounds.height / 2
            content = createPopOverMenu(closeOnAction, owner = this@hbox, parentMenu = this@popOverSubMenu, op = op)
        }
    }
    properties[PopOverMenu.subMenuMarker] = (popOver.contentNode as ScrollPane).content
}

inline fun PopOver.createPopOverMenu(closeOnAction: Boolean, owner: Node, parentMenu: PopOverMenu?, op: PopOverMenu.() -> Unit = {}): PopOverMenu =
    PopOverMenu(popOver = this, owner = owner, parentMenu = parentMenu, closeOnAction = closeOnAction).apply {
        addClass(GameDexStyle.popOverMenu)
        op()
    }

/**
 * A popOver based menu that may have sub-menus which show on hover and hide automatically when another member of the menu is hovered.
 */
class PopOverMenu(popOver: PopOver, val owner: Node, val parentMenu: PopOverMenu?, closeOnAction: Boolean) : PopOverContent(popOver) {
    private var unregisterEventHandlers = emptyList<() -> Unit>()
    private var debounceJob: Job? = null

    init {
        addClass(GameDexStyle.popOverMenu)
        children.onChange {
            unregisterEventHandlers.forEach { it() }
            unregisterEventHandlers = children.map { node ->
                val mouseEnteredHandler = EventHandler<MouseEvent> {
                    debounceJob?.cancel()
                    val subMenu = node.subMenu
                    if (subMenu != null) {
                        // We're hovering over a subMenu.
                        // If this is the currently showing subMenu, do nothing.
                        // If there is no subMenu currently showing, show this one.
                        // Otherwise, hide the currently showing subMenu and show this one.
                        val currentlyShowingSubMenu = this.currentlyShowingSubMenu
                        if (subMenu != currentlyShowingSubMenu) {
                            debounceJob = debounce(millis = 200) {
                                subMenu.show()
                                currentlyShowingSubMenu?.hide()
                            }
                        }
                    } else {
                        // We're hovering over a regular control.
                        // Hide the currently showing subMenu, if there is one.
                        if (!node.isIgnore) {
                            currentlyShowingSubMenu?.hide()
                        }
                    }
                }
                node.addEventHandler(MouseEvent.MOUSE_ENTERED, mouseEnteredHandler)

                val mouseClickedHandler =
                    if (closeOnAction) EventHandler<MouseEvent> { e ->
                        if (e.target is ButtonBase && !node.isIgnore) {
                            hideHierarchy()
                        }
                    }
                    else null
                if (mouseClickedHandler != null) {
                    node.addEventFilter(MouseEvent.MOUSE_CLICKED, mouseClickedHandler)
                }

                return@map {
                    node.removeEventHandler(MouseEvent.MOUSE_ENTERED, mouseEnteredHandler)
                    if (mouseClickedHandler != null) {
                        node.removeEventFilter(MouseEvent.MOUSE_CLICKED, mouseClickedHandler)
                    }
                }
            }
        }
    }

    private val currentlyShowingSubMenu: PopOverMenu?
        get() = children.asSequence().mapNotNull { it.subMenu }.find { it.popOver.isShowing }

    private fun show() = popOver.show(owner)
    private fun hideHierarchy() {
        hide()
        parentMenu?.hideHierarchy()
    }

    companion object {
        const val popOverIgnore = "popOverIgnore"
        const val subMenuMarker = "popOverSubMenu"
    }
}

private val Node.subMenu: PopOverMenu? get() = properties[PopOverMenu.subMenuMarker] as? PopOverMenu
val Node.isIgnore: Boolean get() = properties[PopOverMenu.popOverIgnore] as? Boolean ?: false