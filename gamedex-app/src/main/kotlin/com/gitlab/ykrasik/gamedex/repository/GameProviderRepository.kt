package com.gitlab.ykrasik.gamedex.repository

import com.gitlab.ykrasik.gamedex.GameProvider
import com.gitlab.ykrasik.gamedex.GameProviderType
import com.gitlab.ykrasik.gamedex.ProviderHeader
import com.gitlab.ykrasik.gamedex.preferences.ProviderPreferences
import com.gitlab.ykrasik.gamedex.ui.mapProperty
import com.gitlab.ykrasik.gamedex.ui.toImage
import tornadofx.SortedFilteredList
import tornadofx.observable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 07/04/2017
 * Time: 21:30
 */
@Singleton
class GameProviderRepository @Inject constructor(
    allProviders: MutableSet<GameProvider>,
    preferences: ProviderPreferences
) {
    private val providerComparator = preferences.searchOrderProperty.mapProperty { it!!.toComparator().reversed() }

    private val _providers = run {
        check(allProviders.isNotEmpty()) { "No providers are active! Please activate at least 1 provider." }
        val providers = SortedFilteredList(allProviders.toList().observable())
        providers.sortedItems.comparatorProperty().bind(providerComparator)
        providers
    }

    // TODO: Allow enabling / disabling providers? Is this needed?
    val providers: List<GameProvider> get() = _providers

    private val providersByType = allProviders.associateBy { it.type }
    private val providerLogos = providersByType.mapValues { it.value.logo.toImage() }

    operator fun get(type: GameProviderType) = providersByType[type]!!
    operator fun get(header: ProviderHeader) = get(header.type)

    fun logo(type: GameProviderType) = providerLogos[type]!!
    fun logo(header: ProviderHeader) = logo(header.type)
}