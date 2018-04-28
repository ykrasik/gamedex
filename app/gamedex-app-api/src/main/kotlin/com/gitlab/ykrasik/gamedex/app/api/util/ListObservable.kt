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

import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlin.coroutines.experimental.CoroutineContext

/**
 * User: ykrasik
 * Date: 31/03/2018
 * Time: 10:37
 */
interface ListObservable<out T> : List<T> {
    val itemsChannel: BroadcastReceiveChannel<List<T>>
    val changesChannel: BroadcastReceiveChannel<ListChangeEvent<T>>
}

class ListObservableImpl<T>(initial: List<T> = emptyList()) : ListObservable<T> {
    private var _list: List<T> = initial
    override val itemsChannel = BroadcastEventChannel.conflated(initial)
    override val changesChannel = BroadcastEventChannel<ListChangeEvent<T>>()

    operator fun plusAssign(t: T) = add(t)
    fun add(t: T) {
        _list += t
        notifyChange(ListItemAddedEvent(t, index = null))
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
        _list -= t
        notifyChange(ListItemRemovedEvent(t, index))
    }

    operator fun minusAssign(items: List<T>) = removeAll(items)
    fun removeAll(items: List<T>) {
        require(_list.containsAll(items)) { "Items to be deleted are missing from the list: ${items.filterNot(_list::contains)}" }
        _list -= items
        notifyChange(ListItemsRemovedEvent(items))
    }

    fun replace(source: T, target: T) {
        val index = _list.indexOf(source)
        require(index != -1) { "Item to be replaced is missing from the list: $source" }
        _list = _list.mapIndexed { i, item -> if (i == index) target else item }
        notifyChange(ListItemSetEvent(target, index))
    }

    fun set(items: List<T>) {
        _list = items
        notifyChange(ListItemsSetEvent(items))
    }

    private fun notifyChange(event: ListChangeEvent<T>) {
        itemsChannel.offer(_list)
        changesChannel.offer(event) // TODO: Under some circumstances, this could lose events. However, there were internal errors from the compiler :(
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
}

enum class ListChangeType { Add, Remove, Set }
sealed class ListChangeEvent<out T>(val type: ListChangeType)
data class ListItemAddedEvent<out T>(val item: T, val index: Int?) : ListChangeEvent<T>(ListChangeType.Add)
data class ListItemsAddedEvent<out T>(val items: List<T>) : ListChangeEvent<T>(ListChangeType.Add)
data class ListItemRemovedEvent<out T>(val item: T, val index: Int) : ListChangeEvent<T>(ListChangeType.Remove)
data class ListItemsRemovedEvent<out T>(val items: List<T>) : ListChangeEvent<T>(ListChangeType.Remove)
data class ListItemSetEvent<out T>(val item: T, val index: Int) : ListChangeEvent<T>(ListChangeType.Set)
data class ListItemsSetEvent<out T>(val items: List<T>) : ListChangeEvent<T>(ListChangeType.Set)

inline fun <T, R> ListObservable<T>.mapping(context: CoroutineContext = DefaultDispatcher, crossinline f: (T) -> R): ListObservable<R> {
    val list = ListObservableImpl<R>()
    itemsChannel.subscribe(context) {
        list.set(it.map(f))
    }
    return list
}

inline fun <T> ListObservable<T>.filtering(context: CoroutineContext = DefaultDispatcher, crossinline f: (T) -> Boolean): ListObservable<T> {
    val list = ListObservableImpl<T>()
    itemsChannel.subscribe(context) {
        list.set(it.filter(f))
    }
    return list
}