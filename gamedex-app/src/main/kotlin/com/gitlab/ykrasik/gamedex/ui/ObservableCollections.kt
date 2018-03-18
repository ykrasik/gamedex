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

package com.gitlab.ykrasik.gamedex.ui

import javafx.beans.InvalidationListener
import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.ObservableSet
import javafx.collections.transformation.TransformationList
import tornadofx.SortedFilteredList
import tornadofx.observable
import tornadofx.onChange

/**
 * User: ykrasik
 * Date: 20/05/2017
 * Time: 10:58
 */

// TODO: I think this can all be done using objectBinding.
/*******************************************************************************************************
 *                                            List                                                     *
 *******************************************************************************************************/
fun <T> emptyObservableList() = FXCollections.emptyObservableList<T>()

fun <T> ObservableList<T>.changeListener(op: (ListChangeListener.Change<out T>) -> Unit): ListChangeListener<T> =
    ListChangeListener<T> { c -> op(c) }.apply { addListener(this) }

fun <T> ObservableList<T>.unmodifiable(): ObservableList<T> = FXCollections.unmodifiableObservableList(this)
fun <T> ObservableList<T>.sizeProperty(): ReadOnlyIntegerProperty {
    val p = SimpleIntegerProperty(this.size)
    this.onChange { p.set(this.size) }
    return p
}

fun <T, R> ObservableList<T>.mapping(f: (T) -> R): ObservableList<R> = MappedList(this, f)

// TODO: This is the un-optimized version
fun <T, R> ObservableList<T>.flatMapping(f: (T) -> List<R>): ObservableList<R> {
    fun doFlatMap() = this.flatMap(f)

    val list = FXCollections.observableArrayList(doFlatMap())
    this.onChange { list.setAll(doFlatMap()) }
    return list
}

// Perform the action on the initial value of the observable and on each change.
fun <T> ObservableList<T>.performing(f: (ObservableList<T>) -> Unit): ListChangeListener<T> {
    fun doPerform() = f(this)
    doPerform()
    return this.changeListener { doPerform() }
}

// TODO: This is the un-optimized version
fun <T> ObservableList<T>.distincting(): ObservableList<T> {
    fun doDistinct() = this.distinct()

    val list = FXCollections.observableArrayList(doDistinct())
    this.onChange { list.setAll(doDistinct()) }
    return list
}

fun <T> ObservableList<T>.containing(value: Property<T>): BooleanProperty {
    fun doesContain() = this.contains(value.value)

    val property = SimpleBooleanProperty(doesContain())
    this.onChange { property.value = doesContain() }
    value.onChange { property.value = doesContain() }
    return property
}

fun <T> ObservableList<T>.adding(other: ObservableList<T>): ObservableList<T> {
    fun doAdd() = this + other

    val list = doAdd().observable()
    this.onChange { list.setAll(doAdd()) }
    other.onChange { list.setAll(doAdd()) }
    return list
}

fun <T> ObservableList<T>.filtering(property: Property<(T) -> Boolean>): ObservableList<T> {
    fun doFilter() = this.filter(property.value)

    val list = doFilter().observable()
    this.onChange { list.setAll(doFilter()) }
    property.onChange { list.setAll(doFilter()) }
    return list
}

fun <T> ObservableList<T>.existing(f: (T) -> Boolean): BooleanProperty {
    fun doesExist() = this.any(f)

    val property = SimpleBooleanProperty(doesExist())
    this.onChange { property.value = doesExist() }
    return property
}

// TODO: Look at using objectBinding()
fun <T, R> ObservableList<T>.mapProperty(f: (ObservableList<T>) -> R): Property<R> {
    val property = SimpleObjectProperty(f(this))
    this.onChange { property.set(f(this)) }
    return property
}

fun <T> ObservableList<T>.asProperty(): Property<ObservableList<T>> {
    val property = SimpleObjectProperty(this)

    // Very unfortunate, but otherwise a change event isn't fired.
    var invalidationList: ObservableList<T> =
        if (this.isEmpty()) FXCollections.emptyObservableList() else FXCollections.observableArrayList(this.first())

    this.onChange {
        property.set(FXCollections.emptyObservableList())
        if (this.isNotEmpty() && invalidationList.isEmpty()) {
            invalidationList = FXCollections.observableArrayList(this.first())
        } else {
            property.set(invalidationList)
        }
        property.set(this)
    }
    return property
}

fun <T> ObservableList<T>.sortedFiltered() = SortedFilteredList(this)
fun <T> List<T>.sortedFiltered() = this.observable().sortedFiltered()

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

/*******************************************************************************************************
 *                                             Set                                                     *
 *******************************************************************************************************/

// Perform the action on the initial value of the observable and on each change.
fun <T> ObservableSet<T>.performing(f: (ObservableSet<T>) -> Unit) {
    fun doPerform() = f(this)
    doPerform()
    this.addListener(InvalidationListener {
        doPerform()
    })
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

fun <T> ObservableSet<T>.invalidate() {
    if (isNotEmpty()) {
        this += this.first()
    }
}