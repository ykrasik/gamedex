/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.*
import org.joda.time.DateTime
import org.joda.time.DateTimeUtils
import org.joda.time.DateTimeZone

/**
 * User: ykrasik
 * Date: 25/03/2017
 * Time: 11:26
 */
abstract class Spec<Scope> : DescribeSpec() {
    override fun isolationMode() = IsolationMode.SingleInstance

    val now = DateTime(10000000).withZone(DateTimeZone.UTC)
    val nowTimestamp = Timestamp(createDate = now, updateDate = now)

    init {
        DateTimeUtils.setCurrentMillisFixed(now.millis)
    }

    override suspend fun beforeSpec(spec: Spec) {
        beforeAll()
    }

    override suspend fun afterSpec(spec: Spec) {
        afterAll()
    }

    override suspend fun beforeTest(testCase: TestCase) {
        beforeEach()

    }

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        afterEach()
    }

    abstract fun scope(): Scope

    protected open fun beforeAll() {}
    protected open fun afterAll() {}
    protected open fun beforeEach() {}
    protected open fun afterEach() {}

    suspend fun DescribeSpecContainerScope.itShould(name: String, test: suspend Scope.() -> Unit) {
        it(name) { test(scope()) }
    }
}

fun Score?.assertScore(min: Number, max: Number, numReviews: Int): Score {
    this shouldNotBe null
    this!!.score should (io.kotest.matchers.doubles.beGreaterThanOrEqualTo(min.toDouble()) and io.kotest.matchers.doubles.beLessThanOrEqualTo(
        max.toDouble()
    ))
    this.numReviews should io.kotest.matchers.ints.beGreaterThanOrEqualTo(numReviews)
    return this
}

fun have1SearchResultWhere(f: GameProvider.SearchResult.() -> Unit) = object : Matcher<List<GameProvider.SearchResult>> {
    override fun test(value: List<GameProvider.SearchResult>): MatcherResult {
        value should io.kotest.matchers.collections.haveSize(1)
        f(value.first())
        return MatcherResult.Companion.invoke(true, { "" }, { "" })
    }
}

fun have2SearchResultsWhere(f: (first: GameProvider.SearchResult, second: GameProvider.SearchResult) -> Unit) =
    object : Matcher<List<GameProvider.SearchResult>> {
        override fun test(value: List<GameProvider.SearchResult>): MatcherResult {
            value should io.kotest.matchers.collections.haveSize(2)
            f(value[0], value[1])
            return MatcherResult.Companion.invoke(true, { "" }, { "" })
        }
    }
