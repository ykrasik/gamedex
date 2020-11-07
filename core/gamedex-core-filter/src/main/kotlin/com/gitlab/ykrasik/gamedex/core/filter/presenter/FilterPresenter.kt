/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.app.api.filter.find
import com.gitlab.ykrasik.gamedex.app.api.filter.isEmpty
import com.gitlab.ykrasik.gamedex.app.api.util.ValidatedValue
import com.gitlab.ykrasik.gamedex.core.CommonData
import com.gitlab.ykrasik.gamedex.core.view.Presenter
import com.gitlab.ykrasik.gamedex.core.view.ViewSession
import com.gitlab.ykrasik.gamedex.util.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

/**
 * User: ykrasik
 * Date: 02/06/2018
 * Time: 12:17
 */
@Singleton
class FilterPresenter @Inject constructor(
    private val commonData: CommonData,
) : Presenter<FilterView> {
    override fun present(view: FilterView) = object : ViewSession() {
        private val excludedRules = listOf(Filter.Platform::class)

        private val libraries = commonData.currentPlatformLibraries
        private val genres = commonData.currentPlatformGenres
        private val tags = commonData.currentPlatformTags
        private val filterTags = commonData.currentPlatformFilterTags
        private val providers = commonData.currentPlatformProviders

        private val metaFilters = listOf(
            FilterBuilder.param<Filter.Not, Filter>(Filter::Not) { Filter.True() },
            FilterBuilder.param<Filter.And, List<Filter>>(Filter::And) { listOf(Filter.True()) },
            FilterBuilder.param<Filter.Or, List<Filter>>(Filter::Or) { listOf(Filter.True()) }
        ).associateBy { it.klass }.toMap()

        private val filterBuilders = listOf(
            FilterBuilder.param(Filter::Platform) { Platform.Windows },
            FilterBuilder.param(Filter::Library) { libraries.value.first().id },
            FilterBuilder.param(Filter::Genre) { genres.value.firstOrNull()?.id ?: "" },
            FilterBuilder.param(Filter::Tag) { tags.value.firstOrNull() ?: "" },
            FilterBuilder.param(Filter::FilterTag) { filterTags.value.firstOrNull() ?: "" },
            FilterBuilder.param(Filter::Provider) { providers.value.firstOrNull()?.id ?: "" },
            FilterBuilder.param(Filter::CriticScore) { 60.0 },
            FilterBuilder.param(Filter::UserScore) { 60.0 },
            FilterBuilder.param(Filter::AvgScore) { 60.0 },
            FilterBuilder.param(Filter::MinScore) { 60.0 },
            FilterBuilder.param(Filter::MaxScore) { 60.0 },
            FilterBuilder.param(Filter::TargetReleaseDate) { today - 3.years },
            FilterBuilder.param(Filter::PeriodReleaseDate) { 3.years },
            FilterBuilder.noParams(Filter::NullReleaseDate),
            FilterBuilder.param(Filter::TargetCreateDate) { today - 2.months },
            FilterBuilder.param(Filter::PeriodCreateDate) { 2.months },
            FilterBuilder.param(Filter::TargetUpdateDate) { today - 2.months },
            FilterBuilder.param(Filter::PeriodUpdateDate) { 2.months },
            FilterBuilder.param(Filter::FileName) { ".*" },
            FilterBuilder.param(Filter::FileSize) { FileSize(1.gb) }
        ).associateBy { it.klass }.toMap().filterKeys { !excludedRules.contains(it) }

        private val filters = filterBuilders.keys.toList()

        init {
            view::filterValidatedValue *= view.filter.allValues().map { filter ->
                ValidatedValue(filter, IsValid {
                    check(filter.isEmpty || filter.find(Filter.True::class) == null) { "Select a filter!" }
                })
            }

            view::availableLibraries *= libraries
            view::availableProviderIds *= providers.map { it.map { it.id } }
            view::availableGenres *= genres
            view::availableTags *= tags
            view::availableFilterTags *= filterTags
            view::availableFilters *= combine(libraries, providers, genres, tags, filterTags) { libraries, providers, genres, tags, filterTags ->
                when {
                    commonData.games.value.size <= 1 -> emptyList<KClass<out Filter.Rule>>()
                    else -> {
                        val filters = this.filters.toMutableList()
                        if (libraries.size <= 1) {
                            filters -= Filter.Library::class
                        }
                        if (genres.size <= 1) {
                            filters -= Filter.Genre::class
                        }
                        if (tags.isEmpty()) {
                            filters -= Filter.Tag::class
                        }
                        if (filterTags.isEmpty()) {
                            filters -= Filter.FilterTag::class
                        }
                        if (providers.size <= 1) {
                            filters -= Filter.Provider::class
                        }
                        filters
                    }
                }
            }

            view::wrapInAndActions.forEach { replaceFilter(it, with = Filter.And(listOf(it, Filter.True()))) }
            view::wrapInOrActions.forEach { replaceFilter(it, with = Filter.Or(listOf(it, Filter.True()))) }
            view::wrapInNotActions.forEach { replaceFilter(it, with = Filter.Not(it)) }
            view::unwrapNotActions.forEach { replaceFilter(it, with = it.target) }
            view::updateFilterActions.forEach { (filter, with) -> replaceFilter(filter, with) }
            view::replaceFilterActions.forEach { (filter, with) -> replaceFilter(filter, with) }
            view::deleteFilterActions.forEach { deleteFilter(it) }
        }

        private fun replaceFilter(filter: Filter, with: KClass<out Filter>) {
            if (filter::class == with) return

            val filterBuilder = when {
                filter is Filter.Compound && with.superclasses.first() == Filter.Compound::class ->
                    @Suppress("UNCHECKED_CAST")
                    newCompoundFilter(from = filter, to = with as KClass<out Filter.Compound>) { it }
                filter is Filter.TargetScore && with.superclasses.first() == Filter.TargetScore::class ->
                    filterBuilders[with]!!.withParams(filter.score)
                filter is Filter.TargetDate && with.superclasses.first() == Filter.TargetDate::class ->
                    filterBuilders[with]!!.withParams(filter.date)
                filter is Filter.PeriodDate && with.superclasses.first() == Filter.PeriodDate::class ->
                    filterBuilders[with]!!.withParams(filter.period)
                else ->
                    filterBuilders[with]!!
            }
            val newFilter = filterBuilder()
            replaceFilter(filter, newFilter)
        }

        private fun replaceFilter(filter: Filter, with: Filter) {
            view.filter /= view.filter.v.replace(filter, with)
        }

        private fun deleteFilter(filter: Filter) {
            view.filter /= view.filter.v.delete(filter) ?: Filter.Null
        }

        private fun Filter.replace(target: Filter, with: Filter): Filter {
            fun doReplace(current: Filter): Filter = when {
                current === target -> with
                current is Filter.Compound -> {
                    val newTargets = current.targets.map { doReplace(it) }
                    if (newTargets == current.targets) {
                        current
                    } else {
                        metaFilters[current::class]!!.withParams(newTargets)()
                    }
                }
                current is Filter.Modifier -> {
                    val newTarget = doReplace(current.target)
                    if (newTarget === current.target) {
                        current
                    } else {
                        metaFilters[current::class]!!.withParams(newTarget)()
                    }
                }
                else -> current
            }
            return doReplace(this).flatten()
        }

        private fun Filter.delete(target: Filter): Filter? {
            fun doDelete(current: Filter): Filter? = when {
                current === target -> null
                current is Filter.Compound -> {
                    val newTargets = current.targets.mapNotNull(::doDelete)
                    if (newTargets.size > 1) {
                        newCompoundFilter(current).withParams(newTargets)()
                    } else {
                        newTargets.firstOrNull()
                    }
                }
                current is Filter.Modifier -> {
                    val newRule = doDelete(current.target)
                    if (newRule != null) newModifierFilter(current).withParams(newRule)() else null
                }
                else -> current
            }
            return doDelete(this)?.flatten()
        }

        private fun Filter.flatten(): Filter = when (this) {
            is Filter.Compound -> {
                val newTargets = targets.flatMap {
                    val result = it.flatten()
                    if (result is Filter.Compound && result::class == this::class) {
                        result.targets
                    } else {
                        listOf(result)
                    }
                }
                newCompoundFilter(this).withParams(newTargets)()
            }
            is Filter.Modifier -> {
                newModifierFilter(this).withParams(target.flatten())()
            }
            else -> this
        }.let { newFilter ->
            if (this == newFilter) {
                this
            } else {
                newFilter
            }
        }

        private fun newCompoundFilter(
            from: Filter.Compound,
            to: KClass<out Filter.Compound> = from::class,
            transform: ((Filter) -> Filter)? = null,
        ) = metaFilters.getValue(to).let { new ->
            if (transform == null) {
                new
            } else {
                new.withParams(from.targets.map(transform))
            }
        }

        private fun newModifierFilter(
            from: Filter.Modifier,
            to: KClass<out Filter.Modifier> = from::class,
            transform: ((Filter) -> Filter)? = null,
        ) = metaFilters.getValue(to).let { new ->
            if (transform == null) {
                new
            } else {
                new.withParams(transform(from.target))
            }
        }
    }

    private data class FilterBuilder<T : Filter>(
        val klass: KClass<T>,
        private val param1: Any? = null,
        private val param2: Any? = null,
        private val build: (Any?, Any?) -> T,
    ) {
        fun withParams(param1: Any? = null, param2: Any? = null) =
            copy(param1 = param1, param2 = param2)

        operator fun invoke(): T = this.build(param1, param2)

        companion object {
            inline operator fun <reified T : Filter> invoke(crossinline build: (Any?, Any?) -> T): FilterBuilder<T> =
                FilterBuilder(T::class) { param1, param2 -> build(param1, param2) }

            inline fun <reified T : Filter> noParams(crossinline factory: () -> T): FilterBuilder<T> =
                FilterBuilder { _, _ -> factory() }

            inline fun <reified T : Filter, reified A> param(crossinline factory: (A) -> T, crossinline default: () -> A): FilterBuilder<T> =
                FilterBuilder { param, _ -> factory(param as? A ?: default()) }

            inline fun <reified T : Filter, reified A, reified B> twoParams(
                crossinline factory: (A, B) -> T,
                crossinline defaultA: () -> A,
                crossinline defaultB: () -> B,
            ): FilterBuilder<T> = FilterBuilder { param1, param2 ->
                factory(param1 as? A ?: defaultA(), param2 as? B ?: defaultB())
            }
        }
    }
}