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
import com.gitlab.ykrasik.gamedex.util.*
import difflib.DiffUtils
import difflib.Patch
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.Period
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
    JsonSubTypes.Type(value = Filter.NullCriticScore::class, name = "nullCriticScore"),

    JsonSubTypes.Type(value = Filter.UserScore::class, name = "userScore"),
    JsonSubTypes.Type(value = Filter.NullUserScore::class, name = "nullUserScore"),

    JsonSubTypes.Type(value = Filter.AvgScore::class, name = "avgScore"),
    JsonSubTypes.Type(value = Filter.NullAvgScore::class, name = "nullAvgScore"),

    JsonSubTypes.Type(value = Filter.MinScore::class, name = "minScore"),
    JsonSubTypes.Type(value = Filter.MaxScore::class, name = "maxScore"),

    JsonSubTypes.Type(value = Filter.TargetReleaseDate::class, name = "targetReleaseDate"),
    JsonSubTypes.Type(value = Filter.PeriodReleaseDate::class, name = "periodReleaseDate"),
    JsonSubTypes.Type(value = Filter.NullReleaseDate::class, name = "nullReleaseDate"),

    JsonSubTypes.Type(value = Filter.TargetUpdateDate::class, name = "targetUpdateDate"),
    JsonSubTypes.Type(value = Filter.PeriodUpdateDate::class, name = "periodUpdateDate"),

    JsonSubTypes.Type(value = Filter.TargetCreateDate::class, name = "targetCreateDate"),
    JsonSubTypes.Type(value = Filter.PeriodCreateDate::class, name = "periodCreateDate"),

    JsonSubTypes.Type(value = Filter.Platform::class, name = "platform"),
    JsonSubTypes.Type(value = Filter.Library::class, name = "library"),
    JsonSubTypes.Type(value = Filter.Genre::class, name = "genre"),
    JsonSubTypes.Type(value = Filter.Tag::class, name = "tag"),
    JsonSubTypes.Type(value = Filter.Provider::class, name = "provider"),
    JsonSubTypes.Type(value = Filter.FileSize::class, name = "size"),

    JsonSubTypes.Type(value = Filter.Duplications::class, name = "duplications"),
    JsonSubTypes.Type(value = Filter.NameDiff::class, name = "nameDiff")
)
// TODO: What if al the logic is moved into the context, and this will just be the structure?
sealed class Filter {
    companion object {
        val `true` = True()
        fun not(delegate: () -> Filter) = delegate().not
    }

    abstract fun evaluate(game: Game, context: Context): Boolean

    infix fun and(right: Filter) = And(this, right)
    infix fun and(right: () -> Filter) = and(right())
    infix fun or(right: Filter) = Or(this, right)
    infix fun or(right: () -> Filter) = or(right())
    val not get() = Not(this)

    abstract class Operator : Filter()
    abstract class BinaryOperator(val left: Filter, val right: Filter) : Operator()
    abstract class UnaryOperator(val target: Filter) : Operator()

    class And(left: Filter = True(), right: Filter = True()) : BinaryOperator(left, right) {
        override fun evaluate(game: Game, context: Context) = left.evaluate(game, context) && right.evaluate(game, context)
        override fun toString() = "($left) and ($right)"
    }

    class Or(left: Filter = True(), right: Filter = True()) : BinaryOperator(left, right) {
        override fun evaluate(game: Game, context: Context) = left.evaluate(game, context) || right.evaluate(game, context)
        override fun toString() = "($left) or ($right)"
    }

    class Not(target: Filter = True()) : UnaryOperator(target) {
        override fun evaluate(game: Game, context: Context) = !target.evaluate(game, context)
        override fun toString() = "!($target)"
    }

    abstract class Rule : Filter()

    class True : Rule() {
        override fun evaluate(game: Game, context: Context) = true
    }

    abstract class ScoreRule : Rule() {
        protected abstract fun extractScore(game: Game, context: Context): Double?
    }

    abstract class TargetScore(val target: Double) : ScoreRule() {
        override fun evaluate(game: Game, context: Context): Boolean {
            val score = extractScore(game, context)
            return score != null && score >= target
        }
    }

    abstract class NullScore : ScoreRule() {
        override fun evaluate(game: Game, context: Context): Boolean {
            val score = extractScore(game, context)
            return score == null
        }
    }

    class CriticScore(target: Double) : TargetScore(target) {
        override fun extractScore(game: Game, context: Context) = game.criticScore?.score
        override fun toString() = "Critic Score >= $target"
    }

    class NullCriticScore : NullScore() {
        override fun extractScore(game: Game, context: Context) = game.criticScore?.score
        override fun toString() = "Critic Score == NULL"
    }

    class UserScore(target: Double) : TargetScore(target) {
        override fun extractScore(game: Game, context: Context) = game.userScore?.score
        override fun toString() = "User Score >= $target"
    }

    class NullUserScore : NullScore() {
        override fun extractScore(game: Game, context: Context) = game.userScore?.score
        override fun toString() = "User Score == NULL"
    }

    class AvgScore(target: Double) : TargetScore(target) {
        override fun extractScore(game: Game, context: Context) = game.avgScore
        override fun toString() = "Avg Score >= $target"
    }

    class NullAvgScore : NullScore() {
        override fun extractScore(game: Game, context: Context) = game.avgScore
        override fun toString() = "Avg Score == NULL"
    }

    class MinScore(target: Double) : TargetScore(target) {
        override fun extractScore(game: Game, context: Context) = game.minScore
        override fun toString() = "Min Score >= $target"
    }

    class MaxScore(target: Double) : TargetScore(target) {
        override fun extractScore(game: Game, context: Context) = game.maxScore
        override fun toString() = "Max Score >= $target"
    }

    abstract class DateRule : Rule() {
        protected abstract fun extractDate(game: Game, context: Context): DateTime?
    }

    abstract class TargetDate(val date: LocalDate) : DateRule() {
        override fun evaluate(game: Game, context: Context): Boolean {
            val date = extractDate(game, context)
            return date != null && date.toLocalDate() >= this.date
        }
    }

    abstract class PeriodDate(val period: Period) : DateRule() {
        override fun evaluate(game: Game, context: Context): Boolean {
            val date = extractDate(game, context)
            return date != null && date >= (now - period)
        }
    }

    abstract class NullDate : DateRule() {
        override fun evaluate(game: Game, context: Context): Boolean {
            val date = extractDate(game, context)
            return date == null
        }
    }

    class TargetReleaseDate(date: LocalDate) : TargetDate(date) {
        override fun extractDate(game: Game, context: Context) = game.releaseDate?.toDateTimeOrNull()
        override fun toString() = "Release Date >= $date"
    }

    class PeriodReleaseDate(period: Period) : PeriodDate(period) {
        override fun extractDate(game: Game, context: Context) = game.releaseDate?.toDateTimeOrNull()
        override fun toString() = "Release Date >= (Now - ${period.toHumanReadable()}"
    }

    class NullReleaseDate : NullDate() {
        override fun extractDate(game: Game, context: Context) = game.releaseDate?.toDateTimeOrNull()
        override fun toString() = "Release Date == NULL"
    }

    class TargetCreateDate(date: LocalDate) : TargetDate(date) {
        override fun extractDate(game: Game, context: Context) = game.createDate
        override fun toString() = "Create Date >= $date"
    }

    class PeriodCreateDate(period: Period) : PeriodDate(period) {
        override fun extractDate(game: Game, context: Context) = game.createDate
        override fun toString() = "Create Date >= (Now - ${period.toHumanReadable()}"
    }

    class TargetUpdateDate(date: LocalDate) : TargetDate(date) {
        override fun extractDate(game: Game, context: Context) = game.updateDate
        override fun toString() = "Update Date >= $date"
    }

    class PeriodUpdateDate(period: Period) : PeriodDate(period) {
        override fun extractDate(game: Game, context: Context) = game.updateDate
        override fun toString() = "Update Date >= (Now - ${period.toHumanReadable()}"
    }

    class FileSize(val target: com.gitlab.ykrasik.gamedex.util.FileSize) : Rule() {
        override fun evaluate(game: Game, context: Context): Boolean {
            val size = context.size(game)
            return size >= target
        }

        override fun toString() = "File Size >= ${target.humanReadable}"
    }

    class Platform(val platform: com.gitlab.ykrasik.gamedex.Platform) : Rule() {
        override fun evaluate(game: Game, context: Context) = game.platform == platform
        override fun toString() = "Platform == '$platform'"
    }

    class Library(val id: Int) : Rule() {
        override fun evaluate(game: Game, context: Context) = game.library.id == id
        override fun toString() = "Library == Library($id)"
    }

    class Genre(val genre: String) : Rule() {
        override fun evaluate(game: Game, context: Context) = game.genres.any { it == genre }
        override fun toString() = "Genre == '$genre'"
    }

    class Tag(val tag: String) : Rule() {
        override fun evaluate(game: Game, context: Context) = game.tags.any { it == tag }
        override fun toString() = "Tag == '$tag'"
    }

    class Provider(val providerId: ProviderId) : Rule() {
        override fun evaluate(game: Game, context: Context) = game.providerHeaders.any { it.id == providerId }
        override fun toString() = "Provider == '$providerId'"
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