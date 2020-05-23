/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.core.util

import com.gitlab.ykrasik.gamedex.app.api.util.broadcastFlow
import com.gitlab.ykrasik.gamedex.core.CoreEvent
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.util.Extractor
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * User: ykrasik
 * Date: 31/03/2018
 * Time: 10:37
 */
interface ListObservable<T> : List<T> {
    val items: StateFlow<List<T>>
    val changes: Flow<ListEvent<T>>

    // Record all changes that happened while executing f() and execute them as a single 'set' operation.
    suspend fun <R> conflate(f: suspend () -> R): R
}

class ListObservableImpl<T>(initial: List<T> = emptyList()) : ListObservable<T> {
    private var _list: List<T> = initial
    override val items = MutableStateFlow(initial)
    override val changes = broadcastFlow<ListEvent<T>>()

    private var conflate = false

    operator fun plusAssign(t: T) = add(t)
    fun add(t: T) {
        _list = _list + t
        notifyChange(ListEvent.ItemAdded(t))
    }

    operator fun plusAssign(items: List<T>) = addAll(items)
    fun addAll(items: List<T>) {
        _list = _list + items
        notifyChange(ListEvent.ItemsAdded(items))
    }

    operator fun minusAssign(t: T) = remove(t)
    fun remove(t: T) {
        val index = _list.indexOf(t)
        require(index != -1) { "Item to be deleted is missing from the list: $t" }
        removeAt(index)
    }

    fun removeAt(index: Int) {
        var removed: T? = null
        _list = _list.filterIndexed { i, item ->
            val keep = i != index
            if (!keep) {
                removed = item
            }
            keep
        }
        notifyChange(ListEvent.ItemRemoved(index, removed!!))
    }

    operator fun minusAssign(items: List<T>) = removeAll(items)
    fun removeAll(items: List<T>) {
        val indices = items.map { item ->
            val index = _list.indexOf(item)
            require(index != -1) { "Item to be deleted is missing from the list: $item" }
            index
        }
        removeAllAt(indices)
    }

    fun removeAllAt(indices: List<Int>) {
        val indicesSet = indices.toSet()
        var i = 0
        val (toKeep, toRemove) = _list.partition {
            val toKeep = i !in indicesSet
            i++
            toKeep
        }
        _list = toKeep
        notifyChange(ListEvent.ItemsRemoved(indices, toRemove))
    }

    fun replace(source: T, target: T) {
        val index = _list.indexOf(source)
        require(index != -1) { "Item to be replaced is missing from the list: $source" }
        set(index, target)
    }

    operator fun set(index: Int, t: T) {
        val prev = _list[index]
        _list = _list.mapIndexed { i, item -> if (i == index) t else item }
        notifyChange(ListEvent.ItemSet(item = t, prevItem = prev, index = index))
    }

    fun setAll(items: List<T>) {
        val prevItems = _list
        _list = items
        notifyChange(ListEvent.ItemsSet(items = items, prevItems = prevItems))
    }

    fun clear() = removeAll(_list)

    fun touch() = setAll(_list)

    private fun notifyChange(event: ListEvent<T>) {
        if (!conflate) {
            items.value = _list
            GlobalScope.launch(start = CoroutineStart.UNDISPATCHED) {
                changes.send(event)
            }
        }
    }

    // Conflate all changes that may occur while executing f() and report a single 'SetList' event.
    override suspend fun <R> conflate(f: suspend () -> R): R {
        conflate = true
        return try {
            f()
        } finally {
            conflate = false
            setAll(_list)
        }
    }

    override val size get() = _list.size
    override fun contains(element: T) = _list.contains(element)
    override fun containsAll(elements: Collection<T>) = _list.containsAll(elements)
    override operator fun get(index: Int) = _list[index]
    override fun indexOf(element: T) = _list.indexOf(element)
    override fun isEmpty() = _list.isEmpty()
    override fun iterator() = _list.iterator()
    override fun lastIndexOf(element: T) = _list.lastIndexOf(element)
    override fun listIterator() = _list.listIterator()
    override fun listIterator(index: Int) = _list.listIterator(index)
    override fun subList(fromIndex: Int, toIndex: Int) = _list.subList(fromIndex, toIndex)

    override fun toString() = _list.toString()
}

@Suppress("unused")
sealed class ListEvent<out T> {
    abstract class AddEvent<out T> : ListEvent<T>()
    data class ItemAdded<out T>(val item: T) : AddEvent<T>()
    data class ItemsAdded<out T>(val items: List<T>) : AddEvent<T>()

    abstract class RemoveEvent<out T> : ListEvent<T>()
    data class ItemRemoved<out T>(val index: Int, val item: T) : RemoveEvent<T>()
    data class ItemsRemoved<out T>(val indices: List<Int>, val items: List<T>) : RemoveEvent<T>()

    abstract class SetEvent<out T> : ListEvent<T>()
    data class ItemSet<out T>(val item: T, val prevItem: T, val index: Int) : SetEvent<T>()
    data class ItemsSet<out T>(val items: List<T>, val prevItems: List<T>) : SetEvent<T>()
}

// TODO: Make this inline when the compiler stops throwing errors
fun <T, R> ListObservable<T>.mapObservable(f: (T) -> R): ListObservable<R> {
    val list = ListObservableImpl(this.map(f))
    flowScope(Dispatchers.Default) {
        changes.forEach(debugName = "mapObservable") { event ->
            when (event) {
                is ListEvent.ItemAdded -> list += f(event.item)
                is ListEvent.ItemsAdded -> list += event.items.map(f)
                is ListEvent.ItemRemoved -> list.removeAt(event.index)
                is ListEvent.ItemsRemoved -> list.removeAllAt(event.indices)
                is ListEvent.ItemSet -> list[event.index] = f(event.item)
                is ListEvent.ItemsSet -> list.setAll(event.items.map(f))
                else -> Unit
            }
        }
    }
    return list
}

// TODO: Make this inline when the compiler stops throwing errors
fun <T> ListObservable<T>.filterObservable(f: (T) -> Boolean): ListObservable<T> =
    transform { it.filter(f) }

fun <T> ListObservable<T>.filterObservable(flow: Flow<(T) -> Boolean>): ListObservable<T> =
    transform(flow.map { f -> { list: List<T> -> list.filter(f) } })

// TODO: Make this inline when the compiler stops throwing errors
fun <T, R> ListObservable<T>.transform(f: (List<T>) -> List<R>): ListObservable<R> {
    val list = ListObservableImpl(f(this))
    flowScope(Dispatchers.Default) {
        items.forEach(debugName = "transform") {
            list.setAll(f(it))
        }
    }
    return list
}

fun <T, R> ListObservable<T>.transform(flow: Flow<(List<T>) -> List<R>>): ListObservable<R> {
    val list = ListObservableImpl<R>()
    flowScope(Dispatchers.Default) {
        items.combine(flow) { items, transform -> transform(items) }.forEach(debugName = "transformFlow") {
            list.setAll(it)
        }
    }
    return list
}

// TODO: Make this inline when the compiler stops throwing errors
fun <T, K> ListObservable<T>.broadcastTo(
    eventBus: EventBus,
    idExtractor: Extractor<T, K>,
    itemsAddedEvent: (List<T>) -> CoreEvent,
    itemsDeletedEvent: (List<T>) -> CoreEvent,
    itemsUpdatedEvent: (List<Pair<T, T>>) -> CoreEvent
) = flowScope(Dispatchers.Default) {
    changes.forEach(debugName = "broadcastTo") { e ->
        val broadcastEvents = when (e) {
            is ListEvent.ItemAdded -> listOf(itemsAddedEvent(listOf(e.item)))
            is ListEvent.ItemsAdded -> listOf(itemsAddedEvent(e.items))
            is ListEvent.ItemRemoved -> listOf(itemsDeletedEvent(listOf(e.item)))
            is ListEvent.ItemsRemoved -> listOf(itemsDeletedEvent(e.items))
            is ListEvent.ItemSet -> listOf(itemsUpdatedEvent(listOf(e.prevItem to e.item)))
            is ListEvent.ItemsSet -> when {
                e.prevItems.isEmpty() -> listOf(itemsAddedEvent(e.items))
                e.items.isEmpty() -> listOf(itemsDeletedEvent(e.prevItems))
                else -> {
                    val prevItemsById = e.prevItems.associateBy(idExtractor)
                    val updatedItemsById = e.items.associateBy(idExtractor)
                    val updatedIds = prevItemsById.keys.intersect(updatedItemsById.keys)

                    val events = mutableListOf<CoreEvent>()

                    val addedItems = e.items.filter { idExtractor(it) !in updatedIds }
                    if (addedItems.isNotEmpty()) {
                        events += itemsAddedEvent(addedItems)
                    }

                    val deletedItems = e.prevItems.filter { idExtractor(it) !in updatedIds }
                    if (deletedItems.isNotEmpty()) {
                        events += itemsDeletedEvent(deletedItems)
                    }

                    val updatedItems = updatedIds.map { prevItemsById.getValue(it) to updatedItemsById.getValue(it) }
                    if (updatedItems.isNotEmpty()) {
                        events += itemsUpdatedEvent(updatedItems)
                    }
                    events
                }
            }
            else -> emptyList()
        }
        broadcastEvents.forEach { eventBus.send(it) }
    }
}

// TODO: Make this inline when the compiler stops throwing errors
fun <K, V> ListObservable<V>.toMap(keyExtractor: Extractor<V, K>): MutableMap<K, V> {
    val map = associateByTo(LinkedHashMap(), keyExtractor)
    flowScope(Dispatchers.Default) {
        changes.forEach(debugName = "toMap") { e ->
            when (e) {
                is ListEvent.ItemAdded -> map += keyExtractor(e.item) to e.item
                is ListEvent.ItemsAdded -> map += e.items.map { keyExtractor(it) to it }
                is ListEvent.ItemRemoved -> map -= keyExtractor(e.item)
                is ListEvent.ItemsRemoved -> map -= e.items.map(keyExtractor)
                is ListEvent.ItemSet -> map += keyExtractor(e.item) to e.item
                is ListEvent.ItemsSet -> map += e.items.map { keyExtractor(it) to it }
                else -> error("Unexpected event: $e")
            }
        }
    }
    return map
}

//// TODO: Make this inline when the compiler stops throwing errors
//// This assumes that related values are always associated to the same key.
//// If a value is replaced by another value, there is an implicit assumption that both values map to the same key.
//fun <K, V> ListObservable<V>.toMultiMap(keyExtractor: Extractor<V, K>): MutableMap<K, MutableList<V>> {
//    val map = groupByTo(LinkedHashMap(), keyExtractor)
//    val listFor = { item: V -> map.getOrPut(keyExtractor(item)) { mutableListOf() } }
//
//    flowScope(Dispatchers.Default) {
//        changes.forEach(debugName = "toMultiMap.onChange") { e ->
//            when (e) {
//                is ListEvent.ItemAdded -> listFor(e.item) += e.item
//                is ListEvent.ItemsAdded -> e.items.forEach { listFor(it) += it }
//                is ListEvent.ItemRemoved -> listFor(e.item) -= e.item
//                is ListEvent.ItemsRemoved -> e.items.forEach { listFor(it) -= it }
//                is ListEvent.ItemSet -> listFor(e.item).replace(e.prevItem, e.item)
//                is ListEvent.ItemsSet -> {
//                    // Screw it. This event is only called as a replace-all-content event.
//                    map.clear()
//                    e.items.groupByTo(map, keyExtractor)
//                }
//                else -> error("Unexpected event: $e")
//            }
//        }
//    }
//    return map
//}