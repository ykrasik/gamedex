package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.util.toMultiMap
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 17/06/2017
 * Time: 16:02
 */
@Singleton
class ViolationsReportGenerator {
    fun generateReport(games: List<Game>, rule: ReportRule): Report<RuleResult.Fail> {
        val violations = games.mapNotNull { game ->
            val violation = rule.check(game)
            if (violation is RuleResult.Fail) game to violation else null
        }
        return violations.toMultiMap()
    }
}

sealed class ReportRule {
    abstract val name: String
    abstract fun check(game: Game): RuleResult

    fun and(other: ReportRule): ReportRule = And(this, other)

    fun replace(target: ReportRule, with: ReportRule): ReportRule {
        fun doReplace(current: ReportRule): ReportRule = when {
            current === target -> with
            current is ReportRule.CompositeRule -> current.map { doReplace(it) }
            else -> current
        }
        return doReplace(this)
    }

    fun delete(target: ReportRule): ReportRule? {
        fun doDelete(current: ReportRule): ReportRule? = when {
            current === target -> null
            current is ReportRule.CompositeRule -> {
                val newLeft = doDelete(current.left)
                val newRight = doDelete(current.right)
                when {
                    newLeft != null && newRight != null -> current.new(newLeft, newRight)
                    newLeft != null -> newLeft
                    else -> newRight
                }
            }
            else -> current
        }
        return doDelete(this)
    }

    class Nop : ReportRule() {
        override val name = "Nothing"
        override fun check(game: Game) = RuleResult.Pass
        override fun toString() = "nothing"
    }

    abstract class CompositeRule(val left: ReportRule, val right: ReportRule) : ReportRule() {
        fun map(f: (ReportRule) -> ReportRule): CompositeRule = new(f(left), f(right))
        abstract fun new(newLeft: ReportRule, newRight: ReportRule): CompositeRule
    }

    class And(left: ReportRule = Nop(), right: ReportRule = Nop()) : CompositeRule(left, right) {
        override val name = "And"
        override fun check(game: Game): RuleResult {
            val leftResult = left.check(game)
            val rightResult = right.check(game)
            return when {
                leftResult is RuleResult.Filter || rightResult is RuleResult.Filter -> RuleResult.Filter
                leftResult is RuleResult.Fail -> leftResult
                rightResult is RuleResult.Fail -> rightResult
                else -> RuleResult.Pass
            }
        }
        override fun new(newLeft: ReportRule, newRight: ReportRule) = And(newLeft, newRight)
        override fun toString() = "($left and $right)"
    }

    class Or(left: ReportRule = Nop(), right: ReportRule = Nop()) : CompositeRule(left, right) {
        override val name = "Or"
        override fun check(game: Game): RuleResult {
            val leftResult = left.check(game)
            val rightResult = right.check(game)
            return when {
                leftResult is RuleResult.Filter && rightResult is RuleResult.Filter -> RuleResult.Filter
                leftResult is RuleResult.Fail -> rightResult
                rightResult is RuleResult.Fail -> leftResult
                else -> RuleResult.Pass
            }
        }
        override fun new(newLeft: ReportRule, newRight: ReportRule) = Or(newLeft, newRight)
        override fun toString() = "($left or $right)"
    }

    class PlatformFilter(val platform: Platform = Platform.pc) : ReportRule() {
        override val name = "Platform"
        override fun check(game: Game): RuleResult = if (game.platform == platform) RuleResult.Pass else RuleResult.Filter
        override fun toString() = "platform == $platform"
    }

    class CriticScore(val min: Double = 65.0) : ReportRule() {
        override val name = "Critic Score"
        override fun check(game: Game): RuleResult = (game.criticScore?.score).let { score ->
            if (score ?: -1.0 >= min) RuleResult.Pass else RuleResult.Fail(score, this)
        }
        override fun toString() = "criticScore >= $min"
    }

    class UserScore(val min: Double = 65.0) : ReportRule() {
        override val name = "User Score"
        override fun check(game: Game): RuleResult = (game.userScore?.score).let { score ->
            if (score ?: -1.0 >= min) RuleResult.Pass else RuleResult.Fail(game.userScore?.score, this)
        }

        override fun toString() = "userScore >= $min"
    }

    companion object {
        val rules = emptyMap<String, () -> ReportRule>() +
            (Nop().name to { Nop() }) +
            (PlatformFilter().name to { PlatformFilter() }) +
            (CriticScore().name to { CriticScore() }) +
            (UserScore().name to { UserScore() }) +
            (And().name to { And() }) +
            (Or().name to { Or() })
    }
}

sealed class RuleResult {
    object Pass : RuleResult()
    data class Fail(val value: Any?, val rule: ReportRule) : RuleResult()
    object Filter : RuleResult()
}