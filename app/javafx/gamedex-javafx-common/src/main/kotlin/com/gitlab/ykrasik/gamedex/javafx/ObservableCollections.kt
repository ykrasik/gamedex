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

import javafx.collections.*

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