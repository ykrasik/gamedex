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

package com.gitlab.ykrasik.gamedex.util

/**
 * User: ykrasik
 * Date: 31/12/2016
 * Time: 19:20
 */

fun <T, R> List<T>.firstNotNull(extractor: (T) -> R?): R? = this.asSequence().map { extractor(it) }.firstNotNull()

fun <T> Sequence<T>.firstNotNull(): T? = this.firstOrNull { it != null }

fun <T> MutableList<T>.replaceFirst(replacement: T, pred: (T) -> Boolean): Boolean {
    val li = listIterator()
    while (li.hasNext()) {
        val element = li.next()
        if (pred(element)) {
            li.set(replacement)
            return true
        }
    }
    return false
}

inline fun <T, R> Iterable<T>.flatMapIndexed(transform: (index: Int, T) -> Iterable<R>): List<R> = flatMapIndexedTo(ArrayList<R>(), transform)
inline fun <T, R, C : MutableCollection<in R>> Iterable<T>.flatMapIndexedTo(destination: C, transform: (index: Int, T) -> Iterable<R>): C {
    var index = 0
    for (item in this) destination.addAll(transform(index++, item))
    return destination
}

typealias MultiMap<K, V> = Map<K, List<V>>

fun <T, R> Iterable<Pair<T, R>>.toMultiMap(): MultiMap<T, R> = groupBy({ it.first }, { it.second })
fun <T, R> Sequence<Pair<T, R>>.toMultiMap(): MultiMap<T, R> = groupBy({ it.first }, { it.second })