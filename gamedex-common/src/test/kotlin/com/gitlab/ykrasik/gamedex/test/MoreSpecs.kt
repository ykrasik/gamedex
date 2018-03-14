package com.gitlab.ykrasik.gamedex.test

import com.gitlab.ykrasik.gamedex.Score
import io.kotlintest.TestCase
import io.kotlintest.matchers.beGreaterThanOrEqualTo
import io.kotlintest.matchers.beLessThanOrEqualTo
import io.kotlintest.matchers.should
import io.kotlintest.matchers.shouldNotBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.specs.WordSpec
import org.joda.time.DateTime
import org.joda.time.DateTimeUtils
import org.joda.time.DateTimeZone

/**
 * User: ykrasik
 * Date: 25/03/2017
 * Time: 11:26
 */
abstract class ScopedStringSpec : StringSpec() {
    override val oneInstancePerTest = false

    fun <T> String.inScope(scope: T, test: T.() -> Unit): TestCase = this.invoke { test(scope) }
}

abstract class ScopedWordSpec : WordSpec() {
    override val oneInstancePerTest = false

    val nowMock = DateTime(1).withZone(DateTimeZone.UTC)
    init {
        DateTimeUtils.setCurrentMillisFixed(1)
    }

    fun <T> String.inScope(scope: () -> T, test: T.() -> Unit): TestCase = this.invoke { test(scope()) }
}

fun Score?.assertScore(min: Number, max: Number, numReviews: Int): Score {
    this shouldNotBe null
    this!!.score should (beGreaterThanOrEqualTo(min.toDouble()) and beLessThanOrEqualTo(max.toDouble()))
    this.numReviews should beGreaterThanOrEqualTo(numReviews)
    return this
}