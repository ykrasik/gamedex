package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.test.*
import io.kotlintest.matchers.shouldBe

/**
 * User: ykrasik
 * Date: 18/06/2017
 * Time: 09:59
 */
class ReportRuleTest : ScopedWordSpec() {
    val game = randomGame()

    init {
        "ReportRule.Or" should {
            "fail when both sub-checks fail" {
                or(criticScore(90.0), userScore(90.0))
                    .check(, game.withCriticScore(89.0).withUserScore(89.0))::class shouldBe RuleResult.Fail::class
            }

            "succeed if one sub-check succeeds" {
                or(criticScore(90.0), userScore(90.0))
                    .check(, game.withCriticScore(91.0).withUserScore(89.0)) shouldBe RuleResult.Pass

                or(criticScore(90.0), userScore(90.0))
                    .check(, game.withCriticScore(89.0).withUserScore(91.0)) shouldBe RuleResult.Pass
            }

            "filter if both sub-checks filter" {
                or(filter(Platform.pc), filter(Platform.android))
                    .check(, game.withPlatform(Platform.xbox360))::class shouldBe RuleResult.Filter::class
            }

            "return the result of the other check if only 1 check filtered" {
                or(filter(Platform.android), userScore(90.0))
                    .check(, game.withPlatform(Platform.pc).withUserScore(89.0))::class shouldBe RuleResult.Fail::class

                or(filter(Platform.android), userScore(90.0))
                    .check(, game.withPlatform(Platform.pc).withUserScore(91.0)) shouldBe RuleResult.Pass

                or(userScore(90.0), filter(Platform.android))
                    .check(, game.withPlatform(Platform.pc).withUserScore(89.0))::class shouldBe RuleResult.Fail::class

                or(userScore(90.0), filter(Platform.android))
                    .check(, game.withPlatform(Platform.pc).withUserScore(91.0)) shouldBe RuleResult.Pass
            }
        }
    }

    fun filter(platform: Platform) = ReportRule.Filters.PlatformFilter(platform)
    fun criticScore(score: Double) = ReportRule.Rules.CriticScore(score)
    fun userScore(score: Double) = ReportRule.Rules.UserScore(score)
    fun or(left: ReportRule, right: ReportRule) = ReportRule.Operators.Or(left, right)
}