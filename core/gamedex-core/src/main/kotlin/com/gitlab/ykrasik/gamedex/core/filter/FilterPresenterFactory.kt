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

package com.gitlab.ykrasik.gamedex.core.filter

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.app.api.game.GameFilterView
import com.gitlab.ykrasik.gamedex.app.api.game.MenuGameFilterView
import com.gitlab.ykrasik.gamedex.app.api.game.ReportGameFilterView
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.PresenterFactory
import com.gitlab.ykrasik.gamedex.core.api.game.GameService
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.api.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import com.gitlab.ykrasik.gamedex.util.FileSize
import com.gitlab.ykrasik.gamedex.util.toDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 02/06/2018
 * Time: 12:17
 */
abstract class BaseGameFilterPresenterFactory<V : GameFilterView> constructor(
    private val gameService: GameService,
    private val libraryService: LibraryService,
    private val gameProviderService: GameProviderService,
    protected val settingsService: SettingsService
) : PresenterFactory<V> {
    protected abstract val excludedRules: List<KClass<out Filter.Rule>>
    protected abstract val alwaysShowAllRules: Boolean

    private val operators = mapOf(
        Filter.And::class to { Filter.And() },
        Filter.Or::class to { Filter.Or() }
    )

    private val rules = mapOf(
        Filter.Platform::class to { Filter.Platform(Platform.pc) },
        Filter.Library::class to {
            Filter.Library(libraryService.realLibraries.find { it.platform == settingsService.game.platform }!!.id)
        },
        Filter.Genre::class to { Filter.Genre(gameService.genres.firstOrNull() ?: "") },
        Filter.Tag::class to { Filter.Tag(gameService.tags.firstOrNull() ?: "") },
        Filter.ReleaseDate::class to { Filter.ReleaseDate("2014-01-01".toDate()) },
        Filter.Provider::class to { Filter.Provider(gameProviderService.allProviders.first().id) },
        Filter.CriticScore::class to { Filter.CriticScore(60.0) },
        Filter.UserScore::class to { Filter.UserScore(60.0) },
        Filter.AvgScore::class to { Filter.AvgScore(60.0) },
        Filter.FileSize::class to { Filter.FileSize(FileSize(1024L * 1024 * 1024 * 10)) },
        Filter.Duplications::class to { Filter.Duplications() },
        Filter.NameDiff::class to { Filter.NameDiff() }
    ).filterKeys { !excludedRules.contains(it) }

    private val rulesList = rules.keys.toList()

    private fun createRule(klass: KClass<out Filter>): Filter = (rules[klass] ?: operators[klass])!!()

    override fun present(view: V) = object : Presenter() {
        init {
            gameService.genres.bindTo(view.possibleGenres)
            gameService.genres.changesChannel.subscribeOnUi { setPossibleRules() }

            gameService.tags.bindTo(view.possibleTags)
            gameService.tags.changesChannel.subscribeOnUi { setPossibleRules() }

            view.possibleProviderIds.clear()
            view.possibleProviderIds += gameProviderService.allProviders.map { it.id }
            // providers are a static configuration that can't change during runtime, so no need to set rules.

            setPossibleLibraries()
            view.filter = settingsService.game.currentPlatformSettings.filter
            setPossibleRules()

            settingsService.game.platformChannel.subscribeOnUi {
                setPossibleLibraries()
                view.filter = settingsService.game.currentPlatformSettings.filter
                setPossibleRules()
            }

            libraryService.realLibraries.changesChannel.subscribeOnUi {
                setPossibleLibraries()
                setPossibleRules()
            }

            view.wrapInAndActions.actionOnUi { replaceFilter(it, with = Filter.And(it)) }
            view.wrapInOrActions.actionOnUi { replaceFilter(it, with = Filter.Or(it)) }
            view.wrapInNotActions.actionOnUi { replaceFilter(it, with = Filter.Not(it)) }
            view.unwrapNotActions.actionOnUi { replaceFilter(it, with = it.target) }
            view.clearFilterActions.actionOnUi { replaceFilter(view.filter, Filter.`true`) }
            view.updateFilterActions.actionOnUi { (filter, with) -> replaceFilter(filter, with) }
            view.replaceFilterActions.actionOnUi { (filter, with) -> replaceFilter(filter, with) }
            view.deleteFilterActions.actionOnUi { deleteFilter(it) }
        }

        private fun setPossibleLibraries() {
            val libraries = libraryService.realLibraries.filter { filterLibrary(it, settingsService.game.platform) }
            if (libraries != view.possibleLibraries) {
                view.possibleLibraries.clear()
                view.possibleLibraries += libraries
            }
        }

        private fun setPossibleRules() {
            val rules: List<KClass<out Filter.Rule>> = when {
                alwaysShowAllRules -> rulesList
                gameService.games.size <= 1 -> emptyList()
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
                view.possibleRules.clear()
                view.possibleRules += rules
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

    protected abstract fun filterLibrary(library: Library, currentPlatform: Platform): Boolean

    protected abstract fun afterFilterSet(filter: Filter)
}

@Singleton
class MenuGameFilterPresenterFactory @Inject constructor(
    gameService: GameService,
    libraryService: LibraryService,
    gameProviderService: GameProviderService,
    settingsService: SettingsService
) : BaseGameFilterPresenterFactory<MenuGameFilterView>(gameService, libraryService, gameProviderService, settingsService) {
    override val excludedRules get() = listOf(Filter.Platform::class, Filter.Duplications::class, Filter.NameDiff::class)
    override val alwaysShowAllRules = false

    override fun filterLibrary(library: Library, currentPlatform: Platform) = library.platform == currentPlatform

    override fun afterFilterSet(filter: Filter) {
        settingsService.game.modifyCurrentPlatformSettings { copy(filter = filter) }
    }
}

@Singleton
class ReportGameFilterPresenterFactory @Inject constructor(
    gameService: GameService,
    libraryService: LibraryService,
    gameProviderService: GameProviderService,
    settingsService: SettingsService
) : BaseGameFilterPresenterFactory<ReportGameFilterView>(gameService, libraryService, gameProviderService, settingsService) {
    override val excludedRules get() = emptyList<KClass<out Filter.Rule>>()
    override val alwaysShowAllRules = true

    override fun filterLibrary(library: Library, currentPlatform: Platform) = true

    override fun afterFilterSet(filter: Filter) {}
}