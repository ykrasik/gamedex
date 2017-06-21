package com.gitlab.ykrasik.gamedex.core

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.util.toMultiMap
import difflib.DiffUtils
import difflib.Patch
import org.joda.time.DateTime

/**
 * User: ykrasik
 * Date: 17/06/2017
 * Time: 16:02
 */
data class ReportConfig(
    val name: String = "",
    val filters: ReportRule = ReportRule.Rules.True(),
    val rules: ReportRule = ReportRule.Rules.True()
) {
    inline fun withFilters(f: (ReportRule) -> ReportRule) = copy(filters = f(filters))
    inline fun withRules(f: (ReportRule) -> ReportRule) = copy(rules = f(rules))
}

// FIXME: Reverse the logic - only games that pass the rules are shown, along with the value.

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ReportRule.Operators.And::class, name = "and"),
    JsonSubTypes.Type(value = ReportRule.Operators.Or::class, name = "or"),
    JsonSubTypes.Type(value = ReportRule.Operators.Not::class, name = "not"),

    JsonSubTypes.Type(value = ReportRule.Rules.True::class, name = "true"),
    JsonSubTypes.Type(value = ReportRule.Rules.PlatformRule::class, name = "platform"),
    JsonSubTypes.Type(value = ReportRule.Rules.HasCriticScore::class, name = "hasCriticScore"),
    JsonSubTypes.Type(value = ReportRule.Rules.HasUserScore::class, name = "hasUserScore"),
    JsonSubTypes.Type(value = ReportRule.Rules.CriticScore::class, name = "targetCriticScore"),
    JsonSubTypes.Type(value = ReportRule.Rules.UserScore::class, name = "targetUserScore"),
    JsonSubTypes.Type(value = ReportRule.Rules.NoDuplications::class, name = "duplications"),
    JsonSubTypes.Type(value = ReportRule.Rules.NameDiff::class, name = "nameDiff")
)
@JsonIgnoreProperties("name")
sealed class ReportRule {
    abstract val name: String
    abstract fun check(game: Game, context: ReportContext): RuleResult

    override fun toString() = name

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
                return when (leftResult) {
                    is RuleResult.Fail -> leftResult    // TODO: Aggregate failures.
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
                return when (leftResult) {
                    is RuleResult.Pass -> leftResult
                    else -> rightResult  // TODO: Aggregate failures.
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
                    is RuleResult.Pass -> RuleResult.Fail(null, rule)  // TODO: Add value
                }
            }

            override fun new(newRule: ReportRule) = Not(newRule)
            override fun toString() = "not($rule)"
        }
    }

    object Rules {
        val all = emptyMap<String, () -> Rule>() +
            (True().name to { True() }) +
            (PlatformRule().name to { PlatformRule() }) +
            (HasCriticScore().name to { HasCriticScore() }) +
            (HasUserScore().name to { HasUserScore() }) +
            (CriticScore().name to { CriticScore() }) +
            (UserScore().name to { UserScore() }) +
            (NoDuplications().name to { NoDuplications() }) +
            (NameDiff().name to { NameDiff() })

        abstract class Rule(override val name: String) : ReportRule()

        class True : Rule("True") {
            override fun check(game: Game, context: ReportContext) = RuleResult.Pass
        }

        class PlatformRule(val platform: Platform = Platform.pc) : Rule("Platform") {
            override fun check(game: Game, context: ReportContext): RuleResult =
                if (game.platform == platform) RuleResult.Pass else RuleResult.Fail(game.platform, this)

            override fun toString() = "platform == $platform"
        }

        abstract class HasScoreRule(name: String) : Rule("Has $name") {
            override fun check(game: Game, context: ReportContext) = extractScore(game).let { score ->
                if (score != null) RuleResult.Pass else RuleResult.Fail(score, this)
            }

            abstract protected fun extractScore(game: Game): Double?
        }

        class HasCriticScore : HasScoreRule("Critic Score") {
            override fun extractScore(game: Game) = game.criticScore?.score
        }

        class HasUserScore : HasScoreRule("User Score") {
            override fun extractScore(game: Game) = game.criticScore?.score
        }

        abstract class TargetScoreRule(name: String, val target: Double, val greaterThan: Boolean) : Rule(name) {
            override fun check(game: Game, context: ReportContext) = extractScore(game).let { score ->
                val result = if (greaterThan) (score ?: -1.0 >= target) else (score ?: -1.0 <= target)
                if (result) RuleResult.Pass else RuleResult.Fail(score, this)
            }

            abstract protected fun extractScore(game: Game): Double?
            override fun toString() = "$name ${if (greaterThan) ">=" else "<="} $target"
        }

        class CriticScore(target: Double = 60.0, isGt: Boolean = true) : TargetScoreRule("Critic Score", target, isGt) {
            override fun extractScore(game: Game) = game.criticScore?.score
        }

        class UserScore(target: Double = 60.0, isGt: Boolean = true) : TargetScoreRule("User Score", target, isGt) {
            override fun extractScore(game: Game) = game.userScore?.score
        }

        // TODO: Add ignore case option
        // TODO: Add option that makes metadata an optional match.
        class NoDuplications : Rule("No Duplications") {
            override fun check(game: Game, context: ReportContext): RuleResult {
                val allDuplications = calculate(context)
                val duplication = allDuplications[game]
                return if (duplication != null) RuleResult.Fail(duplication, this) else RuleResult.Pass
            }

            private fun calculate(context: ReportContext) = context.getOrPut("NoDuplications.result") {
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
            }

            private fun ProviderHeader.withoutUpdateDate() = copy(updateDate = DateTime(0))
        }

        data class GameDuplication(
            val providerId: ProviderId,
            val duplicatedGame: Game
        )

        class NameDiff : Rule("Name-Folder Diff") {
            override fun check(game: Game, context: ReportContext): RuleResult {
                val allDiffs = calculate(context)
                val diff = allDiffs[game]
                return if (diff != null) RuleResult.Fail(diff, this) else RuleResult.Pass
            }

            private fun calculate(context: ReportContext) = context.getOrPut("NameDiff.result") {
                context.games.flatMap { game ->
                    // TODO: If the majority of providers agree with the name, it is not a diff.
                    game.rawGame.providerData.mapNotNull { providerData ->
                        val difference = diff(game, providerData) ?: return@mapNotNull null
                        game to difference
                    }
                }.toMultiMap()
            }

            private fun diff(game: Game, providerData: ProviderData): GameNameFolderDiff? {
                val actualName = game.folderMetaData.rawName
                val expectedName = expectedFrom(game.folderMetaData, providerData)
                if (actualName == expectedName) return null

                val patch = DiffUtils.diff(actualName.toList(), expectedName.toList())
                return GameNameFolderDiff(
                    providerId = providerData.header.id,
                    actualName = actualName,
                    expectedName = expectedName,
                    patch = patch
                )
            }

            // TODO: This logic looks like it should sit on FolderMetaData.
            private fun expectedFrom(actual: FolderMetaData, providerData: ProviderData): String {
                val expected = StringBuilder()
                actual.order?.let { order -> expected.append("[$order] ") }
                expected.append(NameHandler.toFileName(providerData.gameData.name))
                actual.metaTag?.let { metaTag -> expected.append(" [$metaTag]") }
                actual.version?.let { version -> expected.append(" [$version]") }
                return expected.toString()
            }
        }

        data class GameNameFolderDiff(
            val providerId: ProviderId,
            val actualName: String,
            val expectedName: String,
            val patch: Patch<Char>
        )
    }

    data class ReportContext(val games: List<Game>) {
        private val properties = mutableMapOf<String, Any>()
        operator fun get(key: String) = properties[key]

        @Suppress("UNCHECKED_CAST")
        fun <T> getOrPut(key: String, defaultValue: () -> T) = properties.getOrPut(key, defaultValue as () -> Any) as T
    }
}

sealed class RuleResult {
    object Pass : RuleResult()
    data class Fail(val value: Any?, val rule: ReportRule) : RuleResult()  // TODO: Add a 'cause' rule?
}