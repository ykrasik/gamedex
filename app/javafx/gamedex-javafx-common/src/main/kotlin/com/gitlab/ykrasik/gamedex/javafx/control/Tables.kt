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

import com.gitlab.ykrasik.gamedex.javafx.forEachWith
import com.gitlab.ykrasik.gamedex.javafx.theme.GameDexStyle
import com.sun.javafx.scene.control.skin.ListViewSkin
import com.sun.javafx.scene.control.skin.TableViewSkin
import com.sun.javafx.scene.control.skin.TreeViewSkin
import com.sun.javafx.scene.control.skin.VirtualFlow
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.util.Callback
import tornadofx.*
import kotlin.reflect.KProperty1

/**
 * User: ykrasik
 * Date: 09/12/2018
 * Time: 13:30
 */
fun <S, T> TableView<S>.simpleColumn(title: String, valueProvider: (S) -> T): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = Callback { SimpleObjectProperty(valueProvider(it.value)) }
    columns.add(column)
    return column
}

inline fun <reified S> TableView<S>.customColumn(
    title: String,
    crossinline cellFactory: (TableColumn<S, S>) -> TableCell<S, S>,
): TableColumn<S, S> {
    val column = TableColumn<S, S>(title)
    addColumnInternal(column)
    column.cellValueFactory = Callback { SimpleObjectProperty(it.value) }
    column.setCellFactory { cellFactory(it) }
    return column
}

fun <S, T> TableView<S>.customColumn(
    title: String,
    valueProvider: (S) -> ObservableValue<T>,
    cellFactory: (TableColumn<S, T>) -> TableCell<S, T>,
): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    addColumnInternal(column)
    column.cellValueFactory = Callback { valueProvider(it.value) }
    column.setCellFactory { cellFactory(it) }
    return column
}

inline fun <reified S, T> TableView<S>.customColumn(
    title: String,
    prop: KProperty1<S, T>,
    crossinline cellFactory: (TableColumn<S, T>) -> TableCell<S, T>,
): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    addColumnInternal(column)
    column.cellValueFactory = Callback { observable(it.value, prop) }
    column.setCellFactory { cellFactory(it) }
    return column
}

fun <S, T> TableView<S>.customGraphicColumn(
    title: String,
    valueProvider: (S) -> ObservableValue<T>,
    graphicFactory: (T) -> Node,
): TableColumn<S, T> = customColumn(title, valueProvider) {
    object : TableCell<S, T>() {
        override fun updateItem(item: T?, empty: Boolean) {
            graphic = item?.let { graphicFactory(it) }
        }
    }
}

inline fun <reified S> TableView<S>.customGraphicColumn(title: String, crossinline graphicFactory: (S) -> Node): TableColumn<S, S> =
    customColumn(title) {
        object : TableCell<S, S>() {
            init {
                alignment = Pos.CENTER
            }

            override fun updateItem(item: S?, empty: Boolean) {
                graphic = item?.let { graphicFactory(it) }
            }
        }
    }

// TODO: Can probably rewrite this using customGraphicColumn
inline fun <reified S> TableView<S>.imageViewColumn(
    title: String,
    fitWidth: Number,
    fitHeight: Number,
    isPreserveRatio: Boolean = true,
    crossinline imageRetriever: (S) -> ObservableValue<Image>,
): TableColumn<S, S> = customColumn(title) {
    object : TableCell<S, S>() {
        private val imageView = ImageView().apply {
            fadeOnImageChange()
            this@apply.fitHeight = fitHeight.toDouble()
            this@apply.fitWidth = fitWidth.toDouble()
            this@apply.isPreserveRatio = isPreserveRatio
        }

        init {
            addClass(GameDexStyle.centered)
            graphic = imageView
        }

        override fun updateItem(item: S?, empty: Boolean) {
            if (item != null) {
                val image = imageRetriever(item)
                imageView.imageProperty().cleanBind(image)
            } else {
                imageView.imageProperty().unbind()
                imageView.image = null
            }
        }
    }
}

fun <T> TableView<T>.minWidthFitContent(indexColumn: TableColumn<T, Number>? = null) {
    minWidthProperty().bind(contentColumns.fold(indexColumn?.widthProperty()?.subtract(10) ?: 0.0.toProperty()) { binding, column ->
        binding.add(column.widthProperty())
    })
}

fun <S> TableView<S>.allowDeselection(onClickAgain: Boolean) {
    val tableView = this
    var lastSelectedRow: TableRow<S>? = null
    setRowFactory {
        TableRow<S>().apply {
            selectedProperty().onChange {
                if (it) lastSelectedRow = this
            }
            if (onClickAgain) {
                addEventFilter(MouseEvent.MOUSE_PRESSED) { e ->
                    if (index >= 0 && index < tableView.items.size && tableView.selectionModel.isSelected(index)) {
                        tableView.selectionModel.clearSelection()
                        e.consume()
                    }
                }
            }
        }
    }
    addEventFilter(MouseEvent.MOUSE_CLICKED) { e ->
        lastSelectedRow?.let { row ->
            val boundsOfSelectedRow = row.localToScene(row.layoutBounds)
            if (!boundsOfSelectedRow.contains(e.sceneX, e.sceneY)) {
                tableView.selectionModel.clearSelection()
            }
        }
    }
}

fun <S> TableView<S>.clearSelection() = selectionModel.clearSelection()

fun <S> TableView<S>.keepSelectionInView() {
    selectionModel.selectedIndexProperty().forEachWith(skinProperty()) { selectedIndex, skin ->
        val flow = (skin as TableViewSkin<*>).children[1] as VirtualFlow<*>
        if (selectedIndex != -1) {
            flow.show(selectedIndex as Int)
        }
    }
}

fun <S> TreeView<S>.keepSelectionInView() {
    selectionModel.selectedIndexProperty().forEachWith(skinProperty()) { selectedIndex, skin ->
        val flow = (skin as TreeViewSkin<*>).children.first() as VirtualFlow<*>
        if (selectedIndex != -1) {
            flow.show(selectedIndex as Int)
        }
    }
}

fun <S> ListView<S>.keepSelectionInView() {
    selectionModel.selectedIndexProperty().forEachWith(skinProperty()) { selectedIndex, skin ->
        val flow = (skin as ListViewSkin<*>).children.first() as VirtualFlow<*>
        if (selectedIndex != -1) {
            flow.show(selectedIndex as Int)
        }
    }
}