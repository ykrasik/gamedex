/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.JodaDateTime
import com.gitlab.ykrasik.gamedex.util.dateTimeOrNull
import com.gitlab.ykrasik.gamedex.util.humanReadable
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
    JsonSubTypes.Type(value = Filter.UserScore::class, name = "userScore"),
    JsonSubTypes.Type(value = Filter.AvgScore::class, name = "avgScore"),
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

    JsonSubTypes.Type(value = Filter.FileSize::class, name = "fileSize"),
    JsonSubTypes.Type(value = Filter.FileName::class, name = "fileName"),

    JsonSubTypes.Type(value = Filter.NameDiff::class, name = "nameDiff")
)
sealed class Filter {
    companion object {
        val Null = True()
        fun not(delegate: () -> Filter) = delegate().not
    }

    abstract fun evaluate(game: Game, context: Context): Boolean

    infix fun and(right: Filter) = And(listOf(this, right))
    infix fun and(right: () -> Filter) = and(right())
    infix fun or(right: Filter) = Or(listOf(this, right))
    infix fun or(right: () -> Filter) = or(right())
    val not get() = Not(this)

    abstract fun isEqual(other: Filter): Boolean
    protected inline fun <reified T> Filter.ifIs(f: (T) -> Boolean) = (this as? T)?.let(f) ?: false

    protected open fun evaluateNot(game: Game, context: Context): Boolean = !evaluate(game, context)

    abstract class MetaFilter : Filter()
    abstract class Compound : MetaFilter() {
        abstract val targets: List<Filter>

        protected inline fun <reified T : Compound> isEqual0(other: Filter) =
            other.ifIs<T> { this.targets == it.targets }
    }

    abstract class Modifier : MetaFilter() {
        abstract val target: Filter
    }

    class And(override val targets: List<Filter> = emptyList()) : Compound() {
        override fun evaluate(game: Game, context: Context) = targets.all { it.evaluate(game, context) }
        override fun evaluateNot(game: Game, context: Context) = targets.any { it.evaluateNot(game, context) }
        override fun isEqual(other: Filter) = isEqual0<And>(other)
        override fun toString() = targets.joinToString(separator = ") and (", prefix = "(", postfix = ")")
    }

    class Or(override val targets: List<Filter> = emptyList()) : Compound() {
        override fun evaluate(game: Game, context: Context) = targets.any { it.evaluate(game, context) }
        override fun evaluateNot(game: Game, context: Context) = targets.all { it.evaluateNot(game, context) }
        override fun isEqual(other: Filter) = isEqual0<Or>(other)
        override fun toString() = targets.joinToString(separator = ") or (", prefix = "(", postfix = ")")
    }

    class Not(override val target: Filter = True()) : Modifier() {
        override fun evaluate(game: Game, context: Context) = target.evaluateNot(game, context)
        override fun evaluateNot(game: Game, context: Context) = target.evaluate(game, context)
        override fun isEqual(other: Filter) = other.ifIs<Not> { this.target.isEqual(it.target) }
        override fun toString() = "!($target)"
    }

    abstract class Rule : Filter()

    class True : Rule() {
        override fun evaluate(game: Game, context: Context) = true
        override fun isEqual(other: Filter) = other is True
    }

    abstract class ScoreRule : Rule() {
        protected abstract fun extractScore(game: Game, context: Context): Double?
    }

    abstract class TargetScore : ScoreRule() {
        abstract val score: Double
        override fun evaluate(game: Game, context: Context) = eval(game, context) { it >= score }
        override fun evaluateNot(game: Game, context: Context) =
            if (score > 0.0) {
                eval(game, context) { it < score }
            } else {
                extractScore(game, context) == null
            }

        private inline fun eval(game: Game, context: Context, f: (Double) -> Boolean): Boolean {
            val score = extractScore(game, context)
            return score != null && f(score)
        }

        protected inline fun <reified T : TargetScore> isEqual0(other: Filter) =
            other.ifIs<T> { this.score == it.score }
    }

    class CriticScore(override val score: Double) : TargetScore() {
        override fun extractScore(game: Game, context: Context) = game.criticScore?.score
        override fun isEqual(other: Filter) = isEqual0<CriticScore>(other)
        override fun toString() = "Critic Score >= $score"
    }

    class UserScore(override val score: Double) : TargetScore() {
        override fun extractScore(game: Game, context: Context) = game.userScore?.score
        override fun isEqual(other: Filter) = isEqual0<UserScore>(other)
        override fun toString() = "User Score >= $score"
    }

    class AvgScore(override val score: Double) : TargetScore() {
        override fun extractScore(game: Game, context: Context) = game.avgScore
        override fun isEqual(other: Filter) = isEqual0<AvgScore>(other)
        override fun toString() = "Avg Score >= $score"
    }

    class MinScore(override val score: Double) : TargetScore() {
        override fun extractScore(game: Game, context: Context) = game.minScore
        override fun isEqual(other: Filter) = isEqual0<MinScore>(other)
        override fun toString() = "Min Score >= $score"
    }

    class MaxScore(override val score: Double) : TargetScore() {
        override fun extractScore(game: Game, context: Context) = game.maxScore
        override fun isEqual(other: Filter) = isEqual0<MaxScore>(other)
        override fun toString() = "Max Score >= $score"
    }

    abstract class DateRule : Rule() {
        protected abstract fun extractDate(game: Game, context: Context): DateTime?
    }

    abstract class TargetDate : DateRule() {
        abstract val date: LocalDate
        override fun evaluate(game: Game, context: Context) = eval(game, context) { it >= date }
        override fun evaluateNot(game: Game, context: Context) = eval(game, context) { it < date }

        private inline fun eval(game: Game, context: Context, f: (LocalDate) -> Boolean): Boolean {
            val date = extractDate(game, context)
            return date != null && f(date.toLocalDate())
        }

        protected inline fun <reified T : TargetDate> isEqual0(other: Filter) =
            other.ifIs<T> { this.date == it.date }
    }

    class TargetReleaseDate(override val date: LocalDate) : TargetDate() {
        override fun extractDate(game: Game, context: Context) = game.releaseDate?.dateTimeOrNull
        override fun isEqual(other: Filter) = isEqual0<TargetReleaseDate>(other)
        override fun toString() = "Release Date >= $date"
    }

    class TargetCreateDate(override val date: LocalDate) : TargetDate() {
        override fun extractDate(game: Game, context: Context) = game.createDate
        override fun isEqual(other: Filter) = isEqual0<TargetCreateDate>(other)
        override fun toString() = "Create Date >= $date"
    }

    class TargetUpdateDate(override val date: LocalDate) : TargetDate() {
        override fun extractDate(game: Game, context: Context) = game.updateDate
        override fun isEqual(other: Filter) = isEqual0<TargetUpdateDate>(other)
        override fun toString() = "Update Date >= $date"
    }

    abstract class PeriodDate : DateRule() {
        abstract val period: Period
        override fun evaluate(game: Game, context: Context) = eval(game, context) { it >= (context.now - period) }
        override fun evaluateNot(game: Game, context: Context) = eval(game, context) { it < (context.now - period) }

        private inline fun eval(game: Game, context: Context, f: (DateTime) -> Boolean): Boolean {
            val date = extractDate(game, context)
            return date != null && f(date)
        }

        protected inline fun <reified T : PeriodDate> isEqual0(other: Filter) =
            other.ifIs<T> { this.period == it.period }
    }

    class PeriodReleaseDate(override val period: Period) : PeriodDate() {
        override fun extractDate(game: Game, context: Context) = game.releaseDate?.dateTimeOrNull
        override fun isEqual(other: Filter) = isEqual0<PeriodReleaseDate>(other)
        override fun toString() = "Release Date >= (Now - ${period.humanReadable}"
    }

    class PeriodCreateDate(override val period: Period) : PeriodDate() {
        override fun extractDate(game: Game, context: Context) = game.createDate
        override fun isEqual(other: Filter) = isEqual0<PeriodCreateDate>(other)
        override fun toString() = "Create Date >= (Now - ${period.humanReadable}"
    }

    class PeriodUpdateDate(override val period: Period) : PeriodDate() {
        override fun extractDate(game: Game, context: Context) = game.updateDate
        override fun isEqual(other: Filter) = isEqual0<PeriodUpdateDate>(other)
        override fun toString() = "Update Date >= (Now - ${period.humanReadable}"
    }

    abstract class NullDate : DateRule() {
        override fun evaluate(game: Game, context: Context): Boolean {
            val date = extractDate(game, context)
            return date == null
        }

        override fun isEqual(other: Filter) = this::class == other::class
    }

    class NullReleaseDate : NullDate() {
        override fun extractDate(game: Game, context: Context) = game.releaseDate?.dateTimeOrNull
        override fun toString() = "Release Date == NULL"
    }

    class Platform(val platform: com.gitlab.ykrasik.gamedex.Platform) : Rule() {
        override fun evaluate(game: Game, context: Context) = game.platform == platform
        override fun isEqual(other: Filter) = other.ifIs<Platform> { this.platform == it.platform }
        override fun toString() = "Platform == '$platform'"
    }

    class Library(val id: Int) : Rule() {
        override fun evaluate(game: Game, context: Context) = game.library.id == id
        override fun isEqual(other: Filter) = other.ifIs<Library> { this.id == it.id }
        override fun toString() = "Library == Library($id)"
    }

    class Genre(val genre: String) : Rule() {
        override fun evaluate(game: Game, context: Context) = game.genres.any { it == genre }
        override fun isEqual(other: Filter) = other.ifIs<Genre> { this.genre == it.genre }
        override fun toString() = "Genre == '$genre'"
    }

    class Tag(val tag: String) : Rule() {
        override fun evaluate(game: Game, context: Context) = game.tags.any { it == tag }
        override fun isEqual(other: Filter) = other.ifIs<Tag> { this.tag == it.tag }
        override fun toString() = "Tag == '$tag'"
    }

    class Provider(val providerId: ProviderId) : Rule() {
        override fun evaluate(game: Game, context: Context) = eval(game, context) { it.any { it.header.id == providerId } }
        override fun evaluateNot(game: Game, context: Context) = eval(game, context) { it.none { it.header.id == providerId } }

        private inline fun eval(game: Game, context: Context, f: (List<ProviderData>) -> Boolean): Boolean {
            return context.providerSupports(providerId, game.platform) &&
                !game.isProviderExcluded(providerId) &&
                f(game.rawGame.providerData)
        }

        override fun isEqual(other: Filter) = other.ifIs<Provider> { this.providerId == it.providerId }
        override fun toString() = "Provider == '$providerId'"
    }

    class FileSize(val target: com.gitlab.ykrasik.gamedex.util.FileSize) : Rule() {
        override fun evaluate(game: Game, context: Context): Boolean {
            return game.fileTree.value?.let { it.size >= target } ?: false
        }

        override fun isEqual(other: Filter) = other.ifIs<FileSize> { this.target == it.target }
        override fun toString() = "File Size >= ${target.humanReadable}"
    }

    @JsonIgnoreProperties("regex")
    class FileName(val value: String) : Rule() {
        private val regex = value.toRegex()

        override fun evaluate(game: Game, context: Context): Boolean {
            return game.fileTree.value?.matches() ?: false
        }

        private fun FileTree.matches(): Boolean = name.matches(regex) || children.any { it.matches() }

        override fun isEqual(other: Filter) = other.ifIs<FileName> { this.value == it.value }
        override fun toString() = "File matches /$value/"
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

        override fun isEqual(other: Filter) = other is NameDiff
    }

    interface Context {
        val games: List<Game>
        val additionalData: Map<GameId, Set<AdditionalData>>
        val now: JodaDateTime

        fun providerSupports(providerId: ProviderId, platform: com.gitlab.ykrasik.gamedex.Platform): Boolean

        fun toFileName(name: String): String

        fun addAdditionalInfo(game: Game, rule: Rule, values: List<Any>)
        fun addAdditionalInfo(game: Game, rule: Rule, value: Any?)

        fun <T> cache(key: String, defaultValue: () -> T): T

        data class AdditionalData(val rule: KClass<out Filter.Rule>, val value: Any?)
    }
}

fun Filter.isEqual(other: Filter?): Boolean = other != null && isEqual(other)