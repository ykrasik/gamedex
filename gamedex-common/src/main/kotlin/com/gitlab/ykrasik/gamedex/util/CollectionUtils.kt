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