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

package com.gitlab.ykrasik.gamedex.test

import com.gitlab.ykrasik.gamedex.Score
import com.gitlab.ykrasik.gamedex.Timestamp
import com.gitlab.ykrasik.gamedex.util.capitalize
import com.gitlab.ykrasik.gamedex.util.file
import com.gitlab.ykrasik.gamedex.util.now
import io.github.classgraph.ClassGraph
import kotlinx.coroutines.repackaged.net.bytebuddy.utility.RandomString
import org.joda.time.DateTime
import org.joda.time.LocalDate
import java.text.DecimalFormat
import java.util.*

/**
 * User: ykrasik
 * Date: 19/03/2017
 * Time: 13:45
 */
private object TestResources {
    val words = javaClass.getResource("words.txt")!!.readText().lines()
    val genres = javaClass.getResource("genres.txt")!!.readText().lines()

    /* These images were all taken from [[igdb.com]] */
    val images = ClassGraph()
        .acceptPackages("com.gitlab.ykrasik.gamedex.test")
        .scan()
        .getResourcesWithExtension(".jpg")
        .urLs
}

val rnd = Random()

fun randomString(length: Int = 8): String = RandomString.make(length)

/**
 * [max] is inclusive.
 */
fun randomInt(max: Int = Int.MAX_VALUE - 1, min: Int = 0): Int = if (min != max) rnd.nextInt(max + 1 - min) + min else min

inline fun <T> randomList(max: Int = Int.MAX_VALUE - 1, min: Int = 1, f: (Int) -> T): List<T> =
    List(randomInt(min = min, max = max), f)

fun randomWord() = TestResources.words.randomElement()
fun randomWords(maxWords: Int = Int.MAX_VALUE - 1, minWords: Int = 1): List<String> =
    randomList(min = minWords, max = maxWords) { randomWord() }

fun randomName(maxWords: Int = 6): String = randomWords(maxWords).joinToString(" ") { it.capitalize() }

fun randomParagraph(minWords: Int = 20, maxWords: Int = 100): String =
    randomWords(minWords = minWords, maxWords = maxWords).joinToString(" ").capitalize()

fun randomPath(maxElements: Int = 4, minElements: Int = 1): String =
    randomWords(minWords = minElements, maxWords = maxElements).joinToString("/")

fun randomFile() = randomPath().file
fun randomUrl() = "http://${randomWords(minWords = 3, maxWords = 3).joinToString(".")}/${randomPath()}".lowercase()

fun randomTimestamp(): Timestamp = Timestamp(createDate = randomDateTime(), updateDate = randomDateTime())
fun randomDateTime(): DateTime = now.minusSeconds(randomInt(999999999))
fun randomLocalDate(): LocalDate = randomDateTime().toLocalDate()
fun randomLocalDateString(): String = randomLocalDate().toString()

fun randomScore() = Score(
    score = DecimalFormat("###.##").format(rnd.nextDouble() * 100).toDouble(),
    numReviews = randomInt(max = 3000, min = 1)
)

fun randomGenre() = TestResources.genres.randomElement()

fun randomImage(): ByteArray = TestResources.images.randomElement()!!.readBytes()

inline fun <reified E : Enum<E>> randomEnum(): E = E::class.java.enumConstants.randomElement()

fun <T> List<T>.randomElement(): T = this[randomInt(size - 1)]
fun <T> Array<T>.randomElement(): T = this[randomInt(size - 1)]

// chance of happening is 1/chance
fun <T> T.sometimesNull(chance: Int = 20) = this.takeIf { randomInt(chance) < chance }
