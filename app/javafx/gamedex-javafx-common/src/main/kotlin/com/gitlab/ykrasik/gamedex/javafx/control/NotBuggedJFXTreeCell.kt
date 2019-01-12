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

import com.jfoenix.controls.JFXRippler
import com.jfoenix.utils.JFXNodeUtils
import javafx.beans.InvalidationListener
import javafx.beans.WeakInvalidationListener
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.TreeCell
import javafx.scene.control.TreeItem
import javafx.scene.layout.*
import javafx.scene.paint.Color
import java.lang.ref.WeakReference

/**
 * User: ykrasik
 * Date: 15/11/2018
 * Time: 08:50
 *
 * A copy of [com.jfoenix.controls.JFXTreeCell] which does not override the text of the node
 * when the treeItem's graphic changes.
 */
open class NotBuggedJFXTreeCell<T> : TreeCell<T>() {
    private var cellRippler = object : JFXRippler(this) {
        override fun getMask(): Node {
            val clip = Region()
            JFXNodeUtils.updateBackground(this@NotBuggedJFXTreeCell.background, clip)
            clip.resize(control.layoutBounds.width, control.layoutBounds.height)
            return clip
        }

        override fun positionControl(control: Node) {
            // do nothing
        }
    }

    private var hbox: HBox? = null
    private val selectedPane = StackPane()

    private val treeItemGraphicInvalidationListener = InvalidationListener { updateDisplay(item, isEmpty) }
    private val weakTreeItemGraphicListener = WeakInvalidationListener(treeItemGraphicInvalidationListener)

    private val treeItemInvalidationListener = InvalidationListener {
        val oldTreeItem = if (treeItemRef == null) null else treeItemRef!!.get()
        oldTreeItem?.graphicProperty()?.removeListener(weakTreeItemGraphicListener)

        val newTreeItem = treeItem
        if (newTreeItem != null) {
            newTreeItem.graphicProperty().addListener(weakTreeItemGraphicListener)
            treeItemRef = WeakReference(newTreeItem)
        }
    }
    private val weakTreeItemListener = WeakInvalidationListener(treeItemInvalidationListener)

    private var treeItemRef: WeakReference<TreeItem<T>>? = null

    init {
        selectedPane.styleClass.add("selection-bar")
        selectedPane.background = Background(BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY))
        selectedPane.prefWidth = 3.0
        selectedPane.isMouseTransparent = true
        selectedProperty().addListener { _, _, newVal -> selectedPane.isVisible = newVal }

        treeItemProperty().addListener(weakTreeItemListener)
        treeItem?.graphicProperty()?.addListener(weakTreeItemGraphicListener)
    }

    override fun layoutChildren() {
        super.layoutChildren()
        if (!children.contains(selectedPane)) {
            children.add(0, cellRippler)
//            cellRippler.rippler.clear()
            children.add(0, selectedPane)
        }
        cellRippler.resizeRelocate(0.0, 0.0, width, height)
        selectedPane.resizeRelocate(0.0, 0.0, selectedPane.prefWidth(-1.0), height)
        selectedPane.isVisible = isSelected
    }

    private fun updateDisplay(item: T?, empty: Boolean) {
        if (item == null || empty) {
            hbox = null
            text = null
            setGraphic(null)
        } else {
            val treeItem = treeItem
            if (treeItem != null && treeItem.graphic != null) {
                if (item is Node) {
//                    text = null
                    if (hbox == null) {
                        hbox = HBox(3.0)
                    }
                    hbox!!.children.setAll(treeItem.graphic, item as Node?)
                    setGraphic(hbox)
                } else {
                    hbox = null
//                    text = item.toString()
                    setGraphic(treeItem.graphic)
                }
            } else {
                hbox = null
                if (item is Node) {
//                    text = null
                    setGraphic(item as Node?)
                } else {
//                    text = item.toString()
                    setGraphic(null)
                }
            }
        }
    }

    override fun updateItem(item: T?, empty: Boolean) {
        super.updateItem(item, empty)
        updateDisplay(item, empty)
        isMouseTransparent = item == null || empty
    }
}