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

import java.math.BigInteger
import java.security.MessageDigest
import java.util.function.Predicate

/**
 * User: ykrasik
 * Date: 10/02/2017
 * Time: 09:02
 */
typealias Modifier<T> = T.() -> T
typealias Extractor<T, R> = T.() -> R

fun Any.getResourceAsByteArray(path: String): ByteArray = this::class.java.getResource(path).readBytes()

val Int.kb: Int get() = this * 1024
val Int.mb: Int get() = kb * 1024
val Int.gb: Long get() = mb * 1024L

inline fun <T> millisTaken(block: () -> T): Pair<T, Long> {
    val start = System.currentTimeMillis()
    val result = block()
    val taken = System.currentTimeMillis() - start
    return result to taken
}

inline fun <T> nanosTaken(block: () -> T): Pair<T, Long> {
    val start = System.nanoTime()
    val result = block()
    val taken = System.nanoTime() - start
    return result to taken
}

fun <T> ((T) -> Boolean).toPredicate() = Predicate<T> { this(it) }

fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
}

fun Double.toString(decimalDigits: Int) = String.format("%.${decimalDigits}f", this)
fun Double.asPercent() = "${Math.max(Math.min((this * 100).toInt(), 100), 0)}%"
fun Double.roundBy(step: Double) = Math.round(this / step) * step