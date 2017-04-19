package com.gitlab.ykrasik.gamedex.common.testkit

import io.kotlintest.TestCase
import io.kotlintest.matchers.Matcher
import io.kotlintest.specs.StringSpec
import io.kotlintest.specs.WordSpec

/**
 * User: ykrasik
 * Date: 25/03/2017
 * Time: 11:26
 */
abstract class ScopedStringSpec : StringSpec() {
    fun <T> String.inScope(scope: T, test: T.() -> Unit): TestCase = this.invoke { test(scope) }
}

abstract class ScopedWordSpec : WordSpec() {
    fun <T> String.inScope(scope: T, test: T.() -> Unit): TestCase = this.invoke { test(scope) }
}

infix fun <T> Matcher<T>.and(other: Matcher<T>): Matcher<T> = object : Matcher<T> {
    override fun test(value: T) {
        this@and.test(value)
        other.test(value)
    }
}