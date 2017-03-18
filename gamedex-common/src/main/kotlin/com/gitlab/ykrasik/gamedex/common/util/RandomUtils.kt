package com.gitlab.ykrasik.gamedex.common.util

import org.joda.time.DateTime
import org.joda.time.LocalDate
import java.util.*

/**
 * User: ykrasik
 * Date: 18/03/2017
 * Time: 21:04
 *
 * This should be a test dependency, but I'm lazy.
 */
val rnd = Random()

private val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
fun randomString(length: Int = 20, variance: Int = 0): String {
    val str = StringBuilder()
    repeat(length.withVariance(variance)) {
        str.append(chars[rnd.nextInt(chars.length)])
    }
    return str.toString()
}

fun randomSentence(maxWords: Int = 10, avgWordLength: Int = 20, variance: Int = 5, delimiter: String = " "): String {
    val str = StringBuilder()
    repeat(maxOf(rnd.nextInt(maxWords), 3)) {
        str.append(randomString(avgWordLength, variance))
        str.append(delimiter)
    }
    return str.toString()
}

private fun Int.withVariance(variance: Int): Int =
    if (variance == 0) {
        this
    } else {
        val multiplier: Int = rnd.nextInt(variance) + 1
        if (rnd.nextBoolean()) (this * multiplier) else (this / multiplier)
    }

fun randomDateTime(): DateTime = DateTime(rnd.nextLong())
fun randomLocalDate(): LocalDate = randomDateTime().toLocalDate()

inline fun <reified E : Enum<E>> randomEnum(): E = E::class.java.enumConstants.randomElement()

fun <T> List<T>.randomElement(): T = this[rnd.nextInt(size)]
fun <T> Array<T>.randomElement(): T = this[rnd.nextInt(size)]