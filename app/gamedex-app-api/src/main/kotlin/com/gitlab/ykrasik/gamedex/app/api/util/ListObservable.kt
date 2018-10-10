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

package com.gitlab.ykrasik.gamedex.app.api.util

import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.map
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.CoroutineContext

/**
 * User: ykrasik
 * Date: 31/03/2018
 * Time: 10:37
 */
interface ListObservable<out T> : List<T> {
    val itemsChannel: BroadcastReceiveChannel<List<T>>
    val changesChannel: BroadcastReceiveChannel<ListChangeEvent<T>>

    // Record all changes that happened while executing f() and execute them as a single 'set' operation.
    suspend fun <R> buffered(f: suspend () -> R): R

    // TODO: Add a 'bind'
}

class ListObservableImpl<T>(initial: List<T> = emptyList()) : ListObservable<T> {
    private var _list: List<T> = initial
    override val itemsChannel = BroadcastEventChannel.conflated(initial)
    override val changesChannel = BroadcastEventChannel<ListChangeEvent<T>>()

    private var buffer = false

    operator fun plusAssign(t: T) = add(t)
    fun add(t: T) {
        _list += t
        notifyChange(ListItemAddedEvent(t))
    }

    operator fun plusAssign(items: List<T>) = addAll(items)
    fun addAll(items: List<T>) {
        _list += items
        notifyChange(ListItemsAddedEvent(items))
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
        notifyChange(ListItemRemovedEvent(index, removed!!))
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
        notifyChange(ListItemsRemovedEvent(indices, toRemove))
    }

    fun replace(source: T, target: T) {
        val index = _list.indexOf(source)
        require(index != -1) { "Item to be replaced is missing from the list: $source" }
        set(index, target)
    }

    operator fun set(index: Int, t: T) {
        val prev = _list[index]
        _list = _list.mapIndexed { i, item -> if (i == index) t else item }
        notifyChange(ListItemSetEvent(item = t, prevItem = prev, index = index))
    }

    fun setAll(items: List<T>) {
        val prevItems = _list
        _list = items
        notifyChange(ListItemsSetEvent(items = items, prevItems = prevItems))
    }

    private fun notifyChange(event: ListChangeEvent<T>) {
        if (!buffer) {
            itemsChannel.offer(_list)
            changesChannel.offer(event) // TODO: Under some circumstances, this could lose events. However, there were internal errors from the compiler :(
        }
    }

    override suspend fun <R> buffered(f: suspend () -> R): R {
        buffer = true
        return try {
            f()
        } finally {
            buffer = false
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

enum class ListChangeType { Add, Remove, Set }
sealed class ListChangeEvent<out T>(val type: ListChangeType)
data class ListItemAddedEvent<out T>(val item: T) : ListChangeEvent<T>(ListChangeType.Add)
data class ListItemsAddedEvent<out T>(val items: List<T>) : ListChangeEvent<T>(ListChangeType.Add)
data class ListItemRemovedEvent<out T>(val index: Int, val item: T) : ListChangeEvent<T>(ListChangeType.Remove)
data class ListItemsRemovedEvent<out T>(val indices: List<Int>, val items: List<T>) : ListChangeEvent<T>(ListChangeType.Remove)
data class ListItemSetEvent<out T>(val item: T, val prevItem: T, val index: Int) : ListChangeEvent<T>(ListChangeType.Set)
data class ListItemsSetEvent<out T>(val items: List<T>, val prevItems: List<T>) : ListChangeEvent<T>(ListChangeType.Set)

inline fun <T, R> ListObservable<T>.mapping(context: CoroutineContext = Dispatchers.Default, crossinline f: (T) -> R): ListObservable<R> {
    val list = ListObservableImpl(this.map(f))
    changesChannel.subscribe(context) { event ->
        when (event) {
            is ListItemAddedEvent -> list += f(event.item)
            is ListItemsAddedEvent -> list += event.items.map(f)
            is ListItemRemovedEvent -> list.removeAt(event.index)
            is ListItemsRemovedEvent -> list.removeAllAt(event.indices)
            is ListItemSetEvent -> list[event.index] = f(event.item)
            is ListItemsSetEvent -> list.setAll(event.items.map(f))
        }
    }
    return list
}

inline fun <T, R> ListObservable<T>.flatMapping(context: CoroutineContext = Dispatchers.Default, crossinline f: (T) -> List<R>): ListObservable<R> =
    subscribeTransform(context) { it.flatMap(f) }

inline fun <T> ListObservable<T>.filtering(context: CoroutineContext = Dispatchers.Default, crossinline f: (T) -> Boolean): ListObservable<T> =
    subscribeTransform(context) { it.filter(f) }

fun <T> ListObservable<T>.filtering(channel: ReceiveChannel<(T) -> Boolean>, context: CoroutineContext = Dispatchers.Default): ListObservable<T> =
    subscribeTransformChannel(context, channel.map { f -> { list: List<T> -> list.filter(f) } })

fun <T> ListObservable<T>.distincting(context: CoroutineContext = Dispatchers.Default): ListObservable<T> =
    subscribeTransform(context) { it.distinct() }

inline fun <T, R : Comparable<R>> ListObservable<T>.sortingBy(context: CoroutineContext = Dispatchers.Default, crossinline selector: (T) -> R?): ListObservable<T> =
    subscribeTransform(context) { it.sortedBy(selector) }

fun <T> ListObservable<T>.sortingWith(channel: ReceiveChannel<Comparator<T>>, context: CoroutineContext = Dispatchers.Default): ListObservable<T> =
    subscribeTransformChannel(context, channel.map { c -> { list: List<T> -> list.sortedWith(c) } })

inline fun <T, R> ListObservable<T>.subscribeTransform(context: CoroutineContext, crossinline f: (List<T>) -> List<R>): ListObservable<R> {
    val list = ListObservableImpl<R>()
    itemsChannel.subscribe(context) {
        list.setAll(f(it))
    }
    return list
}

fun <T, R> ListObservable<T>.subscribeTransformChannel(context: CoroutineContext, channel: ReceiveChannel<(List<T>) -> List<R>>): ListObservable<R> {
    val list = ListObservableImpl<R>()
    GlobalScope.launch(context) {
        itemsChannel.subscribe().combineLatest(channel).consumeEach { (items, f) ->
            list.setAll(f(items))
        }
    }
    return list
}