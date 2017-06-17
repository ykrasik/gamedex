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

fun <T, R> Iterable<Pair<T, R>>.toMultiMap(): Map<T, List<R>> = groupBy({ it.first }, { it.second })
fun <T, R> Sequence<Pair<T, R>>.toMultiMap(): Map<T, List<R>> = groupBy({ it.first }, { it.second })