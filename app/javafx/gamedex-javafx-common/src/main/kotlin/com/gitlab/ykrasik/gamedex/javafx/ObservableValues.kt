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

import com.gitlab.ykrasik.gamedex.util.Extractor
import javafx.beans.InvalidationListener
import javafx.beans.binding.Bindings
import javafx.beans.binding.BooleanBinding
import javafx.beans.binding.ObjectBinding
import javafx.beans.binding.StringBinding
import javafx.beans.property.ObjectProperty
import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.util.StringConverter
import tornadofx.cleanBind
import tornadofx.observable
import tornadofx.onChange

/**
 * User: ykrasik
 * Date: 16/05/2017
 * Time: 10:24
 */
fun <T> ObservableValue<T>.printChanges(id: String) {
    println("$id: $value")
    addListener { _, o, v -> println("$id changed: $o -> $v") }
}

inline fun <T, R> ObservableValue<T>.map(crossinline f: (T) -> R): ObjectProperty<R> {
    val property = SimpleObjectProperty(f(value))
    this.typeSafeOnChange { property.value = f(it) }
    return property
}

inline fun <T> ObservableValue<T>.typeSafeOnChange(crossinline op: (T) -> Unit): ChangeListener<T> {
    val listener = ChangeListener { _, _, newValue -> op(newValue) }
    addListener(listener)
    return listener
}

inline fun <T> ObservableValue<T>.onInvalidated(crossinline op: (T) -> Unit): InvalidationListener {
    val listener = InvalidationListener { op(value) }
    addListener(listener)
    return listener
}

inline fun <T, R> ObservableValue<T>.binding(crossinline op: (T) -> R): ObjectBinding<R> =
    Bindings.createObjectBinding({ op(value) }, this)

inline fun <T> ObservableValue<T>.typesafeBooleanBinding(crossinline op: (T) -> Boolean): BooleanBinding =
    Bindings.createBooleanBinding({ op(value) }, this)

inline fun <T> ObservableValue<T>.typesafeStringBinding(crossinline op: (T) -> String): StringBinding =
    Bindings.createStringBinding({ op(value) }, this)

fun <T> ObservableValue<out Collection<T>>.asObservableList(): ObservableList<T> {
    val list = value.toMutableList().observable()
    onChange {
        list.setAll(it)
    }
    return list
}

fun <T> Property<String>.bindBidirectional(property: Property<T>, toString: (T) -> String, fromString: (String) -> T) {
    property.value = fromString(value)
    Bindings.bindBidirectional(this, property, object : StringConverter<T>() {
        override fun toString(t: T) = toString(t!!)
        override fun fromString(string: String?) = fromString(string!!)
    })
}

fun <T, R> Property<T>.mapBidirectional(extractor: Extractor<T, R>, reverseExtractor: Extractor<R, T>): ObjectProperty<R> {
    val origin = this
    val mapped = SimpleObjectProperty(extractor(this.value))

    var shouldCall = true
    origin.typeSafeOnChange {
        shouldCall = false
        mapped.value = extractor(it)
        shouldCall = true
    }
    mapped.typeSafeOnChange {
        if (shouldCall) {
            origin.value = reverseExtractor(it)
        }
    }

    return mapped
}

inline fun <T, R> ObservableValue<T>.flatMap(crossinline f: (T) -> ObservableValue<R>): ObjectProperty<R> {
    val property = SimpleObjectProperty<R>()
    property.bind(f(value))
    this.typeSafeOnChange { property.cleanBind(f(it)) }
    return property
}

// Perform the action on the initial value of the observable and on each change.
inline fun <T> ObservableValue<T>.perform(crossinline f: (T) -> Unit): ChangeListener<T> {
    f(value)
    return this.typeSafeOnChange { f(it) }
}

inline fun <T, R> ObservableValue<T>.mapToList(crossinline f: (T) -> List<R>): ObservableList<R> {
    val list = FXCollections.observableArrayList(f(value))
    this.typeSafeOnChange { list.setAll(f(it)) }
    return list
}

inline fun <T, R> ObservableValue<T>.forEachWith(other: ObservableValue<R>, crossinline f: (T, R) -> Unit) {
    this.typeSafeOnChange { f(it, other.value) }
    other.typeSafeOnChange { f(this.value, it) }
}

fun <T, R> ObservableValue<T>.combineLatest(other: ObservableValue<R>): ObjectProperty<Pair<T, R>> {
    val property = SimpleObjectProperty(this.value to other.value)
    this.typeSafeOnChange { property.value = it to other.value }
    other.typeSafeOnChange { property.value = this.value to it }
    return property
}

inline fun <T, R, U> ObservableValue<T>.mapWith(other: ObservableValue<R>, crossinline f: (T, R) -> U): ObjectProperty<U> {
    val property = SimpleObjectProperty(f(this.value, other.value))
    this.typeSafeOnChange { property.value = f(it, other.value) }
    other.typeSafeOnChange { property.value = f(this.value, it) }
    return property
}
