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

package com.gitlab.ykrasik.gamedex.util

/**
 * User: ykrasik
 * Date: 31/12/2016
 * Time: 19:20
 */
typealias MultiMap<K, V> = Map<K, List<V>>

fun <T, R> Iterable<Pair<T, R>>.toMultiMap(): MultiMap<T, R> = groupBy({ it.first }, { it.second })
fun <T, R> Sequence<Pair<T, R>>.toMultiMap(): MultiMap<T, R> = groupBy({ it.first }, { it.second })

fun <T> Sequence<T>.firstNotNull(): T? = this.firstOrNull { it != null }

fun <T, C : Collection<T>> C.emptyToNull(): C? = if (isEmpty()) null else this

fun <T> MutableCollection<T>.setAll(iterable: Iterable<T>) {
    this.clear()
    this.addAll(iterable)
}

inline fun <T, R> Iterable<T>.flatMapIndexed(transform: (index: Int, T) -> Iterable<R>): List<R> = flatMapIndexedTo(ArrayList(), transform)
inline fun <T, R, C : MutableCollection<in R>> Iterable<T>.flatMapIndexedTo(destination: C, transform: (index: Int, T) -> Iterable<R>): C {
    var index = 0
    for (item in this) destination.addAll(transform(index++, item))
    return destination
}

inline fun <K, V, K2, V2> Map<K, V>.mapNotNullToMap(crossinline f: (K, V) -> Pair<K2, V2>?): Map<K2, V2> {
    if (this.isEmpty()) return emptyMap()

    val map = mutableMapOf<K2, V2>()
    forEach { k, v ->
        val newKeyValue = f(k, v)
        if (newKeyValue != null) {
            map[newKeyValue.first] = newKeyValue.second
        }
    }
    return map
}

fun <K, V> Map<K, V?>.filterNullValues(): Map<K, V> = mapNotNullToMap { key, value -> value?.let { key to it } }

fun <T> List<T>.replace(source: T, target: T): List<T> {
    var replaced = false
    return map { elem ->
        if (!replaced && elem == source) {
            replaced = true
            target
        } else {
            elem
        }
    }
}

inline fun <T> MutableList<T>.replaceWhere(target: T, predicate: (T) -> Boolean) {
    val index = indexOfFirst(predicate)
    check(index != -1) { "List doesn't contain any elements matching predicate to replace!" }
    this[index] = target
}

inline fun <T> List<T>.replaceWhere(target: T, predicate: (T) -> Boolean): List<T> =
    map { elem -> if (predicate(elem)) target else elem }

fun <T> List<T>.replaceIndex(index: Int, value: T): List<T> =
    mapIndexed { i, elem -> if (index == i) value else elem }

inline fun <T> List<T>.modify(index: Int, modifier: Modifier<T>): List<T> =
    mapIndexed { i, elem -> if (index == i) modifier(elem) else elem }

inline fun <T> List<T>.modifyLast(modifier: Modifier<T>): List<T> = modify(size - 1, modifier)

inline fun <T> List<T>.findCircular(startIndex: Int = 0, predicate: (T) -> Boolean): T? {
    var i = startIndex
    do {
        i = (i + 1) % size
        val element = this[i]
        if (predicate(element)) return element
    } while (i != startIndex)
    return null
}

fun <T> MutableList<T>.push(t: T) = add(t)
fun <T> MutableList<T>.pop() = removeAt(size - 1)