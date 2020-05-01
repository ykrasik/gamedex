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
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import io.kotlintest.Spec
import io.kotlintest.TestCaseContext
import io.kotlintest.matchers.*
import io.kotlintest.specs.WordSpec
import kotlinx.coroutines.runBlocking
import org.joda.time.DateTime
import org.joda.time.DateTimeUtils
import org.joda.time.DateTimeZone

/**
 * User: ykrasik
 * Date: 25/03/2017
 * Time: 11:26
 */
abstract class Spec<Scope> : WordSpec() {
    override val oneInstancePerTest = false

    private val now = DateTime(1).withZone(DateTimeZone.UTC)
    val nowMock = Timestamp(createDate = now, updateDate = now)

    init {
        DateTimeUtils.setCurrentMillisFixed(now.millis)
    }

    override fun interceptSpec(context: Spec, spec: () -> Unit) {
        beforeAll()
        spec()
        afterAll()
    }

    override fun interceptTestCase(context: TestCaseContext, test: () -> Unit) {
        beforeEach()
        test()
        afterEach()
    }

    abstract fun scope(): Scope

    protected open fun beforeAll() {}
    protected open fun afterAll() {}
    protected open fun beforeEach() {}
    protected open fun afterEach() {}

    infix fun String.test(test: suspend Scope.() -> Unit) = this.invoke { runBlocking { test(scope()) } }
}

fun Score?.assertScore(min: Number, max: Number, numReviews: Int): Score {
    this shouldNotBe null
    this!!.score should (beGreaterThanOrEqualTo(min.toDouble()) and beLessThanOrEqualTo(max.toDouble()))
    this.numReviews should beGreaterThanOrEqualTo(numReviews)
    return this
}

fun have1SearchResultWhere(f: GameProvider.SearchResult.() -> Unit) = object : Matcher<List<GameProvider.SearchResult>> {
    override fun test(value: List<GameProvider.SearchResult>): Result {
        value should haveSize(1)
        f(value.first())
        return Result(true, "")
    }
}

fun have2SearchResultsWhere(f: (first: GameProvider.SearchResult, second: GameProvider.SearchResult) -> Unit) = object : Matcher<List<GameProvider.SearchResult>> {
    override fun test(value: List<GameProvider.SearchResult>): Result {
        value should haveSize(2)
        f(value[0], value[1])
        return Result(true, "")
    }
}