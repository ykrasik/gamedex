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

import com.gitlab.ykrasik.gamedex.app.api.util.*
import io.reactivex.Observable
import javafx.beans.property.BooleanProperty
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.*
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import kotlinx.coroutines.experimental.javafx.JavaFx
import tornadofx.SortedFilteredList
import tornadofx.observable
import tornadofx.onChange
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * User: ykrasik
 * Date: 20/05/2017
 * Time: 10:58
 */
private val observableListSubscriptions = IdentityHashMap<ObservableList<*>, SubscriptionReceiveChannel<ListChangeEvent<*>>>()

fun <T> Observable<out Collection<T>>.toObservableList(): ObservableList<T> {
    val list = mutableListOf<T>().observable()
    this.subscribe {
        list.setAll(it)
    }
    return list
}

// TODO: Cache and re-use the result?
fun <T> ListObservable<T>.toObservableList(list: ObservableList<T> = FXCollections.observableArrayList()): ObservableList<T> {
    list.setAll(this)
    val subscription = subscribe(list)
    require(observableListSubscriptions.put(list, subscription) == null) { "ObservableList already subscribed: $list" }
    return list
}

fun ObservableList<*>.dispose() {
    val subscription = requireNotNull(observableListSubscriptions.remove(this)) { "ObservableList is missing subscription: $this" }
    subscription.close()
}

private fun <T> ListObservable<T>.subscribe(list: ObservableList<T>): SubscriptionReceiveChannel<ListChangeEvent<T>> =
    changesChannel.subscribe(JavaFx) { event ->
        when (event) {
            is ListItemAddedEvent -> list += event.item
            is ListItemsAddedEvent -> list += event.items
            is ListItemRemovedEvent -> list.removeAt(event.index)
            is ListItemsRemovedEvent -> list.removeAll(event.items)
            is ListItemSetEvent -> list[event.index] = event.item
            is ListItemsSetEvent -> list.setAll(event.items)
        }
    }

class InitOnceListObservable<T>(private val observableList: ObservableList<T>) : ReadWriteProperty<Any, ListObservable<T>> {
    private var value: ListObservable<T>? = null

    override fun getValue(thisRef: Any, property: KProperty<*>): ListObservable<T> {
        check(value != null) { "Value wasn't initialized yet!" }
        return value as ListObservable<T>
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: ListObservable<T>) {
        check(this.value == null) { "Value was already initialized: ${this.value}" }
        this.value = value
        value.toObservableList(observableList)
    }
}

inline fun <T> ObservableSet<T>.onChange(crossinline f: (SetChangeListener.Change<out T>) -> Unit): SetChangeListener<T> =
    SetChangeListener<T> { f(it) }.apply { addListener(this) }

inline fun <K, V> ObservableMap<K, V>.onChange(crossinline f: (MapChangeListener.Change<out K, out V>) -> Unit): MapChangeListener<K, V> =
    MapChangeListener<K, V> { f(it) }.apply { addListener(this) }

// TODO: I think this can all be done using objectBinding.
/*******************************************************************************************************
 *                                            List                                                     *
 *******************************************************************************************************/
fun <T> ObservableList<T>.changeListener(op: (ListChangeListener.Change<out T>) -> Unit): ListChangeListener<T> =
    ListChangeListener<T> { c -> op(c) }.apply { addListener(this) }

fun <T, R> ObservableList<T>.mapping(f: (T) -> R): ObservableList<R> {
    fun doMap() = this.map(f)

    val list = FXCollections.observableArrayList(doMap())
    this.onChange { list.setAll(doMap()) }
    return list
}

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