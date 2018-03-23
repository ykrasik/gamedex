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

import com.github.thomasnield.rxkotlinfx.toBinding
import com.gitlab.ykrasik.gamdex.core.api.util.changes
import com.gitlab.ykrasik.gamdex.core.api.util.value_
import com.gitlab.ykrasik.gamedex.util.Extractor
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import javafx.beans.binding.Binding
import javafx.beans.property.*
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import tornadofx.cleanBind
import tornadofx.onChange
import java.util.function.Predicate

/**
 * User: ykrasik
 * Date: 16/05/2017
 * Time: 10:24
 */
fun <T> ObservableValue<T>.subscribeFx(f: suspend (T) -> Unit) {
    onChange {
        launch(JavaFx) {
            f(it!!)
        }
    }
}

// FIXME: Looks like these caches don't solve listener leaks. Instead of leaking on the source observable,
// FIXME: they leak from the transformed property / binding
private val propertyCache = mutableMapOf<BehaviorSubject<*>, ObjectProperty<*>>()
private val bindingCache = mutableMapOf<Observable<*>, Binding<*>>()

@Suppress("UNCHECKED_CAST")
fun <T> BehaviorSubject<T>.toPropertyCached(): ObjectProperty<T> = propertyCache.getOrPut(this) {
    val p = SimpleObjectProperty(value_)

    var shouldCall = true
    this.changes().subscribe {
        shouldCall = false
        p.value = it
        shouldCall = true
    }
    p.onChange {
        if (shouldCall) {
            this.value_ = it!!
        }
    }
    p
} as ObjectProperty<T>

@Suppress("UNCHECKED_CAST")
fun <T> Observable<T>.toBindingCached(): Binding<T> = bindingCache.getOrPut(this) { this.toBinding() } as Binding<T>

fun <T> ObservableValue<T>.changeListener(op: (T?) -> Unit): ChangeListener<T> =
    ChangeListener<T> { _, _, newValue -> op(newValue) }.apply { addListener(this) }

fun <T> ObservableValue<T>.printChanges(id: String) {
    println("$id: $value")
    addListener { _, o, v -> println("$id changed: $o -> $v") }
}

fun Property<Boolean>.toBoolean() = BooleanProperty.booleanProperty(this)

fun <T, R> ObservableValue<T>.map(f: (T?) -> R): ObjectProperty<R> = map(f) { SimpleObjectProperty(it) }
fun <T> ObservableValue<T>.mapString(f: (T?) -> String): StringProperty = map(f) { SimpleStringProperty(it) }
fun <T> ObservableValue<T>.mapBoolean(f: (T?) -> Boolean): BooleanProperty = map(f) { SimpleBooleanProperty(it) }
fun <T> ObservableValue<T>.mapInt(f: (T?) -> Int): IntegerProperty = map(f) { SimpleIntegerProperty(it) }
fun <T> ObservableValue<T>.mapDouble(f: (T?) -> Double): DoubleProperty = map(f) { SimpleDoubleProperty(it) }

private fun <T, R, P : Property<in R>> ObservableValue<T>.map(f: (T?) -> R, factory: (R) -> P): P {
    fun doMap() = f(this.value)

    val property = factory(doMap())
    this.onChange { property.value = doMap() }
    return property
}

fun <T, R> Property<T>.mapBidirectional(extractor: Extractor<T, R>, reverseExtractor: Extractor<R, T>): ObjectProperty<R> =
    mapBidirectional(extractor, reverseExtractor) { SimpleObjectProperty(it) }

fun <T> Property<T>.mapBidirectionalBoolean(extractor: Extractor<T, Boolean>, reverseExtractor: Extractor<Boolean, T>): BooleanProperty =
    mapBidirectional(extractor, reverseExtractor) { SimpleBooleanProperty(it) }

fun <T> Property<T>.mapBidirectionalString(extractor: Extractor<T, String>, reverseExtractor: Extractor<String, T>): StringProperty =
    mapBidirectional(extractor, reverseExtractor) { SimpleStringProperty(it) }

fun <T> Property<T>.mapBidirectionalInt(extractor: Extractor<T, Int>, reverseExtractor: Extractor<Int, T>): IntegerProperty =
    mapBidirectional(extractor, reverseExtractor) { SimpleIntegerProperty(it) }

fun <T> Property<T>.mapBidirectionalDouble(extractor: Extractor<T, Double>, reverseExtractor: Extractor<Double, T>): DoubleProperty =
    mapBidirectional(extractor, reverseExtractor) { SimpleDoubleProperty(it) }

private fun <T, R, P : Property<in R>> Property<T>.mapBidirectional(extractor: Extractor<T, R>,
                                                                    reverseExtractor: Extractor<R, T>,
                                                                    factory: (R) -> P): P {
    val origin = this
    val mapped = factory(extractor(this.value))

    var shouldCall = true
    origin.onChange {
        shouldCall = false
        mapped.value = extractor(it!!)
        shouldCall = true
    }
    mapped.onChange {
        if (shouldCall) {
            @Suppress("UNCHECKED_CAST")
            origin.value = reverseExtractor(it!! as R)
        }
    }

    return mapped
}

fun <T, R> ObservableValue<T>.flatMap(f: (T?) -> ObservableValue<R>): ObjectProperty<R> {
    fun doFlatMap() = f(this.value)
    val property = SimpleObjectProperty<R>()
    property.bind(doFlatMap())

    this.onChange { property.cleanBind(doFlatMap()) }
    return property
}

// Perform the action on the initial value of the observable and on each change.
fun <T> ObservableValue<T>.perform(f: (T) -> Unit) {
    fun doPerform() = f(value)
    doPerform()
    this.onChange { doPerform() }
}

fun <T, R> ObservableValue<T>.toPredicate(f: (T?, R) -> Boolean): Property<Predicate<R>> =
    map { t -> Predicate { r: R -> f(t, r) } }

fun <T, R> ObservableValue<T>.toPredicateF(f: (T?, R) -> Boolean): Property<(R) -> Boolean> =
    map { t -> { r: R -> f(t, r) } }

infix fun <T> ObservableValue<Predicate<T>>.and(other: ObservableValue<Predicate<T>>): Property<Predicate<T>> {
    fun compose() = this.value.and(other.value)
    val property = SimpleObjectProperty(compose())
    this.onChange { property.set(compose()) }
    other.onChange { property.set(compose()) }
    return property
}

fun <T, R> ObservableValue<T>.mapToList(f: (T) -> List<R>): ObservableList<R> {
    fun doMap() = f(this.value)

    val list = FXCollections.observableArrayList(doMap())
    this.onChange { list.setAll(doMap()) }
    return list
}