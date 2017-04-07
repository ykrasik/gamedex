package com.gitlab.ykrasik.gamedex.ui

import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.Separator
import javafx.scene.control.SplitPane
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import org.controlsfx.control.GridView
import org.controlsfx.control.StatusBar
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 11:40
 */
fun EventTarget.statusBar(op: (StatusBar.() -> Unit)? = null) = opcr(this, StatusBar(), op)
fun StatusBar.left(op: (Node.() -> Unit)) = statusBarItems(op, leftItems)
fun StatusBar.right(op: (Node.() -> Unit)) = statusBarItems(op, rightItems)
private fun statusBarItems(op: (Node.() -> Unit), items: ObservableList<Node>) {
    val target = object : Group() {
        override fun getChildren() = items
    }
    op(target)
}

fun Region.padding(op: (InsetBuilder.() -> Unit)) {
    val builder = InsetBuilder(this)
    op(builder)
    padding = Insets(builder.top.toDouble(), builder.right.toDouble(), builder.bottom.toDouble(), builder.left.toDouble())
}
class InsetBuilder(region: Region) {
    var top: Number = region.padding.top
    var bottom: Number = region.padding.bottom
    var right: Number = region.padding.right
    var left: Number = region.padding.left
}

fun EventTarget.verticalSeparator(padding: Double? = null, op: (Separator.() -> Unit)? = null) = separator(Orientation.VERTICAL, op).apply {
    padding?.let {
        padding { right = it; left = it }
    }
}

fun <T> EventTarget.gridView(op: (GridView<T>.() -> Unit)? = null) = opcr(this, GridView(), op)

var SplitPane.dividerPosition: Double
    get() = dividerPositions.first()
    set(value) = setDividerPositions(value)

fun EventTarget.horizontalExpander(): Region = region {
    hgrow = Priority.ALWAYS
    gridpaneConstraints { hGrow = Priority.ALWAYS }
}