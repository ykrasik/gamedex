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

package com.gitlab.ykrasik.gamedex.core.filter.presenter

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.app.api.filter.FilterView
import com.gitlab.ykrasik.gamedex.core.CommonData
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import com.gitlab.ykrasik.gamedex.core.util.mapping
import com.gitlab.ykrasik.gamedex.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

/**
 * User: ykrasik
 * Date: 02/06/2018
 * Time: 12:17
 */
// FIXME: This does not live-update possible tags & genres, and possibly libraries
@Singleton
class FilterPresenter @Inject constructor(
    private val commonData: CommonData,
    private val settingsService: SettingsService
) : Presenter<FilterView> {
    override fun present(view: FilterView) = object : ViewSession() {
        private val excludedRules =
            if (view.onlyShowConditionsForCurrentPlatform) listOf(Filter.Platform::class, Filter.Duplications::class, Filter.NameDiff::class)
            else emptyList()

        private val libraries = if (view.onlyShowConditionsForCurrentPlatform) commonData.platformLibraries else commonData.realLibraries
        private val genres = if (view.onlyShowConditionsForCurrentPlatform) commonData.platformGenres else commonData.genres
        private val tags = if (view.onlyShowConditionsForCurrentPlatform) commonData.platformTags else commonData.tags
        private val providers = if (view.onlyShowConditionsForCurrentPlatform) commonData.platformProviders else commonData.allProviders

        private val operators = listOf(
            ConditionBuilder.singleParam<Filter.Not, Filter>(Filter::Not) { Filter.True() },
            ConditionBuilder.twoParams<Filter.And, Filter, Filter>(Filter::And, { Filter.True() }, { Filter.True() }),
            ConditionBuilder.twoParams<Filter.Or, Filter, Filter>(Filter::Or, { Filter.True() }, { Filter.True() })
        ).associateBy { it.klass }.toMap()

        private val rules = listOf(
            ConditionBuilder.singleParam(Filter::Platform) { Platform.pc },
            ConditionBuilder.singleParam(Filter::Library) { libraries.first().id },
            ConditionBuilder.singleParam(Filter::Genre) { genres.firstOrNull() ?: "" },
            ConditionBuilder.singleParam(Filter::Tag) { tags.firstOrNull() ?: "" },
            ConditionBuilder.singleParam(Filter::Provider) { commonData.allProviders.first().id },
            ConditionBuilder.singleParam(Filter::CriticScore) { 60.0 },
            ConditionBuilder.noParams(Filter::NullCriticScore),
            ConditionBuilder.singleParam(Filter::UserScore) { 60.0 },
            ConditionBuilder.noParams(Filter::NullUserScore),
            ConditionBuilder.singleParam(Filter::AvgScore) { 60.0 },
            ConditionBuilder.singleParam(Filter::MinScore) { 60.0 },
            ConditionBuilder.singleParam(Filter::MaxScore) { 60.0 },
            ConditionBuilder.singleParam(Filter::TargetReleaseDate) { "2014-01-01".toDate() },
            ConditionBuilder.singleParam(Filter::PeriodReleaseDate) { 1.years },
            ConditionBuilder.noParams(Filter::NullReleaseDate),
            ConditionBuilder.singleParam(Filter::TargetCreateDate) { "2014-01-01".toDate() },
            ConditionBuilder.singleParam(Filter::PeriodCreateDate) { 2.months },
            ConditionBuilder.singleParam(Filter::TargetUpdateDate) { "2014-01-01".toDate() },
            ConditionBuilder.singleParam(Filter::PeriodUpdateDate) { 2.months },
            ConditionBuilder.singleParam(Filter::FileSize) { FileSize(10.gb) },
            ConditionBuilder.noParams(Filter::Duplications),
            ConditionBuilder.noParams(Filter::NameDiff)
        ).associateBy { it.klass }.toMap().filterKeys { !excludedRules.contains(it) }

        private val rulesList = rules.keys.toList()

        init {
            genres.bind(view.possibleGenres)
            genres.changesChannel.forEach { setPossibleRules() }

            tags.bind(view.possibleTags)
            tags.changesChannel.forEach { setPossibleRules() }

            providers.mapping { it.id }.bind(view.possibleProviderIds)
            providers.changesChannel.forEach { setPossibleRules() }

            setState()
            libraries.changesChannel.forEach { setState() }

            if (view.onlyShowConditionsForCurrentPlatform) {
//            settingsService.game.platformChannel.combineLatest(settingsService.game.platformSettingsChannel).distinctUntilChanged().subscribeOnUi {
                // FIXME: Also reset the state when importing a database (settingsService.game.platformSettingsChannel is changed but not by the view)
                settingsService.game.platformChannel.forEach { setState() }
            }

            view.setFilterActions.forEach { setFilter(it) }
            view.wrapInAndActions.forEach { replaceFilter(it, with = Filter.And(it)) }
            view.wrapInOrActions.forEach { replaceFilter(it, with = Filter.Or(it)) }
            view.wrapInNotActions.forEach { replaceFilter(it, with = Filter.Not(it)) }
            view.unwrapNotActions.forEach { replaceFilter(it, with = it.target) }
            view.clearFilterActions.forEach { replaceFilter(view.filter.value, Filter.Null) }
            view.updateFilterActions.forEach { (filter, with) -> replaceFilter(filter, with) }
            view.replaceFilterActions.forEach { (filter, with) -> replaceFilter(filter, with) }
            view.deleteFilterActions.forEach { deleteFilter(it) }
        }

        private fun setState() {
            setPossibleLibraries()
            setPossibleRules()
        }

        private fun setPossibleLibraries() {
            if (libraries != view.possibleLibraries) {
                view.possibleLibraries.setAll(libraries)
            }
        }

        private fun setPossibleRules() {
            val rules: List<KClass<out Filter.Rule>> = when {
                !view.onlyShowConditionsForCurrentPlatform -> rulesList
                commonData.games.size <= 1 -> emptyList()
                else -> {
                    val rules = rulesList.toMutableList()
                    if (view.possibleGenres.size <= 1) {
                        rules -= Filter.Genre::class
                    }
                    if (view.possibleTags.size < 1) {
                        rules -= Filter.Tag::class
                    }
                    if (view.possibleLibraries.size <= 1) {
                        rules -= Filter.Library::class
                    }
                    if (view.possibleProviderIds.size <= 1) {
                        rules -= Filter.Provider::class
                    }
                    rules
                }
            }

            if (rules != view.possibleRules) {
                view.possibleRules.setAll(rules)
            }
        }

        private fun setFilter(filter: Filter) = replaceFilter(view.filter.value, filter)

        private fun replaceFilter(filter: Filter, with: KClass<out Filter>) {
            val conditionBuilder = when {
                filter is Filter.BinaryOperator && with.superclasses.first() == Filter.BinaryOperator::class ->
                    @Suppress("UNCHECKED_CAST")
                    newBinaryOperator(from = filter, to = with as KClass<out Filter.BinaryOperator>) { it }
                filter is Filter.TargetScore && with.superclasses.first() == Filter.TargetScore::class ->
                    rules[with]!!.withParams(filter.score)
                filter is Filter.TargetDate && with.superclasses.first() == Filter.TargetDate::class ->
                    rules[with]!!.withParams(filter.date)
                filter is Filter.PeriodDate && with.superclasses.first() == Filter.PeriodDate::class ->
                    rules[with]!!.withParams(filter.period)
                else ->
                    rules[with]!!
            }
            val newRule = conditionBuilder()
            replaceFilter(filter, newRule)
        }

        private fun replaceFilter(filter: Filter, with: Filter) = modifyFilter { replace(filter, with) }

        private fun deleteFilter(filter: Filter) = modifyFilter { delete(filter) ?: Filter.Null }

        private inline fun modifyFilter(f: Modifier<Filter>) {
            view.filter.modify(f)
            setIsValid()
        }

        private fun setIsValid() {
            view.filterIsValid *= Try {
                check(view.filter.value is Filter.True || view.filter.value.find(Filter.True::class) == null) { "Please select a condition!" }
            }
        }

        private fun Filter.replace(target: Filter, with: Filter): Filter {
            fun doReplace(current: Filter): Filter = when {
                current === target -> with
                current is Filter.BinaryOperator -> newBinaryOperator(current, transform = ::doReplace)()
                current is Filter.UnaryOperator -> newUnaryOperator(current, transform = ::doReplace)()
                else -> current
            }
            return doReplace(this)
        }

        private fun Filter.delete(target: Filter): Filter? {
            fun doDelete(current: Filter): Filter? = when {
                current === target -> null
                current is Filter.BinaryOperator -> {
                    val newLeft = doDelete(current.left)
                    val newRight = doDelete(current.right)
                    when {
                        newLeft != null && newRight != null -> newBinaryOperator(current).withParams(newLeft, newRight)()
                        newLeft != null -> newLeft
                        else -> newRight
                    }
                }
                current is Filter.UnaryOperator -> {
                    val newRule = doDelete(current.target)
                    if (newRule != null) newUnaryOperator(current).withParams(newRule)() else null
                }
                else -> current
            }
            return doDelete(this)
        }

        private fun Filter.find(target: KClass<out Filter>): Filter? {
            fun doFind(current: Filter): Filter? = when {
                current::class == target -> current
                current is Filter.BinaryOperator -> doFind(current.left) ?: doFind(current.right)
                current is Filter.UnaryOperator -> doFind(current.target)
                else -> null
            }
            return doFind(this)
        }

        private fun newBinaryOperator(
            from: Filter.BinaryOperator,
            to: KClass<out Filter.BinaryOperator> = from::class,
            transform: ((Filter) -> Filter)? = null
        ) = operators[to]!!.let { new ->
            if (transform == null) {
                new
            } else {
                new.withParams(transform(from.left), transform(from.right))
            }
        }

        private fun newUnaryOperator(
            from: Filter.UnaryOperator,
            to: KClass<out Filter.UnaryOperator> = from::class,
            transform: ((Filter) -> Filter)? = null
        ) = operators[to]!!.let { new ->
            if (transform == null) {
                new
            } else {
                new.withParams(transform(from.target))
            }
        }
    }

    private data class ConditionBuilder<T : Filter>(
        val klass: KClass<T>,
        private val param1: Any? = null,
        private val param2: Any? = null,
        private val build: (Any?, Any?) -> T
    ) {
        fun withParams(param1: Any? = null, param2: Any? = null) =
            copy(param1 = param1, param2 = param2)

        operator fun invoke(): T = this.build(param1, param2)

        companion object {
            inline operator fun <reified T : Filter> invoke(crossinline build: (Any?, Any?) -> T): ConditionBuilder<T> =
                ConditionBuilder(T::class) { param1, param2 -> build(param1, param2) }

            inline fun <reified T : Filter> noParams(crossinline factory: () -> T): ConditionBuilder<T> =
                ConditionBuilder { _, _ -> factory() }

            inline fun <reified T : Filter, reified A> singleParam(crossinline factory: (A) -> T, crossinline default: () -> A): ConditionBuilder<T> =
                ConditionBuilder { param, _ -> factory(param as? A ?: default()) }

            inline fun <reified T : Filter, reified A, reified B> twoParams(
                crossinline factory: (A, B) -> T,
                crossinline defaultA: () -> A,
                crossinline defaultB: () -> B
            ): ConditionBuilder<T> = ConditionBuilder { param1, param2 ->
                factory(param1 as? A ?: defaultA(), param2 as? B ?: defaultB())
            }
        }
    }
}