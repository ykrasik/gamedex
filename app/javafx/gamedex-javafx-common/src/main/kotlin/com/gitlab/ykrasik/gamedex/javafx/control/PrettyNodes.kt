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

import com.gitlab.ykrasik.gamedex.javafx.containsDelta
import com.gitlab.ykrasik.gamedex.javafx.theme.GameDexStyle
import com.jfoenix.controls.JFXListCell
import javafx.animation.Transition
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.geometry.Orientation
import javafx.scene.Node
import javafx.scene.control.Control
import javafx.scene.control.ListView
import javafx.scene.control.ScrollBar
import javafx.scene.control.ScrollPane
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import org.controlsfx.control.GridView
import tornadofx.*
import kotlin.collections.set

inline fun EventTarget.prettyScrollPane(op: (PrettyScrollPane).() -> Unit) = opcr(this, PrettyScrollPane(), op)

inline fun <T> EventTarget.prettyListView(values: ObservableList<T>? = null, crossinline op: PrettyListView<T>.() -> Unit = {}) =
    opcr(this, PrettyListView<T>()) {
        if (values != null) {
            if (values is SortedFilteredList<T>) {
                values.bindTo(this)
            } else {
                items = values
            }
        }
        op()
    }

fun <T> ListView<T>.prettyListCell(f: JFXListCell<T>.(T) -> Unit) {
    setCellFactory {
        object : JFXListCell<T>() {
            override fun updateItem(item: T?, empty: Boolean) {
                super.updateItem(item, empty)
                if (item == null) return
                f(item)
                graphic?.addClass(GameDexStyle.prettyListCellContent)
            }
        }
    }
}

inline fun <T> EventTarget.prettyGridView(values: ObservableList<T>, crossinline op: PrettyGridView<T>.() -> Unit = {}) =
    opcr(this, PrettyGridView(values)) {
        addClass(GameDexStyle.prettyGridView)
        op()
    }

/**
 * Based on https://github.com/dlemmermann/WorkbenchFX/blob/master/workbenchfx-core/src/main/java/com/dlsc/workbenchfx/view/controls/PrettyScrollPane.java
 *
 * @author Dirk Lemmermann
 * @author François Martin
 * @author Marco Sanfratello
 */
class PrettyScrollPane : ScrollPane() {
    private val vBar: ScrollBar
    private val hBar: ScrollBar

    init {
        addClass(GameDexStyle.prettyScrollPane)
        isFitToWidth = true
        isFitToHeight = true
        vbarPolicy = ScrollBarPolicy.NEVER
        hbarPolicy = ScrollBarPolicy.NEVER

        val (vBar, hBar) = makePrettyScroll("ScrollBar", children)
        this.vBar = vBar
        this.hBar = hBar
    }

    override fun layoutChildren() {
        super.layoutChildren()
        layoutPrettyScrollBars(vBar, hBar)
    }
}

/**
 * Based on https://dlsc.com/2017/09/07/javafx-tip-28-pretty-list-view/
 */
class PrettyListView<T> : ListView<T>() {
    private val vBar: ScrollBar
    private val hBar: ScrollBar

    init {
        addClass(GameDexStyle.prettyList)

        keepSelectionInView()

        val (vBar, hBar) = makePrettyScroll("VirtualScrollBar", children)
        this.vBar = vBar
        this.hBar = hBar
    }

    override fun layoutChildren() {
        super.layoutChildren()
        layoutPrettyScrollBars(vBar, hBar)
    }
}

class PrettyGridView<T>(values: ObservableList<T>) : GridView<T>(values) {
    private val vBar: ScrollBar
    private val hBar: ScrollBar

    init {
        addClass(GameDexStyle.prettyList)

        val (vBar, hBar) = makePrettyScroll("VirtualScrollBar", children)
        this.vBar = vBar
        this.hBar = hBar
    }

    override fun layoutChildren() {
        super.layoutChildren()
        layoutPrettyScrollBars(vBar, hBar)
    }
}

private fun Region.layoutPrettyScrollBars(vBar: ScrollBar, hBar: ScrollBar) {
    val insets = insets
    val w = width
    val h = height
    val prefWidth = vBar.prefWidth(-1.0)
    vBar.resizeRelocate(w - prefWidth - insets.right, insets.top, prefWidth, h - insets.top - insets.bottom)

    val prefHeight = hBar.prefHeight(-1.0)
    hBar.resizeRelocate(insets.left, h - prefHeight - insets.bottom, w - insets.left - insets.right, prefHeight)
}

private var ScrollBar.currentlyBeingShown: Boolean
    get() = properties["gameDex.currentlyBeingShown"] as Boolean
    set(value) {
        properties["gameDex.currentlyBeingShown"] = value
    }

private var ScrollBar.currentlyBeingHidden: Boolean
    get() = properties["gameDex.currentlyBeingHidden"] as Boolean
    set(value) {
        properties["gameDex.currentlyBeingHidden"] = value
    }

private var Node.currentScrollBarTransition: Transition?
    get() = properties["gameDex.currentScrollBarTransition"] as? Transition
    set(value) {
        properties["gameDex.currentScrollBarTransition"] = value
    }

private inline fun createScrollBar(orientation: Orientation, f: ScrollBar.() -> Unit = {}) = ScrollBar().apply {
    this.orientation = orientation
    addClass(GameDexStyle.prettyScrollBar)
    isManaged = false
    opacity = 0.0
    currentlyBeingShown = false
    currentlyBeingHidden = false
    f()
}

private fun Control.bindScrollBars(scrollBarClass: String, vBar: ScrollBar, hBar: ScrollBar) {
    fun bindScrollBars(scrollBarA: ScrollBar, scrollBarB: ScrollBar) {
        scrollBarA.valueProperty().bindBidirectional(scrollBarB.valueProperty())
        scrollBarA.minProperty().bindBidirectional(scrollBarB.minProperty())
        scrollBarA.maxProperty().bindBidirectional(scrollBarB.maxProperty())
        scrollBarA.visibleAmountProperty().bindBidirectional(scrollBarB.visibleAmountProperty())
        scrollBarA.unitIncrementProperty().bindBidirectional(scrollBarB.unitIncrementProperty())
        scrollBarA.blockIncrementProperty().bindBidirectional(scrollBarB.blockIncrementProperty())
    }

    val nodes = lookupAll(scrollBarClass)
    for (node in nodes) {
        if (node is ScrollBar) {
            if (node.orientation == Orientation.VERTICAL) {
                bindScrollBars(vBar, node)
            } else if (node.orientation == Orientation.HORIZONTAL) {
                bindScrollBars(hBar, node)
            }
        }
    }
}

private fun Control.makePrettyScroll(scrollBarClass: String, children: ObservableList<Node>): Pair<ScrollBar, ScrollBar> {
    val parent = this

    val vBar = createScrollBar(Orientation.VERTICAL)
    val hBar = createScrollBar(Orientation.HORIZONTAL)

    skinProperty().onChangeOnce {
        // first bind, then add new scrollbars, otherwise the new bars will be found
        bindScrollBars(scrollBarClass, vBar, hBar)
        children.addAll(vBar, hBar)
    }

    fun ScrollBar.showScrollBar() {
        if (opacity < 1.0 && !currentlyBeingShown) {
            parent.currentScrollBarTransition?.stop()
            currentlyBeingHidden = false
            currentlyBeingShown = true
            parent.currentScrollBarTransition = fade(0.3.seconds, opacity = 1.0, play = true) {
                setOnFinished {
                    currentlyBeingShown = false
                }
            }
        }
    }

    fun ScrollBar.hideScrollBar() {
        if (opacity > 0.0 && !currentlyBeingHidden) {
            parent.currentScrollBarTransition?.stop()
            currentlyBeingShown = false
            currentlyBeingHidden = true
            parent.currentScrollBarTransition = fade(0.5.seconds, opacity = 0.0, play = true) {
                setOnFinished {
                    currentlyBeingHidden = false
                }
            }
        }
    }

    fun ScrollBar.showOnMouseOver() {
        parent.addEventFilter(MouseEvent.MOUSE_MOVED) { e ->
            if (boundsInParent.containsDelta(e.x, e.y, delta = 5.0) && visibleAmount != 1.0) {
                showScrollBar()
            } else {
                hideScrollBar()
            }
        }
    }

    fun ScrollBar.showOnScroll(deltaExtractor: (ScrollEvent) -> Double) {
        parent.addEventFilter(ScrollEvent.SCROLL) { e ->
            if (deltaExtractor(e) != 0.0 && visibleAmount != 1.0) {
                showScrollBar()
            }
        }
    }

    vBar.showOnMouseOver()
    vBar.showOnScroll(ScrollEvent::getDeltaY)

    hBar.showOnMouseOver()
    hBar.showOnScroll(ScrollEvent::getDeltaX)

    var dragging = false
    var draggingExited = false
    addEventFilter(MouseEvent.MOUSE_PRESSED) {
        dragging = true
        draggingExited = false
    }
    addEventFilter(MouseEvent.MOUSE_ENTERED) {
        draggingExited = false
    }
    addEventFilter(MouseEvent.MOUSE_RELEASED) {
        dragging = false
        if (draggingExited) {
            vBar.hideScrollBar()
            hBar.hideScrollBar()
        }
    }
    addEventFilter(MouseEvent.MOUSE_EXITED) {
        if (dragging) {
            draggingExited = true
        } else {
            vBar.hideScrollBar()
            hBar.hideScrollBar()
        }
    }

    return vBar to hBar
}

inline fun EventTarget.prettyToolbar(spacing: Number = 10, crossinline op: HBox.() -> Unit) = defaultHbox(spacing) {
    addClass(GameDexStyle.prettyToolbar)
    useMaxWidth = true
    hgrow = Priority.ALWAYS
    op()
}