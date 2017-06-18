package com.gitlab.ykrasik.gamedex.core

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.ProviderHeader
import com.gitlab.ykrasik.gamedex.util.toMultiMap
import org.joda.time.DateTime

/**
 * User: ykrasik
 * Date: 17/06/2017
 * Time: 16:02
 */
data class ReportConfig(
    val name: String = "",
    val filters: ReportRule = ReportRule.Filters.True(),
    val rules: ReportRule = ReportRule.Rules.True()
) {
    inline fun withFilters(f: (ReportRule) -> ReportRule) = copy(filters = f(filters))
    inline fun withRules(f: (ReportRule) -> ReportRule) = copy(rules = f(rules))
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ReportRule.Operators.And::class, name = "and"),
    JsonSubTypes.Type(value = ReportRule.Operators.Or::class, name = "or"),
    JsonSubTypes.Type(value = ReportRule.Operators.Not::class, name = "not"),

    JsonSubTypes.Type(value = ReportRule.Filters.True::class, name = "trueFilter"),
    JsonSubTypes.Type(value = ReportRule.Filters.PlatformFilter::class, name = "platform"),
    JsonSubTypes.Type(value = ReportRule.Filters.HasCriticScoreFilter::class, name = "hasCriticScore"),
    JsonSubTypes.Type(value = ReportRule.Filters.HasUserScoreFilter::class, name = "hasUserScore"),

    JsonSubTypes.Type(value = ReportRule.Rules.True::class, name = "trueRule"),
    JsonSubTypes.Type(value = ReportRule.Rules.CriticScore::class, name = "criticScore"),
    JsonSubTypes.Type(value = ReportRule.Rules.UserScore::class, name = "userScore"),
    JsonSubTypes.Type(value = ReportRule.Rules.NoDuplications::class, name = "duplications")
)
@JsonIgnoreProperties("name")
sealed class ReportRule {
    abstract val name: String
    abstract fun check(game: Game, context: ReportContext): RuleResult

    override fun toString() = name.toLowerCase()

    fun replace(target: ReportRule, with: ReportRule): ReportRule {
        fun doReplace(current: ReportRule): ReportRule = when {
            current === target -> with
            current is Operators.BinaryOperator -> current.map { doReplace(it) }
            current is Operators.UnaryOperator -> current.map { doReplace(it) }
            else -> current
        }
        return doReplace(this)
    }

    fun delete(target: ReportRule): ReportRule? {
        fun doDelete(current: ReportRule): ReportRule? = when {
            current === target -> null
            current is Operators.BinaryOperator -> {
                val newLeft = doDelete(current.left)
                val newRight = doDelete(current.right)
                when {
                    newLeft != null && newRight != null -> current.new(newLeft, newRight)
                    newLeft != null -> newLeft
                    else -> newRight
                }
            }
            current is Operators.UnaryOperator -> {
                val newRule = doDelete(current.rule)
                if (newRule != null) current.new(newRule) else null
            }
            else -> current
        }
        return doDelete(this)
    }

    object Operators {
        val all = emptyMap<String, () -> Operator>() +
            (And().name to { And() }) +
            (Or().name to { Or() }) +
            (Not().name to { Not() })

        abstract class Operator : ReportRule()

        abstract class BinaryOperator(val left: ReportRule, val right: ReportRule) : Operator() {
            fun map(f: (ReportRule) -> ReportRule): BinaryOperator = new(f(left), f(right))
            abstract fun new(newLeft: ReportRule, newRight: ReportRule): BinaryOperator
        }

        abstract class UnaryOperator(val rule: ReportRule) : Operator() {
            fun map(f: (ReportRule) -> ReportRule): UnaryOperator = new(f(rule))
            abstract fun new(newRule: ReportRule): UnaryOperator
        }

        class And(left: ReportRule = Rules.True(), right: ReportRule = Rules.True()) : BinaryOperator(left, right) {
            override val name = "And"
            override fun check(game: Game, context: ReportContext): RuleResult {
                val leftResult = left.check(game, context)
                val rightResult = right.check(game, context)
                return when {
                    leftResult is RuleResult.Filter || rightResult is RuleResult.Filter -> RuleResult.Filter
                    leftResult is RuleResult.Fail -> leftResult
                    else -> rightResult
                }
            }

            override fun new(newLeft: ReportRule, newRight: ReportRule) = And(newLeft, newRight)
            override fun toString() = "($left and $right)"
        }

        class Or(left: ReportRule = Rules.True(), right: ReportRule = Rules.True()) : BinaryOperator(left, right) {
            override val name = "Or"
            override fun check(game: Game, context: ReportContext): RuleResult {
                val leftResult = left.check(game, context)
                val rightResult = right.check(game, context)
                return when {
                    leftResult is RuleResult.Pass || rightResult is RuleResult.Pass -> RuleResult.Pass
                    leftResult is RuleResult.Filter || rightResult is RuleResult.Filter -> RuleResult.Filter
                    else -> leftResult  // TODO: Both left & right are failures at this point - combine failures
                }
            }

            override fun new(newLeft: ReportRule, newRight: ReportRule) = Or(newLeft, newRight)
            override fun toString() = "($left or $right)"
        }

        class Not(rule: ReportRule = Rules.True()) : UnaryOperator(rule) {
            override val name = "Not"
            override fun check(game: Game, context: ReportContext): RuleResult {
                val result = rule.check(game, context)
                return when (result) {
                    is RuleResult.Fail -> RuleResult.Pass
                    is RuleResult.Filter -> RuleResult.Pass
                    is RuleResult.Pass ->
                        if (rule is Filters.Filter) RuleResult.Filter else RuleResult.Fail(null, rule)  // TODO: Add value
                }
            }

            override fun new(newRule: ReportRule) = Not(newRule)
            override fun toString() = "not($rule)"
        }
    }

    object Filters {
        val all = emptyMap<String, () -> Filter>() +
            (True().name to { True() }) +
            (PlatformFilter().name to { PlatformFilter() }) +
            (HasCriticScoreFilter().name to { HasCriticScoreFilter() }) +
            (HasUserScoreFilter().name to { HasUserScoreFilter() })

        abstract class Filter(override val name: String) : ReportRule() {
            override fun check(game: Game, context: ReportContext) = if (doCheck(game)) RuleResult.Pass else RuleResult.Filter
            protected abstract fun doCheck(game: Game): Boolean
        }

        class True : Filter("True") {
            override fun doCheck(game: Game) = true
        }

        class PlatformFilter(val platform: Platform = Platform.pc) : Filter("Platform") {
            override fun doCheck(game: Game) = game.platform == platform
            override fun toString() = "platform == $platform"
        }

        class HasCriticScoreFilter : Filter("Has Critic Score") {
            override fun doCheck(game: Game) = game.criticScore != null
        }

        class HasUserScoreFilter : Filter("Has User Score") {
            override fun doCheck(game: Game) = game.userScore != null
        }
    }

    object Rules {
        val all = emptyMap<String, () -> Rule>() +
            (True().name to { True() }) +
            (CriticScore().name to { CriticScore() }) +
            (UserScore().name to { UserScore() }) +
            (NoDuplications().name to { NoDuplications() })

        abstract class Rule(override val name: String) : ReportRule()

        class True : Rule("True") {
            override fun check(game: Game, context: ReportContext) = RuleResult.Pass
        }

        class CriticScore(val min: Double = 65.0) : Rule("Critic Score At Least") {
            override fun check(game: Game, context: ReportContext) = (game.criticScore?.score).let { score ->
                if (score ?: -1.0 >= min) RuleResult.Pass else RuleResult.Fail(score, this)
            }
            override fun toString() = "criticScore >= $min"
        }

        class UserScore(val min: Double = 65.0) : Rule("User Score At Least") {
            override fun check(game: Game, context: ReportContext) = (game.userScore?.score).let { score ->
                if (score ?: -1.0 >= min) RuleResult.Pass else RuleResult.Fail(score, this)
            }
            override fun toString() = "userScore >= $min"
        }

        class NoDuplications : Rule("No Duplications") {
            private val key = "NoDuplications.result"

            override fun check(game: Game, context: ReportContext): RuleResult {
                val allDuplications = calculate(context)
                val duplication = allDuplications[game]
                return if (duplication != null) RuleResult.Fail(duplication, this) else RuleResult.Pass
            }

            @Suppress("UNCHECKED_CAST")
            private fun calculate(context: ReportContext): Report<GameDuplication> = context.getOrPut(key) {
                val headerToGames = context.games.asSequence()
                    .flatMap { game -> game.providerHeaders.asSequence().map { it.withoutUpdateDate() to game } }
                    .toMultiMap()

                // Only detect duplications in the same platform.
                val duplicateHeaders = headerToGames
                    .mapValues { (_, games) -> games.groupBy { it.platform }.filterValues { it.size > 1 }.flatMap { it.value } }
                    .filterValues { it.size > 1 }

                val duplicateGames = duplicateHeaders.asSequence().flatMap { (header, games) ->
                    games.asSequence().flatMap { game ->
                        (games - game).asSequence().map { duplicatedGame ->
                            game to GameDuplication(header.id, duplicatedGame)
                        }
                    }
                }.toMultiMap()

                duplicateGames
            } as Report<GameDuplication>

            private fun ProviderHeader.withoutUpdateDate() = copy(updateDate = DateTime(0))
        }
    }

    data class ReportContext(val games: List<Game>) {
        private val properties = mutableMapOf<String, Any>()
        operator fun get(key: String) = properties[key]
        fun getOrPut(key: String, defaultValue: () -> Any) = properties.getOrPut(key, defaultValue)
    }
}

sealed class RuleResult {
    object Pass : RuleResult()
    data class Fail(val value: Any?, val rule: ReportRule) : RuleResult()  // TODO: Add a 'cause' rule?
    object Filter : RuleResult()
}