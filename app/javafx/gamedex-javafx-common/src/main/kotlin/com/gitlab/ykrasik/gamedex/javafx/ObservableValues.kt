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

import com.gitlab.ykrasik.gamedex.util.Extractor
import com.gitlab.ykrasik.gamedex.util.Try
import com.gitlab.ykrasik.gamedex.util.and
import javafx.beans.binding.Bindings
import javafx.beans.binding.ObjectBinding
import javafx.beans.property.ObjectProperty
import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.util.StringConverter
import tornadofx.cleanBind
import java.util.concurrent.Callable

/**
 * User: ykrasik
 * Date: 16/05/2017
 * Time: 10:24
 */
fun <T> ObservableValue<T>.printChanges(id: String) {
    println("$id: $value")
    addListener { _, o, v -> println("$id changed: $o -> $v") }
}

fun <T, R> ObservableValue<T>.map(f: (T) -> R): ObjectProperty<R> {
    fun doMap() = f(this.value)

    val property = SimpleObjectProperty(doMap())
    this.typeSafeOnChange { property.value = doMap() }
    return property
}

inline fun <T> ObservableValue<T>.typeSafeOnChange(crossinline op: (T) -> Unit) = apply {
    addListener { _, _, newValue -> op(newValue) }
}

inline fun <T, R> ObservableValue<T>.binding(crossinline op: (T) -> R): ObjectBinding<R> =
    Bindings.createObjectBinding(Callable { op(value) }, this)

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

fun <T, R> ObservableValue<T>.flatMap(f: (T) -> ObservableValue<R>): ObjectProperty<R> {
    fun doFlatMap() = f(this.value)
    val property = SimpleObjectProperty<R>()
    property.bind(doFlatMap())

    this.typeSafeOnChange { property.cleanBind(doFlatMap()) }
    return property
}

// Perform the action on the initial value of the observable and on each change.
fun <T> ObservableValue<T>.perform(f: (T) -> Unit) {
    fun doPerform() = f(value)
    doPerform()
    this.typeSafeOnChange { doPerform() }
}

fun <T, R> ObservableValue<T>.mapToList(f: (T) -> List<R>): ObservableList<R> {
    fun doMap() = f(this.value)

    val list = FXCollections.observableArrayList(doMap())
    this.typeSafeOnChange { list.setAll(doMap()) }
    return list
}

inline fun <T, R, U> ObservableValue<T>.forEachWith(other: ObservableValue<R>, crossinline f: (T, R) -> U) {
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

fun ObservableValue<out Try<Any>>.and(other: ObservableValue<out Try<Any>>): ObjectProperty<Try<Any>> =
    mapWith(other) { first, second -> first.and(second) }