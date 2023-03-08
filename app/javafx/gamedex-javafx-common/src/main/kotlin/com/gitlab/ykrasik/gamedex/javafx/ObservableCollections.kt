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

package com.gitlab.ykrasik.gamedex.javafx

import com.gitlab.ykrasik.gamedex.app.api.util.SettableList
import javafx.beans.value.ObservableValue
import javafx.collections.*
import javafx.scene.control.ListView
import javafx.scene.control.TableView
import tornadofx.SortedFilteredList
import tornadofx.getValue
import tornadofx.observable
import tornadofx.setValue

/**
 * User: ykrasik
 * Date: 20/05/2017
 * Time: 10:58
 */
inline fun <T> ObservableSet<T>.onChange(crossinline f: (SetChangeListener.Change<out T>) -> Unit): SetChangeListener<T> =
    SetChangeListener<T> { f(it) }.apply { addListener(this) }

inline fun <K, V> ObservableMap<K, V>.onChange(crossinline f: (MapChangeListener.Change<out K, out V>) -> Unit): MapChangeListener<K, V> =
    MapChangeListener<K, V> { f(it) }.apply { addListener(this) }

inline fun <T> ObservableList<T>.changeListener(crossinline op: (ListChangeListener.Change<out T>) -> Unit): ListChangeListener<T> =
    ListChangeListener<T> { c -> op(c) }.apply { addListener(this) }

// Perform the action on the initial value of the observable and on each change.
inline fun <T> ObservableList<T>.perform(crossinline f: (ObservableList<T>) -> Unit): ListChangeListener<T> {
    f(this)
    return this.changeListener { f(this) }
}

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

    override fun removeAt(index: Int): E {
        // SortedFilteredList removes from the sortedItems list instead of from the base items list,
        // which in our use cases for this list results in it removing the wrong item.
        return list.items.removeAt(index)
    }

    override fun set(index: Int, element: E): E {
        // SortedFilteredList sets items in the sortedItems list instead of in the base items list,
        // which in our use cases for this list results in it setting the wrong item.
        return list.items.set(index, element)
    }

    override fun equals(other: Any?) = list == other
    override fun hashCode() = list.hashCode()
    override fun toString() = list.toString()
}

fun <E> settableList(list: ObservableList<E> = mutableListOf<E>().observable()) = JavaFxSettableList(list)
fun <E> settableSortedFilteredList(
    list: SortedFilteredList<E> = SortedFilteredList(),
    comparator: Comparator<E>? = null,
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