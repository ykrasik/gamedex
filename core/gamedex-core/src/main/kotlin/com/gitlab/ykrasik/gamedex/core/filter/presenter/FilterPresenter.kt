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
import com.gitlab.ykrasik.gamedex.provider.id
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
            if (view.onlyShowFiltersForCurrentPlatform) listOf(Filter.Platform::class, Filter.Duplications::class, Filter.NameDiff::class)
            else emptyList()

        private val libraries = if (view.onlyShowFiltersForCurrentPlatform) commonData.platformLibraries else commonData.contentLibraries
        private val genres = if (view.onlyShowFiltersForCurrentPlatform) commonData.platformGenres else commonData.genres
        private val tags = if (view.onlyShowFiltersForCurrentPlatform) commonData.platformTags else commonData.tags
        private val providers = if (view.onlyShowFiltersForCurrentPlatform) commonData.platformProviders else commonData.allProviders

        private val metaFilters = listOf(
            FilterBuilder.param<Filter.Not, Filter>(Filter::Not) { Filter.True() },
            FilterBuilder.param<Filter.And, List<Filter>>(Filter::And) { listOf(Filter.True()) },
            FilterBuilder.param<Filter.Or, List<Filter>>(Filter::Or) { listOf(Filter.True()) }
        ).associateBy { it.klass }.toMap()

        private val filters = listOf(
            FilterBuilder.param(Filter::Platform) { Platform.Windows },
            FilterBuilder.param(Filter::Library) { libraries.first().id },
            FilterBuilder.param(Filter::Genre) { genres.firstOrNull() ?: "" },
            FilterBuilder.param(Filter::Tag) { tags.firstOrNull() ?: "" },
            FilterBuilder.param(Filter::Provider) { commonData.allProviders.first().id },
            FilterBuilder.param(Filter::CriticScore) { 60.0 },
            FilterBuilder.param(Filter::UserScore) { 60.0 },
            FilterBuilder.param(Filter::AvgScore) { 60.0 },
            FilterBuilder.param(Filter::MinScore) { 60.0 },
            FilterBuilder.param(Filter::MaxScore) { 60.0 },
            FilterBuilder.param(Filter::TargetReleaseDate) { "2014-01-01".date },
            FilterBuilder.param(Filter::PeriodReleaseDate) { 1.years },
            FilterBuilder.noParams(Filter::NullReleaseDate),
            FilterBuilder.param(Filter::TargetCreateDate) { "2014-01-01".date },
            FilterBuilder.param(Filter::PeriodCreateDate) { 2.months },
            FilterBuilder.param(Filter::TargetUpdateDate) { "2014-01-01".date },
            FilterBuilder.param(Filter::PeriodUpdateDate) { 2.months },
            FilterBuilder.param(Filter::FileName) { ".*" },
            FilterBuilder.param(Filter::FileSize) { FileSize(1.gb) },
            FilterBuilder.noParams(Filter::Duplications),
            FilterBuilder.noParams(Filter::NameDiff)
        ).associateBy { it.klass }.toMap().filterKeys { !excludedRules.contains(it) }

        private val filterList = filters.keys.toList()

        init {
            genres.bind(view.possibleGenres)
            genres.changesChannel.forEach { setPossibleRules() }

            tags.bind(view.possibleTags)
            tags.changesChannel.forEach { setPossibleRules() }

            providers.mapping { it.id }.bind(view.possibleProviderIds)
            providers.changesChannel.forEach { setPossibleRules() }

            setState()
            libraries.changesChannel.forEach { setState() }

            if (view.onlyShowFiltersForCurrentPlatform) {
//            settingsService.game.platformChannel.combineLatest(settingsService.game.platformSettingsChannel).distinctUntilChanged().subscribeOnUi {
                // FIXME: Also reset the state when importing a database (settingsService.game.platformSettingsChannel is changed but not by the view)
                settingsService.currentPlatformSettingsChannel.forEach { setState() }
            }

            view.setFilterActions.forEach { setFilter(it) }
            view.wrapInAndActions.forEach { replaceFilter(it, with = Filter.And(listOf(it, Filter.True()))) }
            view.wrapInOrActions.forEach { replaceFilter(it, with = Filter.Or(listOf(it, Filter.True()))) }
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
            val filters: List<KClass<out Filter.Rule>> = when {
                !view.onlyShowFiltersForCurrentPlatform -> filterList
                commonData.games.size <= 1 -> emptyList()
                else -> {
                    val filters = filterList.toMutableList()
                    if (view.possibleGenres.size <= 1) {
                        filters -= Filter.Genre::class
                    }
                    if (view.possibleTags.size < 1) {
                        filters -= Filter.Tag::class
                    }
                    if (view.possibleLibraries.size <= 1) {
                        filters -= Filter.Library::class
                    }
                    if (view.possibleProviderIds.size <= 1) {
                        filters -= Filter.Provider::class
                    }
                    filters
                }
            }

            if (filters != view.possibleFilters) {
                view.possibleFilters.setAll(filters)
            }
        }

        private fun setFilter(filter: Filter) = replaceFilter(view.filter.value, filter)

        private fun replaceFilter(filter: Filter, with: KClass<out Filter>) {
            val filterBuilder = when {
                filter is Filter.Compound && with.superclasses.first() == Filter.Compound::class ->
                    @Suppress("UNCHECKED_CAST")
                    newCompoundFilter(from = filter, to = with as KClass<out Filter.Compound>) { it }
                filter is Filter.TargetScore && with.superclasses.first() == Filter.TargetScore::class ->
                    filters[with]!!.withParams(filter.score)
                filter is Filter.TargetDate && with.superclasses.first() == Filter.TargetDate::class ->
                    filters[with]!!.withParams(filter.date)
                filter is Filter.PeriodDate && with.superclasses.first() == Filter.PeriodDate::class ->
                    filters[with]!!.withParams(filter.period)
                else ->
                    filters[with]!!
            }
            val newFilter = filterBuilder()
            replaceFilter(filter, newFilter)
        }

        private fun replaceFilter(filter: Filter, with: Filter) = modifyFilter { replace(filter, with) }

        private fun deleteFilter(filter: Filter) = modifyFilter { delete(filter) ?: Filter.Null }

        private inline fun modifyFilter(f: Modifier<Filter>) {
            view.filter.modify(f)
            setIsValid()
        }

        private fun setIsValid() {
            view.filterIsValid *= Try {
                check(view.filter.value is Filter.True || view.filter.value.find(Filter.True::class) == null) { "Please select a filter!" }
            }
        }

        private fun Filter.replace(target: Filter, with: Filter): Filter {
            fun doReplace(current: Filter): Filter = when {
                current === target -> with
                current is Filter.Compound -> newCompoundFilter(current, transform = ::doReplace)()
                current is Filter.Modifier -> newModifierFilter(current, transform = ::doReplace)()
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

        private fun Filter.find(target: KClass<out Filter>): Filter? {
            fun doFind(current: Filter): Filter? = when {
                current::class == target -> current
                current is Filter.Compound -> current.targets.find { doFind(it) != null }
                current is Filter.Modifier -> doFind(current.target)
                else -> null
            }
            return doFind(this)
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
        }

        private fun newCompoundFilter(
            from: Filter.Compound,
            to: KClass<out Filter.Compound> = from::class,
            transform: ((Filter) -> Filter)? = null
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
            transform: ((Filter) -> Filter)? = null
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
        private val build: (Any?, Any?) -> T
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
                crossinline defaultB: () -> B
            ): FilterBuilder<T> = FilterBuilder { param1, param2 ->
                factory(param1 as? A ?: defaultA(), param2 as? B ?: defaultB())
            }
        }
    }
}