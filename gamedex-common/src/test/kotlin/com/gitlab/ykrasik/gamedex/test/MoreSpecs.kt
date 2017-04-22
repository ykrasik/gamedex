package com.gitlab.ykrasik.gamedex.test

import io.kotlintest.TestCase
import io.kotlintest.specs.StringSpec
import io.kotlintest.specs.WordSpec

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

    fun <T> String.inScope(scope: T, test: T.() -> Unit): TestCase = this.invoke { test(scope) }
    fun <T> String.inLazyScope(scope: () -> T, test: T.() -> Unit): TestCase = this.invoke { test(scope()) }
}