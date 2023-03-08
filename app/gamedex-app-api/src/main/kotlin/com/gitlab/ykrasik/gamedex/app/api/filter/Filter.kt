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

package com.gitlab.ykrasik.gamedex.app.api.filter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.JodaDateTime
import com.gitlab.ykrasik.gamedex.util.dateTimeOrNull
import com.gitlab.ykrasik.gamedex.util.humanReadable
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.Period
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 17/06/2017
 * Time: 16:02
 */
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
    JsonSubTypes.Type(value = Filter.FilterTag::class, name = "filterTag"),
    JsonSubTypes.Type(value = Filter.Provider::class, name = "provider"),

    JsonSubTypes.Type(value = Filter.FileName::class, name = "fileName"),
    JsonSubTypes.Type(value = Filter.FileSize::class, name = "fileSize")
)
sealed class Filter {
    companion object {
        val Null: Filter = True()
    }

    abstract fun evaluate(game: Game, context: Context): Boolean

    protected open fun evaluateNot(game: Game, context: Context): Boolean = !evaluate(game, context)

    abstract class MetaFilter : Filter()
    abstract class Compound : MetaFilter() {
        abstract val targets: List<Filter>
    }

    abstract class Modifier : MetaFilter() {
        abstract val target: Filter
    }

    data class And(override val targets: List<Filter>) : Compound() {
        override fun evaluate(game: Game, context: Context) = targets.all { it.evaluate(game, context) }
        override fun evaluateNot(game: Game, context: Context) = targets.any { it.evaluateNot(game, context) }
        override fun toString() = targets.joinToString(separator = ") and (", prefix = "(", postfix = ")")
    }

    data class Or(override val targets: List<Filter>) : Compound() {
        override fun evaluate(game: Game, context: Context) = targets.any { it.evaluate(game, context) }
        override fun evaluateNot(game: Game, context: Context) = targets.all { it.evaluateNot(game, context) }
        override fun toString() = targets.joinToString(separator = ") or (", prefix = "(", postfix = ")")
    }

    data class Not(override val target: Filter) : Modifier() {
        override fun evaluate(game: Game, context: Context) = target.evaluateNot(game, context)
        override fun evaluateNot(game: Game, context: Context) = target.evaluate(game, context)
        override fun toString() = "!($target)"
    }

    abstract class Rule : Filter()

    class True : Rule() {
        override fun evaluate(game: Game, context: Context) = true
        override fun equals(other: Any?) = other is True
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
    }

    data class CriticScore(override val score: Double) : TargetScore() {
        override fun extractScore(game: Game, context: Context) = game.criticScore?.score
        override fun toString() = "Critic Score >= $score"
    }

    data class UserScore(override val score: Double) : TargetScore() {
        override fun extractScore(game: Game, context: Context) = game.userScore?.score
        override fun toString() = "User Score >= $score"
    }

    data class AvgScore(override val score: Double) : TargetScore() {
        override fun extractScore(game: Game, context: Context) = game.avgScore
        override fun toString() = "Avg Score >= $score"
    }

    data class MinScore(override val score: Double) : TargetScore() {
        override fun extractScore(game: Game, context: Context) = game.minScore
        override fun toString() = "Min Score >= $score"
    }

    data class MaxScore(override val score: Double) : TargetScore() {
        override fun extractScore(game: Game, context: Context) = game.maxScore
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
    }

    data class TargetReleaseDate(override val date: LocalDate) : TargetDate() {
        override fun extractDate(game: Game, context: Context) = game.releaseDate?.dateTimeOrNull
        override fun toString() = "Release Date >= $date"
    }

    data class TargetCreateDate(override val date: LocalDate) : TargetDate() {
        override fun extractDate(game: Game, context: Context) = game.createDate
        override fun toString() = "Create Date >= $date"
    }

    data class TargetUpdateDate(override val date: LocalDate) : TargetDate() {
        override fun extractDate(game: Game, context: Context) = game.updateDate
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
    }

    data class PeriodReleaseDate(override val period: Period) : PeriodDate() {
        override fun extractDate(game: Game, context: Context) = game.releaseDate?.dateTimeOrNull
        override fun toString() = "Release Date >= (Now - ${period.humanReadable}"
    }

    data class PeriodCreateDate(override val period: Period) : PeriodDate() {
        override fun extractDate(game: Game, context: Context) = game.createDate
        override fun toString() = "Create Date >= (Now - ${period.humanReadable}"
    }

    data class PeriodUpdateDate(override val period: Period) : PeriodDate() {
        override fun extractDate(game: Game, context: Context) = game.updateDate
        override fun toString() = "Update Date >= (Now - ${period.humanReadable}"
    }

    abstract class NullDate : DateRule() {
        override fun evaluate(game: Game, context: Context): Boolean {
            val date = extractDate(game, context)
            return date == null
        }

        override fun equals(other: Any?) = other != null && this::class == other::class
    }

    class NullReleaseDate : NullDate() {
        override fun extractDate(game: Game, context: Context) = game.releaseDate?.dateTimeOrNull
        override fun toString() = "Release Date == NULL"
    }

    data class Platform(val platform: com.gitlab.ykrasik.gamedex.Platform) : Rule() {
        override fun evaluate(game: Game, context: Context) = game.platform == platform
        override fun toString() = "Platform == '$platform'"
    }

    data class Library(val id: LibraryId) : Rule() {
        override fun evaluate(game: Game, context: Context) = game.library.id == id
        override fun toString() = "Library == Library($id)"
    }

    data class Genre(val genre: GenreId) : Rule() {
        override fun evaluate(game: Game, context: Context) = game.genres.any { it.id == genre }
        override fun toString() = "Genre == '$genre'"
    }

    data class Tag(val tag: TagId) : Rule() {
        override fun evaluate(game: Game, context: Context) = game.tags.any { it == tag }
        override fun toString() = "Tag == '$tag'"
    }

    data class FilterTag(val tag: TagId) : Rule() {
        override fun evaluate(game: Game, context: Context) = game.filterTags.any { it == tag }
        override fun toString() = "FilterTag == '$tag'"
    }

    data class Provider(val providerId: ProviderId) : Rule() {
        override fun evaluate(game: Game, context: Context) = eval(game, context) { it.any { it.providerId == providerId } }
        override fun evaluateNot(game: Game, context: Context) = eval(game, context) { it.none { it.providerId == providerId } }

        private inline fun eval(game: Game, context: Context, f: (List<ProviderData>) -> Boolean): Boolean {
            return context.providerSupports(providerId, game.platform) &&
                !game.isProviderExcluded(providerId) &&
                f(game.providerData)
        }

        override fun toString() = "Provider == '$providerId'"
    }

    data class FileSize(val target: com.gitlab.ykrasik.gamedex.util.FileSize) : Rule() {
        override fun evaluate(game: Game, context: Context): Boolean {
            return game.fileTree.value?.let { it.size >= target } ?: false
        }

        override fun toString() = "File Size >= ${target.humanReadable}"
    }

    @JsonIgnoreProperties("regex")
    data class FileName(val value: String) : Rule() {
        private val regex = value.toRegex()

        override fun evaluate(game: Game, context: Context): Boolean {
            return game.fileTree.value?.matches() ?: false
        }

        private fun FileTree.matches(): Boolean = name.matches(regex) || children.any { it.matches() }

        override fun toString() = "File matches /$value/"
    }

    interface Context {
        val now: JodaDateTime
        fun providerSupports(providerId: ProviderId, platform: com.gitlab.ykrasik.gamedex.Platform): Boolean
    }
}

infix fun Filter.and(right: Filter) = Filter.And(listOf(this, right))
infix fun Filter.and(right: () -> Filter) = and(right())
infix fun Filter.or(right: Filter) = Filter.Or(listOf(this, right))
infix fun Filter.or(right: () -> Filter) = or(right())
val Filter.not get() = Filter.Not(this)
val Filter.isEmpty get() = this is Filter.True
fun Filter.find(target: KClass<out Filter>): Filter? {
    fun doFind(current: Filter): Filter? = when {
        current::class == target -> current
        current is Filter.Compound -> current.targets.find { doFind(it) != null }
        current is Filter.Modifier -> doFind(current.target)
        else -> null
    }
    return doFind(this)
}
fun Filter.hasFilter(target: KClass<out Filter>): Boolean = find(target) != null