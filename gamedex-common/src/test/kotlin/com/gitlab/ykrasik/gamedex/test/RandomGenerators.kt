package com.gitlab.ykrasik.gamedex.test

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
fun randomName() = randomSentence(maxWords = 5, avgWordLength = 8, variance = 2)
fun randomScore() = rnd.nextDouble() * 100

fun randomDateTime(): DateTime = DateTime.now().plusSeconds((if (rnd.nextBoolean()) 1 else -1) * rnd.nextInt(999999999))
fun randomLocalDate(): LocalDate = randomDateTime().toLocalDate()
fun randomLocalDateString(): String = randomLocalDate().toString()

inline fun <reified E : Enum<E>> randomEnum(): E = E::class.java.enumConstants.randomElement()

fun <T> List<T>.randomElement(): T = this[rnd.nextInt(size)]
fun <T> Array<T>.randomElement(): T = this[rnd.nextInt(size)]