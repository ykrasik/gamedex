package com.gitlab.ykrasik.gamedex.ui.util

import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.Separator
import javafx.scene.control.SplitPane
import javafx.scene.layout.Region
import org.controlsfx.control.StatusBar
import tornadofx.opcr
import tornadofx.separator

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
    padding = Insets(builder.top, builder.right, builder.bottom, builder.left)
}
class InsetBuilder(region: Region) {
    var top: Double = region.padding.top
    var bottom: Double = region.padding.bottom
    var right: Double = region.padding.right
    var left: Double = region.padding.left
}

fun EventTarget.verticalSeparator(op: (Separator.() -> Unit)? = null) = separator(Orientation.VERTICAL, op)

var SplitPane.dividerPosition: Double
    get() = dividerPositions.first()
    set(value) = setDividerPositions(value)



//fun GridPane.row(op: (GridPane.() -> Unit)) {
//    properties["gridPane.row"] = properties["gridPane.row"]?.let { it as Int + 1 } ?: 0
//}