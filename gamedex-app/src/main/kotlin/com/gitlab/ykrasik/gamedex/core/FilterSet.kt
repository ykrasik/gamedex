package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.controller.LibraryController
import com.gitlab.ykrasik.gamedex.core.Filter.Companion.filterClass
import com.gitlab.ykrasik.gamedex.core.Filter.Companion.name
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.util.FileSize
import com.gitlab.ykrasik.gamedex.util.toDate
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 25/01/2018
 * Time: 10:04
 */
class FilterSet private constructor(
    private val _rules: Map<KClass<out Filter.Rule>, () -> Filter.Rule>,
    private val _operators: Map<KClass<out Filter.BinaryOperator>, () -> Filter.BinaryOperator>
) {
    val rules = _rules.keys.toList().map { it.name }
    val operators = _operators.keys.toList().map { it.name }

    fun new(klass: KClass<out Filter>): Filter = (_rules[klass] ?: _operators[klass])!!()
    fun new(name: String): Filter = new(name.filterClass)

    class Builder(
        private val settings: GameSettings,
        private val libraryController: LibraryController,
        private val gameController: GameController,
        private val providerRepository: GameProviderRepository
    ) {
        private val operators = mapOf(
            Filter.And::class to { Filter.And() },
            Filter.Or::class to { Filter.Or() }
        )

        private val rules = mutableMapOf(
            Filter.Platform::class to { Filter.Platform(settings.platform) },
            Filter.Library::class to {
                Filter.Library(settings.platform, libraryController.realLibraries.firstOrNull()?.name ?: "")
            },
            Filter.Genre::class to { Filter.Genre(gameController.genres.firstOrNull() ?: "") },
            Filter.Tag::class to { Filter.Tag(gameController.tags.firstOrNull() ?: "") },
            Filter.ReleaseDate::class to { Filter.ReleaseDate("2014-01-01".toDate()) },
            Filter.Provider::class to { Filter.Provider(providerRepository.providers.first().id) },
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