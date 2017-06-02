package com.gitlab.ykrasik.gamedex.ui

import javafx.beans.binding.Bindings
import javafx.beans.binding.ListExpression
import javafx.beans.binding.NumberBinding
import javafx.beans.property.*
import javafx.beans.value.ObservableNumberValue
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import tornadofx.cleanBind
import tornadofx.observable
import tornadofx.onChange
import java.util.function.Predicate

/**
 * User: ykrasik
 * Date: 16/05/2017
 * Time: 10:24
 */

fun <T> ObservableValue<T>.printChanges(id: String) = addListener { _, o, v -> println("$id changed: $o -> $v") }

fun ObservableNumberValue.min(other: ObservableNumberValue): NumberBinding = Bindings.min(this, other)

fun <T, R> ObservableValue<T>.map(f: (T?) -> R): Property<R> {
    fun doMap() = f(this.value)

    val property = SimpleObjectProperty(doMap())
    this.onChange { property.set(doMap()) }
    return property
}

fun <T, R> ObservableValue<T>.flatMap(f: (T?) -> ObservableValue<R>): Property<R> {
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

fun <T> ObservableValue<T>.nonNull(): Property<T> {
    val property = SimpleObjectProperty(this.value)
    this.onChange { newValue ->
        if (newValue != null) {
            property.value = newValue
        }
    }
    return property
}

fun <T> ObservableValue<List<T>>.notEmpty(): BooleanProperty {
    fun isNotEmpty() = this.value.isNotEmpty()

    val property = SimpleBooleanProperty(isNotEmpty())
    this.onChange {
        property.value = isNotEmpty()
    }
    return property
}

fun <K, V> ObservableValue<Map<K, V>>.getting(keyProperty: Property<K>): Property<V?> {
    fun doGet() = this.value[keyProperty.value]

    val property = SimpleObjectProperty(doGet())
    this.onChange {
        property.value = doGet()
    }
    keyProperty.onChange {
        property.value = doGet()
    }
    return property
}

fun <K, V> ObservableValue<Map<K, V>>.gettingOrElse(keyProperty: Property<K>, defaultValue: V): Property<V> {
    fun doGetOrElse() = this.value.getOrElse(keyProperty.value) { defaultValue }

    val property = SimpleObjectProperty(doGetOrElse())
    this.onChange {
        property.value = doGetOrElse()
    }
    keyProperty.onChange {
        property.value = doGetOrElse()
    }
    return property
}

fun <T, R> ListExpression<T>.mapping(f: (T) -> R): ListProperty<R> = SimpleListProperty(this.value.mapping(f))
fun <T, R> ObservableValue<List<T>>.mapping(f: (T) -> R): Property<List<R>> {
    fun doMap() = this.value.map(f)

    val property = SimpleObjectProperty(doMap())
    this.onChange { property.set(doMap()) }
    return property
}

fun <T, R> ObservableValue<T>.mapToList(f: (T) -> List<R>): ObservableList<R> {
    fun doMap() = f(this.value)

    val list = doMap().observable()
    this.onChange { list.setAll(doMap()) }
    return list
}