package com.gitlab.ykrasik.gamedex.ui

import javafx.beans.InvalidationListener
import javafx.beans.binding.Bindings
import javafx.beans.binding.ListExpression
import javafx.beans.binding.NumberBinding
import javafx.beans.property.*
import javafx.beans.value.ObservableNumberValue
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.ObservableSet
import javafx.collections.transformation.TransformationList
import tornadofx.cleanBind
import tornadofx.onChange
import java.util.*
import java.util.function.Predicate

/**
 * User: ykrasik
 * Date: 16/05/2017
 * Time: 10:24
 */
fun <T> ObservableList<T>.unmodifiable(): ObservableList<T> = FXCollections.unmodifiableObservableList(this)
fun <T> ObservableList<T>.sizeProperty(): ReadOnlyIntegerProperty {
    val p = SimpleIntegerProperty(this.size)
    this.addListener(ListChangeListener { p.set(this.size) })
    return p
}

fun <T> ObservableValue<T>.printChanges(id: String) = addListener { _, o, v -> println("$id changed: $o -> $v") }

fun ObservableNumberValue.min(other: ObservableNumberValue): NumberBinding = Bindings.min(this, other)

fun <T, R> ListExpression<T>.mapped(f: (T) -> R): ListProperty<R> = SimpleListProperty(this.value.mapped(f))
fun <T, R> ObservableList<T>.mapped(f: (T) -> R): ObservableList<R> = MappedList(this, f)

// TODO: This is the un-optimized version
fun <T, R> ObservableList<T>.flatMapped(f: (T) -> List<R>): ObservableList<R> {
    fun doFlatMap() = this.flatMap(f)

    val list = FXCollections.observableArrayList(doFlatMap())
    this.onChange {
        list.setAll(doFlatMap())
    }
    return list
}

// TODO: This is the un-optimized version
fun <T> ObservableList<T>.distincted(): ObservableList<T> {
    fun doDistinct() = this.distinct()

    val list = FXCollections.observableArrayList(doDistinct())
    this.onChange {
        list.setAll(doDistinct())
    }
    return list
}

fun <T> ObservableList<T>.containing(value: Property<T>): BooleanProperty {
    fun doesContain() = this.contains(value.value)

    val property = SimpleBooleanProperty(doesContain())
    this.onChange {
        property.value = doesContain()
    }
    value.onChange {
        property.value = doesContain()
    }
    return property
}

fun <T> ObservableSet<T>.containing(value: Property<T>): BooleanProperty {
    fun doesContain() = this.contains(value.value)

    val property = SimpleBooleanProperty(doesContain())
    this.addListener(InvalidationListener {
        property.value = doesContain()
    })
    value.onChange {
        property.value = doesContain()
    }
    return property
}

fun <T> ObservableSet<T>.equaling(other: ObservableSet<T>): BooleanProperty {
    fun doesEqual() = this == other

    val property = SimpleBooleanProperty(doesEqual())
    this.addListener(InvalidationListener {
        property.value = doesEqual()
    })
    other.addListener(InvalidationListener {
        property.value = doesEqual()
    })
    return property
}

fun <T> ObservableList<T>.existing(f: (T) -> Boolean): BooleanProperty {
    fun doesExist() = this.any(f)

    val property = SimpleBooleanProperty(doesExist())
    this.onChange {
        property.value = doesExist()
    }
    return property
}

/**
 * Creates a new MappedList list wrapped around the source list.
 * Each element will have the given function applied to it, such that the list is cast through the mapper.
 * Taken from https://gist.github.com/mikehearn/a2e4a048a996fd900656
 */
// TODO: tornadoFx has something similar, called ListConversionListener
class MappedList<E, F>(source: ObservableList<out F>, private val mapper: (F) -> E) : TransformationList<E, F>(source) {
    private var mapped = transform()

    private fun transform(): MutableList<E> = source.map(mapper) as MutableList<E>

    override fun sourceChanged(c: ListChangeListener.Change<out F>) {
        // Is all this stuff right for every case? Probably it doesn't matter for this app.
        beginChange()
        while (c.next()) {
            if (c.wasPermutated()) {
                val perm = IntArray(c.to - c.from)
                for (i in c.from..c.to - 1)
                    perm[i - c.from] = c.getPermutation(i)
                nextPermutation(c.from, c.to, perm)
            } else if (c.wasUpdated()) {
                for (i in c.from..c.to - 1) {
                    remapIndex(i)
                    nextUpdate(i)
                }
            } else {
                if (c.wasRemoved()) {
                    // Removed should come first to properly handle replacements, then add.
                    val removed = mapped.subList(c.from, c.from + c.removedSize)
                    val duped = ArrayList(removed)
                    removed.clear()
                    nextRemove(c.from, duped)
                }
                if (c.wasAdded()) {
                    for (i in c.from..c.to - 1) {
                        mapped.addAll(c.from, c.addedSubList.map(mapper))
                        remapIndex(i)
                    }
                    nextAdd(c.from, c.to)
                }
            }
        }
        endChange()
    }

    private fun remapIndex(i: Int) {
        if (i >= mapped.size) {
            for (j in mapped.size..i) {
                mapped.add(mapper(source[j]))
            }
        }
        mapped[i] = mapper(source[i])
    }

    override fun getSourceIndex(index: Int) = index

    override fun get(index: Int): E = mapped[index]

    override val size: Int get() = mapped.size
}

fun <T, R> ObservableValue<T>.map(f: (T?) -> R): Property<R> {
    val property = SimpleObjectProperty(f(this.value))
    this.onChange { property.set(f(it)) }
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

fun <T, R> ObservableValue<T>.flatMap(f: (T?) -> ObservableValue<R>): Property<R> {
    fun calc() = f(this.value)
    val property = SimpleObjectProperty<R>()
    property.bind(calc())

    this.onChange { property.cleanBind(calc()) }
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

infix fun <T> ObservableValue<Predicate<T>>.and(other: ObservableValue<Predicate<T>>): Property<Predicate<T>> {
    fun compose() = this.value.and(other.value)
    val property = SimpleObjectProperty(compose())
    this.onChange { property.set(compose()) }
    other.onChange { property.set(compose()) }
    return property
}

// TODO: Look at using objectBinding()
fun <T, R> ObservableList<T>.mapProperty(f: (ObservableList<T>) -> R): Property<R> {
    val property = SimpleObjectProperty(f(this))
    this.onChange {
        property.set(f(this))
    }
    return property
}

// Perform the action on the initial value of the observable and on each change.
fun <T> ObservableList<T>.perform(f: (ObservableList<T>) -> Unit) {
    fun doPerform() = f(this)
    doPerform()
    this.onChange {
        doPerform()
    }
}

// Perform the action on the initial value of the observable and on each change.
fun <T> ObservableSet<T>.perform(f: (ObservableSet<T>) -> Unit) {
    fun doPerform() = f(this)
    doPerform()
    this.addListener(InvalidationListener {
        doPerform()
    })
}

fun <T> ObservableValue<List<T>>.notEmpty(): BooleanProperty {
    fun isNotEmpty() = this.value.isNotEmpty()

    val property = SimpleBooleanProperty(isNotEmpty())
    this.onChange {
        property.value = isNotEmpty()
    }
    return property
}

fun <T> ObservableSet<T>.invalidate() {
    if (isNotEmpty()) {
        this += this.first()
    }
}