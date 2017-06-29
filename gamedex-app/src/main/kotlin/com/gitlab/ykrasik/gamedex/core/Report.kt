package com.gitlab.ykrasik.gamedex.core

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.util.MultiMap
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
    val rules: ReportRule = ReportRule.Rules.True(),
    val excludedGames: List<Int> = emptyList()
) {
    inline fun withRules(f: (ReportRule) -> ReportRule) = copy(rules = f(rules))
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ReportRule.Operators.And::class, name = "and"),
    JsonSubTypes.Type(value = ReportRule.Operators.Or::class, name = "or"),
    JsonSubTypes.Type(value = ReportRule.Operators.Not::class, name = "not"),

    JsonSubTypes.Type(value = ReportRule.Rules.True::class, name = "true"),

    JsonSubTypes.Type(value = ReportRule.Rules.CriticScore::class, name = "targetCriticScore"),
    JsonSubTypes.Type(value = ReportRule.Rules.UserScore::class, name = "targetUserScore"),
    JsonSubTypes.Type(value = ReportRule.Rules.MinScore::class, name = "targetMinScore"),
    JsonSubTypes.Type(value = ReportRule.Rules.AvgScore::class, name = "targetAvgScore"),

    JsonSubTypes.Type(value = ReportRule.Rules.HasPlatform::class, name = "hasPlatform"),
    JsonSubTypes.Type(value = ReportRule.Rules.HasProvider::class, name = "hasProvider"),
    JsonSubTypes.Type(value = ReportRule.Rules.HasLibrary::class, name = "hasLibrary"),
    JsonSubTypes.Type(value = ReportRule.Rules.HasTag::class, name = "hasTag"),
    JsonSubTypes.Type(value = ReportRule.Rules.HasCriticScore::class, name = "hasCriticScore"),
    JsonSubTypes.Type(value = ReportRule.Rules.HasUserScore::class, name = "hasUserScore"),
    JsonSubTypes.Type(value = ReportRule.Rules.HasMinScore::class, name = "hasMinScore"),
    JsonSubTypes.Type(value = ReportRule.Rules.HasAvgScore::class, name = "hasAvgScore"),

    JsonSubTypes.Type(value = ReportRule.Rules.Duplications::class, name = "duplications"),
    JsonSubTypes.Type(value = ReportRule.Rules.NameDiff::class, name = "nameDiff")
)
@JsonIgnoreProperties("name", "ruleId")
sealed class ReportRule(val name: String) {
    abstract fun evaluate(game: Game, context: Context): Boolean
    open val ruleId get() = name
    override fun toString() = ruleId

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
        abstract class Operator(name: String) : ReportRule(name)

        abstract class BinaryOperator(name: String, val left: ReportRule, val right: ReportRule) : Operator(name) {
            fun map(f: (ReportRule) -> ReportRule): BinaryOperator = new(f(left), f(right))
            abstract fun new(newLeft: ReportRule, newRight: ReportRule): BinaryOperator
        }

        abstract class UnaryOperator(name: String, val rule: ReportRule) : Operator(name) {
            fun map(f: (ReportRule) -> ReportRule): UnaryOperator = new(f(rule))
            abstract fun new(newRule: ReportRule): UnaryOperator
        }

        class And(left: ReportRule, right: ReportRule) : BinaryOperator("And", left, right) {
            override fun evaluate(game: Game, context: Context): Boolean {
                val leftResult = left.evaluate(game, context)
                val rightResult = right.evaluate(game, context)
                return leftResult && rightResult    // Don't short-circuit to allow all rules to deposit their results in the context.
            }

            override fun new(newLeft: ReportRule, newRight: ReportRule) = And(newLeft, newRight)
            override fun toString() = "($left) and ($right)"
        }

        class Or(left: ReportRule, right: ReportRule) : BinaryOperator("Or", left, right) {
            override fun evaluate(game: Game, context: Context): Boolean {
                val leftResult = left.evaluate(game, context)
                val rightResult = right.evaluate(game, context)
                return leftResult || rightResult    // Don't short-circuit to allow all rules to deposit their results in the context.
            }

            override fun new(newLeft: ReportRule, newRight: ReportRule) = Or(newLeft, newRight)
            override fun toString() = "($left) or ($right)"
        }

        class Not(rule: ReportRule) : UnaryOperator("Not", rule) {
            override fun evaluate(game: Game, context: Context): Boolean {
                val result = rule.evaluate(game, context)
                return !result
            }

            override fun new(newRule: ReportRule) = Not(newRule)
            override fun toString() = "!($rule)"
        }
    }

    object Rules {
        abstract class Rule(name: String) : ReportRule(name)

        abstract class SimpleRule<T>(name: String) : Rule(name) {
            override fun evaluate(game: Game, context: Context): Boolean {
                val value = extractValue(game, context)
                if (value is List<*>) {
                    value.forEach { context.addResult(game, rule = this, value = it) }
                } else {
                    context.addResult(game, rule = this, value = value)
                }
                return doCheck(value)
            }

            abstract protected fun extractValue(game: Game, context: Context): T
            abstract protected fun doCheck(value: T): Boolean
        }

        class True : Rule("True") {
            override fun evaluate(game: Game, context: Context) = true
        }

        abstract class HasRule(name: String) : SimpleRule<Boolean>("Has $name") {
            override fun extractValue(game: Game, context: Context) = extractNullableValue(game, context) != null
            protected abstract fun extractNullableValue(game: Game, context: Context): Any?
            override fun doCheck(value: Boolean) = value
        }

        class HasPlatform(val platform: Platform) : HasRule("Platform") {
            override fun extractNullableValue(game: Game, context: Context) = if (game.platform == platform) platform else null
            override val ruleId = "$name '$platform'"
        }

        class HasProvider(val providerId: ProviderId) : HasRule("Provider") {
            override fun extractNullableValue(game: Game, context: Context) = game.providerHeaders.find { it.id == providerId }
            override val ruleId = "$name '$providerId'"
        }

        class HasLibrary(val platform: Platform, val libraryName: String) : HasRule("Library") {
            override fun extractNullableValue(game: Game, context: Context) = game.library.let { lib ->
                if (lib.platform == platform && lib.name == libraryName) lib else null
            }
            override val ruleId = "$name '[$platform] $libraryName'"
        }

        class HasTag(val tag: String) : HasRule("Tag") {
            override fun extractNullableValue(game: Game, context: Context) = game.tags.find { it == tag }
            override val ruleId = "$name '$tag'"
        }

        class HasCriticScore : HasRule("Critic Score") {
            override fun extractNullableValue(game: Game, context: Context) = game.criticScore?.score
        }

        class HasUserScore : HasRule("User Score") {
            override fun extractNullableValue(game: Game, context: Context) = game.userScore?.score
        }

        class HasMinScore : HasRule("Min Score") {
            override fun extractNullableValue(game: Game, context: Context) = game.minScore
        }

        class HasAvgScore : HasRule("Avg Score") {
            override fun extractNullableValue(game: Game, context: Context) = game.avgScore
        }

        abstract class TargetScoreRule(name: String, val target: Double, @get:JsonProperty("greaterThan") val greaterThan: Boolean) : SimpleRule<Double?>(name) {
            override fun doCheck(value: Double?) = if (greaterThan) (value ?: -1.0 >= target) else (value ?: -1.0 <= target)
            override fun toString() = "$name ${if (greaterThan) ">=" else "<="} $target"
        }

        class CriticScore(target: Double, greaterThan: Boolean) : TargetScoreRule("Critic Score", target, greaterThan) {
            override fun extractValue(game: Game, context: Context) = game.criticScore?.score
        }

        class UserScore(target: Double, greaterThan: Boolean) : TargetScoreRule("User Score", target, greaterThan) {
            override fun extractValue(game: Game, context: Context) = game.userScore?.score
        }

        class MinScore(target: Double, greaterThan: Boolean) : TargetScoreRule("Min Score", target, greaterThan) {
            override fun extractValue(game: Game, context: Context) = game.minScore
        }

        class AvgScore(target: Double, greaterThan: Boolean) : TargetScoreRule("Avg Score", target, greaterThan) {
            override fun extractValue(game: Game, context: Context) = game.avgScore
        }

        // TODO: Add ignore case option
        // TODO: Add option that makes metadata an optional match.
        class Duplications : SimpleRule<List<ReportRule.Rules.GameDuplication>>("Duplications") {
            override fun extractValue(game: Game, context: Context): List<GameDuplication> {
                val duplicates = context.cache("NoDuplications.result") {
                    val headerToGames = context.games.asSequence()
                        .flatMap { checkedGame -> checkedGame.providerHeaders.asSequence().map { it.withoutUpdateDate() to checkedGame } }
                        .toMultiMap()

                    // TODO: Does this belong here?
                    // Only detect duplications in the same platform.
                    val duplicateHeaders = headerToGames
                        .mapValues { (_, games) -> games.groupBy { it.platform }.filterValues { it.size > 1 }.flatMap { it.value } }
                        .filterValues { it.size > 1 }

                    duplicateHeaders.asSequence().flatMap { (header, games) ->
                        games.asSequence().flatMap { checkedGame ->
                            (games - checkedGame).asSequence().map { duplicatedGame ->
                                checkedGame to GameDuplication(header.id, duplicatedGame)
                            }
                        }
                    }.toMultiMap()
                }
                return duplicates[game] ?: emptyList()
            }

            override fun doCheck(value: List<GameDuplication>) = value.isNotEmpty()

            private fun ProviderHeader.withoutUpdateDate() = copy(updateDate = DateTime(0))
        }

        data class GameDuplication(
            val providerId: ProviderId,
            val duplicatedGame: Game
        )

        class NameDiff : SimpleRule<List<GameNameFolderDiff>>("Name-Folder Diff") {
            override fun extractValue(game: Game, context: Context): List<GameNameFolderDiff> {
                val diffs = context.cache("NameDiff.result") {
                    context.games.flatMap { checkedGame ->
                        // TODO: If the majority of providers agree with the name, it is not a diff.
                        checkedGame.rawGame.providerData.mapNotNull { providerData ->
                            val difference = diff(checkedGame, providerData) ?: return@mapNotNull null
                            checkedGame to difference
                        }
                    }.toMultiMap()
                }
                return diffs[game] ?: emptyList()
            }

            override fun doCheck(value: List<GameNameFolderDiff>) = value.isNotEmpty()

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

    data class Context(val games: List<Game>) {
        private val cache = mutableMapOf<String, Any>()
        private val _results = mutableMapOf<Game, MutableList<Result>>()
        val results: MultiMap<Game, Result> get() = _results

        @Suppress("UNCHECKED_CAST")
        fun <T> cache(key: String, defaultValue: () -> T) = cache.getOrPut(key, defaultValue as () -> Any) as T

        fun addResult(game: Game, rule: Rules.Rule, value: Any?) {
            val gameResults = _results.getOrPut(game) { mutableListOf() }
            val result = Result(rule.ruleId, value)
            if (!gameResults.contains(result)) gameResults += result
        }
    }

    data class Result(val ruleName: String, val value: Any?)
}