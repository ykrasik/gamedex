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

package com.gitlab.ykrasik.gamedex.javafx

import com.gitlab.ykrasik.gamedex.app.api.util.SettableList
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import javafx.scene.control.ListView
import javafx.scene.control.TableView
import tornadofx.SortedFilteredList
import tornadofx.getValue
import tornadofx.observable
import tornadofx.setValue

/**
 * User: ykrasik
 * Date: 17/06/2019
 * Time: 08:16
 */
class JavaFxSettableList<E>(private val list: ObservableList<E>) : SettableList<E>, ObservableList<E> by list {
    override fun equals(other: Any?) = list == other
    override fun hashCode() = list.hashCode()
    override fun toString() = list.toString()
}

class JavaFxSettableSortedFilteredList<E>(private val list: SortedFilteredList<E>) : SettableList<E>, ObservableList<E> by list {
    init {
        list.setAllPassThrough = true
    }

    val filteredItems get() = list.filteredItems
    val sortedItems get() = list.sortedItems

    fun refilter() = list.refilter()

    val predicateProperty get() = list.predicateProperty
    var predicate by predicateProperty

    fun bindTo(tableView: TableView<E>): SortedFilteredList<E> = list.bindTo(tableView)
    fun bindTo(listView: ListView<E>): SortedFilteredList<E> = list.bindTo(listView)

    fun <Q> filterWhen(observable: ObservableValue<Q>, filterExpr: (Q, E) -> Boolean) = list.filterWhen(observable, filterExpr)

    override fun equals(other: Any?) = list == other
    override fun hashCode() = list.hashCode()
    override fun toString() = list.toString()
}

fun <E> settableList(list: ObservableList<E> = mutableListOf<E>().observable()) = JavaFxSettableList(list)
fun <E> settableSortedFilteredList(
    list: SortedFilteredList<E> = SortedFilteredList(),
    comparator: Comparator<E>? = null
) = JavaFxSettableSortedFilteredList(list).apply {
    if (comparator != null) {
        sortedItems.comparator = comparator
    }
}

fun <E> ObservableList<E>.sortedFiltered(comparator: Comparator<E>? = null): SortedFilteredList<E> =
    SortedFilteredList(this).apply {
        if (comparator != null) {
            sortedItems.comparator = comparator
        }
    }