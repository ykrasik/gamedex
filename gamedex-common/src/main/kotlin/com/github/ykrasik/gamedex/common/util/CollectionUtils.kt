package com.github.ykrasik.gamedex.common.util

/**
 * User: ykrasik
 * Date: 31/12/2016
 * Time: 19:20
 */

fun <T, R> List<T>.firstNotNull(extractor: (T) -> R?): R? = this.asSequence().map { extractor(it) }.firstNotNull()

fun <T> Sequence<T>.firstNotNull(): T? = this.firstOrNull { it != null }