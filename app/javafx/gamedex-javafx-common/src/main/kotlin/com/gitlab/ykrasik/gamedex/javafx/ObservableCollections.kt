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

package com.gitlab.ykrasik.gamedex.javafx

import io.reactivex.Observable
import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.*
import tornadofx.SortedFilteredList
import tornadofx.observable
import tornadofx.onChange

/**
 * User: ykrasik
 * Date: 20/05/2017
 * Time: 10:58
 */
fun <T> Observable<out Collection<T>>.toObservableList(): ObservableList<T> {
    val list = mutableListOf<T>().observable()
    this.subscribe {
        list.setAll(it)
    }
    return list
}

inline fun <T> ObservableSet<T>.onChange(crossinline f: (SetChangeListener.Change<out T>) -> Unit): SetChangeListener<T> =
    SetChangeListener<T> { f(it) }.apply { addListener(this) }

inline fun <K, V> ObservableMap<K, V>.onChange(crossinline f: (MapChangeListener.Change<out K, out V>) -> Unit): MapChangeListener<K, V> =
    MapChangeListener<K, V> { f(it) }.apply { addListener(this) }

fun <T> ObservableList<T>.changeListener(op: (ListChangeListener.Change<out T>) -> Unit): ListChangeListener<T> =
    ListChangeListener<T> { c -> op(c) }.apply { addListener(this) }

// Perform the action on the initial value of the observable and on each change.
fun <T> ObservableList<T>.perform(f: (ObservableList<T>) -> Unit): ListChangeListener<T> {
    fun doPerform() = f(this)
    doPerform()
    return this.changeListener { doPerform() }
}

// TODO: Look at using objectBinding()
fun <T, R> ObservableList<T>.mapProperty(f: (ObservableList<T>) -> R): Property<R> {
    val property = SimpleObjectProperty(f(this))
    this.onChange { property.set(f(this)) }
    return property
}

fun <T> ObservableList<T>.sortedFiltered() = SortedFilteredList(this)
fun <T> List<T>.sortedFiltered() = this.observable().sortedFiltered()