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

package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.core.api.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.api.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.core.game.Filter
import com.gitlab.ykrasik.gamedex.core.game.Filter.Companion.filterClass
import com.gitlab.ykrasik.gamedex.core.game.Filter.Companion.name
import com.gitlab.ykrasik.gamedex.core.game.GameUserConfig
import com.gitlab.ykrasik.gamedex.javafx.game.GameController
import com.gitlab.ykrasik.gamedex.util.FileSize
import com.gitlab.ykrasik.gamedex.util.toDate
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 25/01/2018
 * Time: 10:04
 */
// TODO: Wrap this class in a factory. And Rename to FilterRepository.
class FilterSet private constructor(
    private val _rules: Map<KClass<out Filter.Rule>, () -> Filter.Rule>,
    private val _operators: Map<KClass<out Filter.BinaryOperator>, () -> Filter.BinaryOperator>
) {
    val rules = _rules.keys.toList().map { it.name }
    val operators = _operators.keys.toList().map { it.name }

    fun new(klass: KClass<out Filter>): Filter = (_rules[klass] ?: _operators[klass])!!()
    fun new(name: String): Filter = new(name.filterClass)

    class Builder(
        private val gameUserConfig: GameUserConfig,
        private val libraryService: LibraryService,
        private val gameController: GameController,
        private val gameProviderService: GameProviderService
    ) {
        private val operators = mapOf(
            Filter.And::class to { Filter.And() },
            Filter.Or::class to { Filter.Or() }
        )

        // TODO: Only show any of these when there's values to show for filter, always show for report.
        private val rules = mutableMapOf(
            Filter.Platform::class to { Filter.Platform(gameUserConfig.platform) },
            Filter.Library::class to {
                // TODO: Use platformLibraries for filter, realLibraries for report.
                Filter.Library(gameUserConfig.platform, libraryService.realLibraries.firstOrNull()?.name ?: "")
            },
            Filter.Genre::class to { Filter.Genre(gameController.genres.firstOrNull() ?: "") },
            Filter.Tag::class to { Filter.Tag(gameController.tags.firstOrNull() ?: "") },
            Filter.ReleaseDate::class to { Filter.ReleaseDate("2014-01-01".toDate()) },
            Filter.Provider::class to { Filter.Provider(gameProviderService.allProviders.first().id) },
            Filter.CriticScore::class to { Filter.CriticScore(60.0) },
            Filter.UserScore::class to { Filter.UserScore(60.0) },
            Filter.AvgScore::class to { Filter.AvgScore(60.0) },
            Filter.FileSize::class to { Filter.FileSize(FileSize(1024L * 1024 * 1024 * 10)) },
            Filter.Duplications::class to { Filter.Duplications() },
            Filter.NameDiff::class to { Filter.NameDiff() }
        )

        fun without(vararg klass: KClass<out Filter.Rule>) = apply {
            rules -= klass
        }

        fun build(): FilterSet = FilterSet(rules, operators)
    }
}