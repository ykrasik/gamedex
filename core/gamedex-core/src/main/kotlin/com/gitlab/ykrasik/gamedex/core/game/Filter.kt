/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.core.game

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.gitlab.ykrasik.gamedex.FolderMetadata
import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.ProviderData
import com.gitlab.ykrasik.gamedex.ProviderHeader
import com.gitlab.ykrasik.gamedex.core.api.file.FileSystemService
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.MultiMap
import com.gitlab.ykrasik.gamedex.util.toDate
import com.gitlab.ykrasik.gamedex.util.toMultiMap
import com.gitlab.ykrasik.gamedex.util.today
import difflib.DiffUtils
import difflib.Patch
import org.joda.time.DateTime
import org.joda.time.LocalDate
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 17/06/2017
 * Time: 16:02
 */
// TODO: Separate Filters from ReportRules.
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = Filter.And::class, name = "and"),
    JsonSubTypes.Type(value = Filter.Or::class, name = "or"),
    JsonSubTypes.Type(value = Filter.Not::class, name = "not"),

    JsonSubTypes.Type(value = Filter.True::class, name = "true"),

    JsonSubTypes.Type(value = Filter.CriticScore::class, name = "criticScore"),
    JsonSubTypes.Type(value = Filter.UserScore::class, name = "userScore"),
    JsonSubTypes.Type(value = Filter.AvgScore::class, name = "avgScore"),

    JsonSubTypes.Type(value = Filter.Platform::class, name = "platform"),
    JsonSubTypes.Type(value = Filter.Library::class, name = "library"),
    JsonSubTypes.Type(value = Filter.Genre::class, name = "genre"),
    JsonSubTypes.Type(value = Filter.Tag::class, name = "tag"),
    JsonSubTypes.Type(value = Filter.ReleaseDate::class, name = "releaseDate"),
    JsonSubTypes.Type(value = Filter.Provider::class, name = "provider"),
    JsonSubTypes.Type(value = Filter.FileSize::class, name = "size"),

    JsonSubTypes.Type(value = Filter.Duplications::class, name = "duplications"),
    JsonSubTypes.Type(value = Filter.NameDiff::class, name = "nameDiff")
)
@JsonIgnoreProperties("name", "not")
sealed class Filter {
    companion object {
        private val names = mapOf(
            And::class to "And",
            Or::class to "Or",
            Not::class to "Not",
            True::class to "True",
            CriticScore::class to "Critic Score",
            UserScore::class to "User Score",
            AvgScore::class to "Avg Score",
            Platform::class to "Platform",
            Library::class to "Library",
            Genre::class to "Genre",
            Tag::class to "Tag",
            ReleaseDate::class to "Release Date",
            Provider::class to "Provider",
            FileSize::class to "File Size",
            Duplications::class to "Duplications",
            NameDiff::class to "Name-Folder Diff"
        )

        private val classes = names.map { (k, v) -> v to k }.toMap()

        inline fun <reified T> name() where T : Filter = name(T::class)
        fun <T : Filter> name(klass: KClass<T>) = names[klass]!!
        val <T : Filter> KClass<T>.name get() = name(this)

        val String.filterClass: KClass<out Filter> get() = classes[this]!!

        val `true` = True()
        val noCriticScore = CriticScore(ScoreRule.NoScore)
        val noUserScore = UserScore(ScoreRule.NoScore)

        fun not(delegate: () -> Filter) = delegate().not
    }

    val name get() = name(javaClass.kotlin)
    override fun toString() = name
    abstract fun evaluate(game: Game, context: Context): Boolean

    infix fun and(right: Filter) = And(this, right)
    infix fun and(right: () -> Filter) = and(right())
    infix fun or(right: Filter) = Or(this, right)
    infix fun or(right: () -> Filter) = or(right())
    val not get() = Not(this)

    fun replace(target: Filter, with: Filter): Filter {
        fun doReplace(current: Filter): Filter = when {
            current === target -> with
            current is BinaryOperator -> current.map { doReplace(it) }
            current is UnaryOperator -> current.map { doReplace(it) }
            else -> current
        }
        return doReplace(this)
    }

    fun delete(target: Filter): Filter? {
        fun doDelete(current: Filter): Filter? = when {
            current === target -> null
            current is BinaryOperator -> {
                val newLeft = doDelete(current.left)
                val newRight = doDelete(current.right)
                when {
                    newLeft != null && newRight != null -> current.new(newLeft, newRight)
                    newLeft != null -> newLeft
                    else -> newRight
                }
            }
            current is UnaryOperator -> {
                val newRule = doDelete(current.delegate)
                if (newRule != null) current.new(newRule) else null
            }
            else -> current
        }
        return doDelete(this)
    }

    abstract class Operator : Filter()

    abstract class BinaryOperator(val left: Filter, val right: Filter) : Operator() {
        fun map(f: (Filter) -> Filter): BinaryOperator = new(f(left), f(right))
        abstract fun new(newLeft: Filter, newRight: Filter): BinaryOperator
    }

    abstract class UnaryOperator(val delegate: Filter) : Operator() {
        fun map(f: (Filter) -> Filter): UnaryOperator = new(f(delegate))
        abstract fun new(newRule: Filter): UnaryOperator
    }

    class And(left: Filter = True(), right: Filter = True()) : BinaryOperator(left, right) {
        override fun evaluate(game: Game, context: Context) = left.evaluate(game, context) && right.evaluate(game, context)
        override fun new(newLeft: Filter, newRight: Filter) = And(newLeft, newRight)
        override fun toString() = "($left) and ($right)"
    }

    class Or(left: Filter = True(), right: Filter = True()) : BinaryOperator(left, right) {
        override fun evaluate(game: Game, context: Context) = left.evaluate(game, context) || right.evaluate(game, context)
        override fun new(newLeft: Filter, newRight: Filter) = Or(newLeft, newRight)
        override fun toString() = "($left) or ($right)"
    }

    class Not(delegate: Filter = True()) : UnaryOperator(delegate) {
        override fun evaluate(game: Game, context: Context) = !delegate.evaluate(game, context)
        override fun new(newRule: Filter) = Not(newRule)
        override fun toString() = "!($delegate)"
    }

    abstract class Rule : Filter()

    class True : Rule() {
        override fun evaluate(game: Game, context: Context) = true
    }

    abstract class ScoreRule(val target: Double) : Rule() {
        override fun evaluate(game: Game, context: Context): Boolean {
            val score = extractScore(game, context)
            return when {
                score == null -> target == NoScore
                target != NoScore -> score >= target
                else -> false
            }
        }

        protected abstract fun extractScore(game: Game, context: Context): Double?
        override fun toString() = "$name ${if (target == NoScore) "== null" else ">= $target"}"

        companion object {
            val NoScore = -1.0
        }
    }

    class CriticScore(target: Double) : ScoreRule(target) {
        override fun extractScore(game: Game, context: Context) = game.criticScore?.score
    }

    class UserScore(target: Double) : ScoreRule(target) {
        override fun extractScore(game: Game, context: Context) = game.userScore?.score
    }

    class AvgScore(target: Double) : ScoreRule(target) {
        override fun extractScore(game: Game, context: Context) = game.avgScore
    }

    class FileSize(val target: com.gitlab.ykrasik.gamedex.util.FileSize) : Rule() {
        override fun evaluate(game: Game, context: Context): Boolean {
            val size = context.fileSystemService.sizeSync(game.path)
            return size >= target
        }
        override fun toString() = "$name >= ${target.humanReadable}"
    }

    class Platform(val platform: com.gitlab.ykrasik.gamedex.Platform) : Rule() {
        override fun evaluate(game: Game, context: Context) = game.platform == platform
        override fun toString() = "$name == '$platform'"
    }

    class Library(val platform: com.gitlab.ykrasik.gamedex.Platform, val libraryName: String) : Rule() {
        override fun evaluate(game: Game, context: Context): Boolean = game.library.let { lib ->
            lib.platform == platform && lib.name == libraryName
        }
        override fun toString() = "$name == '[$platform] $libraryName'"
    }

    class Genre(val genre: String) : Rule() {
        override fun evaluate(game: Game, context: Context) = game.genres.any { it == genre }
        override fun toString() = "$name == '$genre'"
    }

    class Tag(val tag: String) : Rule() {
        override fun evaluate(game: Game, context: Context) = game.tags.any { it == tag }
        override fun toString() = "$name == '$tag'"
    }

    class ReleaseDate(val releaseDate: LocalDate) : Rule() {
        override fun evaluate(game: Game, context: Context): Boolean {
            val target = try {
                game.releaseDate!!.toDate()
            } catch (e: Exception) {
                today
            }
            return releaseDate.isBefore(target)
        }
        override fun toString() = "$name >= '$releaseDate'"
    }

    class Provider(val providerId: ProviderId) : Rule() {
        override fun evaluate(game: Game, context: Context) = game.providerHeaders.any { it.id == providerId }
        override fun toString() = "$name == '$providerId'"
    }

    // TODO: Add ignore case option
    // TODO: Add option that makes metadata an optional match.
    // TODO: This is not a filter.
    class Duplications : Rule() {
        override fun evaluate(game: Game, context: Context): Boolean {
            val allDuplications = calcDuplications(context)
            val gameDuplications = allDuplications[game]
            if (gameDuplications != null) {
                context.addAdditionalInfo(game, this, gameDuplications)
            }
            return gameDuplications != null
        }

        private fun calcDuplications(context: Context): MultiMap<Game, GameDuplication> = context.cache("Duplications.result") {
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

        private fun ProviderHeader.withoutUpdateDate() = copy(updateDate = DateTime(0))

        data class GameDuplication(
            val providerId: ProviderId,
            val duplicatedGame: Game
        )
    }

    class NameDiff : Rule() {
        override fun evaluate(game: Game, context: Context): Boolean {
            // TODO: If the majority of providers agree with the name, it is not a diff.
            val diffs = game.rawGame.providerData.mapNotNull { providerData ->
                diff(game, providerData, context)
            }
            if (diffs.isNotEmpty()) {
                context.addAdditionalInfo(game, this, diffs)
            }
            return diffs.isNotEmpty()
        }

        private fun diff(game: Game, providerData: ProviderData, context: Context): GameNameFolderDiff? {
            val actualName = game.folderMetadata.rawName
            val expectedName = expectedFrom(game.folderMetadata, providerData, context)
            if (actualName == expectedName) return null

            val patch = DiffUtils.diff(actualName.toList(), expectedName.toList())
            return GameNameFolderDiff(
                providerId = providerData.header.id,
                actualName = actualName,
                expectedName = expectedName,
                patch = patch
            )
        }

        // TODO: This logic looks like it should sit on FolderMetadata.
        private fun expectedFrom(actual: FolderMetadata, providerData: ProviderData, context: Context): String {
            val expected = StringBuilder()
            actual.order?.let { order -> expected.append("[$order] ") }
            expected.append(context.fileSystemService.toFileName(providerData.gameData.name))
            actual.metaTag?.let { metaTag -> expected.append(" [$metaTag]") }
            actual.version?.let { version -> expected.append(" [$version]") }
            return expected.toString()
        }

        data class GameNameFolderDiff(
            val providerId: ProviderId,
            val actualName: String,
            val expectedName: String,
            val patch: Patch<Char>
        )

    }

    data class Context(
        val games: List<Game>,
        val fileSystemService: FileSystemService
    ) {
        private val cache = mutableMapOf<String, Any>()
        private val _additionalData = mutableMapOf<Game, MutableList<AdditionalData>>()
        val additionalData: MultiMap<Game, AdditionalData> get() = _additionalData

        @Suppress("UNCHECKED_CAST")
        fun <T> cache(key: String, defaultValue: () -> T) = cache.getOrPut(key, defaultValue as () -> Any) as T

        fun addAdditionalInfo(game: Game, rule: Rule, values: List<Any>) =
            values.forEach { addAdditionalInfo(game, rule, it) }

        fun addAdditionalInfo(game: Game, rule: Rule, value: Any?) {
            val gameAdditionalInfo = _additionalData.getOrPut(game) { mutableListOf() }
            val additionalInfo = AdditionalData(rule::class, value)
            if (!gameAdditionalInfo.contains(additionalInfo)) gameAdditionalInfo += additionalInfo
        }
    }

    data class AdditionalData(val rule: KClass<out Rule>, val value: Any?)
}