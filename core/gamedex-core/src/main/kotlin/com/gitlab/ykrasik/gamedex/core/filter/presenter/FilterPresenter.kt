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

package com.gitlab.ykrasik.gamedex.core.filter.presenter

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.app.api.game.GameFilterView
import com.gitlab.ykrasik.gamedex.app.api.game.MenuGameFilterView
import com.gitlab.ykrasik.gamedex.app.api.game.ReportGameFilterView
import com.gitlab.ykrasik.gamedex.app.api.util.ListObservable
import com.gitlab.ykrasik.gamedex.core.CommonData
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import com.gitlab.ykrasik.gamedex.util.FileSize
import com.gitlab.ykrasik.gamedex.util.setAll
import com.gitlab.ykrasik.gamedex.util.toDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 02/06/2018
 * Time: 12:17
 */
// FIXME: This does not live-update possible tags & genres, and possibly libraries
abstract class BaseGameFilterPresenter<V : GameFilterView> constructor(
    private val commonData: CommonData,
    protected val settingsService: SettingsService,
    private val bindPlatform: Boolean
) : Presenter<V> {
    protected abstract val excludedRules: List<KClass<out Filter.Rule>>
    protected abstract val alwaysShowAllRules: Boolean
    protected abstract val libraries: ListObservable<Library>
    protected abstract val genres: ListObservable<String>
    protected abstract val tags: ListObservable<String>

    private val operators = mapOf(
        Filter.And::class to { Filter.And() },
        Filter.Or::class to { Filter.Or() }
    )

    private val rules = mapOf(
        Filter.Platform::class to { Filter.Platform(Platform.pc) },
        Filter.Library::class to {
            // TODO: Consider making the library here optional, as there are cases where this filter is accessed without any libraries available.
            Filter.Library(libraries.first().id)
        },
        Filter.Genre::class to { Filter.Genre(genres.firstOrNull() ?: "") },
        Filter.Tag::class to { Filter.Tag(tags.firstOrNull() ?: "") },
        Filter.ReleaseDate::class to { Filter.ReleaseDate("2014-01-01".toDate()) },
        Filter.Provider::class to { Filter.Provider(commonData.allProviders.first().id) },
        Filter.CriticScore::class to { Filter.CriticScore(60.0) },
        Filter.UserScore::class to { Filter.UserScore(60.0) },
        Filter.AvgScore::class to { Filter.AvgScore(60.0) },
        Filter.FileSize::class to { Filter.FileSize(FileSize(1024L * 1024 * 1024 * 10)) },
        Filter.Duplications::class to { Filter.Duplications() },
        Filter.NameDiff::class to { Filter.NameDiff() }
    ).filterKeys { !excludedRules.contains(it) }

    private val rulesList = rules.keys.toList()

    private fun createRule(klass: KClass<out Filter>): Filter = (rules[klass] ?: operators[klass])!!()

    override fun present(view: V) = object : ViewSession() {
        init {
            genres.bindTo(view.possibleGenres)
            genres.changesChannel.forEach { setPossibleRules() }

            tags.bindTo(view.possibleTags)
            tags.changesChannel.forEach { setPossibleRules() }

            // Providers are a static configuration that can't change during runtime, so no need to listen to changes.
            view.possibleProviderIds.setAll(commonData.allProviders.map { it.id })

            libraries.changesChannel.forEach {
                setPossibleLibraries()
                setPossibleRules()
            }

            if (bindPlatform) {
//            settingsService.game.platformChannel.combineLatest(settingsService.game.platformSettingsChannel).distinctUntilChanged().subscribeOnUi {
                // FIXME: Also reset the state when importing a database (settingsService.game.platformSettingsChannel is changed but not by the view)
                settingsService.game.platformChannel.forEachImmediately {
                    setState(currentPlatformFilter = true)
                }
            } else {
                setState(currentPlatformFilter = false)
            }

            view.wrapInAndActions.forEach { replaceFilter(it, with = Filter.And(it)) }
            view.wrapInOrActions.forEach { replaceFilter(it, with = Filter.Or(it)) }
            view.wrapInNotActions.forEach { replaceFilter(it, with = Filter.Not(it)) }
            view.unwrapNotActions.forEach { replaceFilter(it, with = it.target) }
            view.clearFilterActions.forEach { replaceFilter(view.filter, Filter.`true`) }
            view.updateFilterActions.forEach { (filter, with) -> replaceFilter(filter, with) }
            view.replaceFilterActions.forEach { (filter, with) -> replaceFilter(filter, with) }
            view.deleteFilterActions.forEach { deleteFilter(it) }
        }

        private fun setState(currentPlatformFilter: Boolean) {
            setPossibleLibraries()
            view.filter = if (currentPlatformFilter) settingsService.currentPlatformSettings.filter else Filter.`true`
            setPossibleRules()
        }

        private fun setPossibleLibraries() {
            if (libraries != view.possibleLibraries) {
                view.possibleLibraries.setAll(libraries)
            }
        }

        private fun setPossibleRules() {
            val rules: List<KClass<out Filter.Rule>> = when {
                alwaysShowAllRules -> rulesList
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

        private fun replaceFilter(filter: Filter, with: KClass<out Filter>) {
            val newRule = createRule(with).let { newRule ->
                if (filter is Filter.BinaryOperator && newRule is Filter.BinaryOperator) {
                    newRule.new(filter.left, filter.right)
                } else {
                    newRule
                }
            }
            replaceFilter(filter, newRule)
        }

        private fun replaceFilter(filter: Filter, with: Filter) = modifyRootFilter {
            replace(filter, with)
        }

        private fun deleteFilter(filter: Filter) = modifyRootFilter {
            delete(filter) ?: Filter.`true`
        }

        private fun modifyRootFilter(f: Filter.() -> Filter) {
            view.filter = f(view.filter)
            afterFilterSet(view.filter)
        }
    }

    protected abstract fun afterFilterSet(filter: Filter)
}

@Singleton
class MenuGameFilterPresenter @Inject constructor(commonData: CommonData, settingsService: SettingsService) :
    BaseGameFilterPresenter<MenuGameFilterView>(commonData, settingsService, bindPlatform = true) {
    override val excludedRules get() = listOf(Filter.Platform::class, Filter.Duplications::class, Filter.NameDiff::class)
    override val alwaysShowAllRules = false
    override val libraries = commonData.platformLibraries
    override val genres = commonData.platformGenres
    override val tags = commonData.platformTags

    override fun afterFilterSet(filter: Filter) {
        settingsService.currentPlatformSettings.modify { copy(filter = filter) }
    }
}

@Singleton
class ReportGameFilterPresenter @Inject constructor(commonData: CommonData, settingsService: SettingsService) :
    BaseGameFilterPresenter<ReportGameFilterView>(commonData, settingsService, bindPlatform = false) {
    override val excludedRules get() = emptyList<KClass<out Filter.Rule>>()
    override val alwaysShowAllRules = true
    override val libraries = commonData.realLibraries
    override val genres = commonData.genres
    override val tags = commonData.tags

    override fun afterFilterSet(filter: Filter) {}
}