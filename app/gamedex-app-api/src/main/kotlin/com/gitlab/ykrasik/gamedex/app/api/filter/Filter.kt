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

package com.gitlab.ykrasik.gamedex.app.api.filter

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.MultiMap
import com.gitlab.ykrasik.gamedex.util.toDate
import com.gitlab.ykrasik.gamedex.util.toMultiMap
import com.gitlab.ykrasik.gamedex.util.today
import difflib.DiffUtils
import difflib.Patch
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

        fun <T : Filter> name(klass: KClass<T>) = names[klass]!!
        val <T : Filter> KClass<T>.name get() = name(this)

        val `true` = True()
        fun not(delegate: () -> Filter) = delegate().not
    }

    val name get() = name(this::class)
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
                val newRule = doDelete(current.target)
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

    abstract class UnaryOperator(val target: Filter) : Operator() {
        fun map(f: (Filter) -> Filter): UnaryOperator = new(f(target))
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

    class Not(target: Filter = True()) : UnaryOperator(target) {
        override fun evaluate(game: Game, context: Context) = !target.evaluate(game, context)
        override fun new(newRule: Filter) = Not(newRule)
        override fun toString() = "!($target)"
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
            val size = context.size(game)
            return size >= target
        }
        override fun toString() = "$name >= ${target.humanReadable}"
    }

    class Platform(val platform: com.gitlab.ykrasik.gamedex.Platform) : Rule() {
        override fun evaluate(game: Game, context: Context) = game.platform == platform
        override fun toString() = "$name == '$platform'"
    }

    class Library(val id: Int) : Rule() {
        override fun evaluate(game: Game, context: Context) = game.library.id == id
        override fun toString() = "$name == Library($id)"
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
            val gameDuplications = allDuplications[game.id]
            if (gameDuplications != null) {
                context.addAdditionalInfo(game, this, gameDuplications)
            }
            return gameDuplications != null
        }

        private fun calcDuplications(context: Context): MultiMap<GameId, GameDuplication> = context.cache("Duplications.result") {
            val headerToGames = context.games.asSequence()
                .flatMap { checkedGame -> checkedGame.providerHeaders.asSequence().map { it.withoutTimestamp() to checkedGame } }
                .toMultiMap()

            // Only detect duplications in the same platform.
            val duplicateHeaders = headerToGames
                .mapValues { (_, games) -> games.groupBy { it.platform }.filterValues { it.size > 1 }.flatMap { it.value } }
                .filterValues { it.size > 1 }

            duplicateHeaders.asSequence().flatMap { (header, games) ->
                games.asSequence().flatMap { checkedGame ->
                    games.asSequence().mapNotNull { duplicatedGame ->
                        if (duplicatedGame != checkedGame) {
                            checkedGame.id to GameDuplication(header.id, duplicatedGame.id)
                        } else {
                            null
                        }
                    }
                }
            }.toMultiMap()
        }

        private fun ProviderHeader.withoutTimestamp() = copy(timestamp = Timestamp.zero)

        data class GameDuplication(
            val providerId: ProviderId,
            val duplicatedGameId: GameId
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
            val actualName = game.folderNameMetadata.rawName
            val expectedName = expectedFrom(game.folderNameMetadata, providerData, context)
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
        private fun expectedFrom(actual: FolderNameMetadata, providerData: ProviderData, context: Context): String {
            val expected = StringBuilder()
            actual.order?.let { order -> expected.append("[$order] ") }
            expected.append(context.toFileName(providerData.gameData.name))
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

    interface Context {
        val games: List<Game>
        val additionalData: Map<GameId, Set<AdditionalData>>

        fun size(game: Game): com.gitlab.ykrasik.gamedex.util.FileSize
        fun toFileName(name: String): String

        fun addAdditionalInfo(game: Game, rule: Rule, values: List<Any>)
        fun addAdditionalInfo(game: Game, rule: Rule, value: Any?)

        fun <T> cache(key: String, defaultValue: () -> T): T

        data class AdditionalData(val rule: KClass<out Filter.Rule>, val value: Any?)
    }
}