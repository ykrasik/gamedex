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

package com.gitlab.ykrasik.gamedex.test

import com.gitlab.ykrasik.gamedex.util.now
import com.gitlab.ykrasik.gamedex.util.toFile
import org.joda.time.DateTime
import org.joda.time.LocalDate
import java.util.*

/**
 * User: ykrasik
 * Date: 19/03/2017
 * Time: 13:45
 */
val rnd = Random()

val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
fun randomString(length: Int = 8, variance: Int = 2): String {
    val str = StringBuilder()
    repeat(length.withVariance(variance)) {
        str.append(chars[rnd.nextInt(chars.length)])
    }
    return str.toString()
}

fun randomSentence(maxWords: Int = 5, avgWordLength: Int = 8, variance: Int = 2, delimiter: String = " "): String {
    val str = StringBuilder()
    repeat(maxOf(rnd.nextInt(maxWords), 3)) {
        str.append(randomString(avgWordLength, variance))
        str.append(delimiter)
    }
    str.deleteCharAt(str.length - 1)
    return str.toString()
}

fun Int.withVariance(variance: Int): Int =
    if (variance == 0) {
        this
    } else {
        val multiplier: Int = rnd.nextInt(variance) + 1
        if (rnd.nextBoolean()) (this * multiplier) else (this / multiplier)
    }

fun randomPath(): String = randomSentence(maxWords = 7, delimiter = "/")
fun randomFile() = randomPath().toFile()
fun randomUrl() = "http://${randomSentence(maxWords = 3, delimiter = ".")}/${randomPath()}}"
fun randomName() = randomSentence(maxWords = 4, avgWordLength = 6, variance = 2)

fun randomDateTime(): DateTime = now.plusSeconds((if (rnd.nextBoolean()) 1 else -1) * rnd.nextInt(999999999))
fun randomLocalDate(): LocalDate = randomDateTime().toLocalDate()
fun randomLocalDateString(): String = randomLocalDate().toString()

inline fun <reified E : Enum<E>> randomEnum(): E = E::class.java.enumConstants.randomElement()

fun <T> List<T>.randomElement(): T = this[rnd.nextInt(size)]
fun <T> List<T>.randomElementExcluding(excluded: T): T {
    require(this.any { it != excluded })
    while(true) {
        val element = this.randomElement()
        if (element != excluded) return element
    }
}
fun <T> Array<T>.randomElement(): T = this[rnd.nextInt(size)]