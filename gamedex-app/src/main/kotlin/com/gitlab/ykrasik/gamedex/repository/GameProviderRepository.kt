package com.gitlab.ykrasik.gamedex.repository

import com.gitlab.ykrasik.gamedex.GameProvider
import com.gitlab.ykrasik.gamedex.GameProviderType
import com.gitlab.ykrasik.gamedex.ProviderHeader
import com.gitlab.ykrasik.gamedex.settings.ProviderSettings
import com.gitlab.ykrasik.gamedex.ui.map
import com.gitlab.ykrasik.gamedex.ui.sortedFiltered
import com.gitlab.ykrasik.gamedex.ui.toImage
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
    settings: ProviderSettings
) {

    private val _providers = run {
        check(allProviders.isNotEmpty()) { "No providers are active! Please activate at least 1 provider." }
        val providers = allProviders.toList().sortedFiltered()
        providers.sortedItems.comparatorProperty().bind(settings.searchOrderProperty.map { it!!.toComparator() })
        providers
    }

    // TODO: Allow enabling / disabling providers? Is this needed?
    val providers: List<GameProvider> get() = _providers

    private val providersByType = allProviders.associateBy { it.type }
    private val providerLogos = providersByType.mapValues { it.value.logo.toImage() }

    operator fun get(type: GameProviderType) = providersByType[type]!!
    operator fun get(header: ProviderHeader) = get(header.type)

    // TODO: Create a ProviderWithLogo class instead?
    fun logo(type: GameProviderType) = providerLogos[type]!!
    fun logo(header: ProviderHeader) = logo(header.type)
}