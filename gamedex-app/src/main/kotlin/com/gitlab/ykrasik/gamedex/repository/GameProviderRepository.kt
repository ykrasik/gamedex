package com.gitlab.ykrasik.gamedex.repository

import com.gitlab.ykrasik.gamedex.GameProvider
import com.gitlab.ykrasik.gamedex.ProviderId
import com.gitlab.ykrasik.gamedex.settings.ProviderSettings
import com.gitlab.ykrasik.gamedex.ui.map
import com.gitlab.ykrasik.gamedex.ui.sortedFiltered
import com.gitlab.ykrasik.gamedex.ui.toImage
import com.gitlab.ykrasik.gamedex.util.logger
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
    private val log = logger()

    private val _providers = run {
        check(allProviders.isNotEmpty()) { "No providers are active! Please activate at least 1 provider." }
        val providers = allProviders.toList().sortedFiltered()
        log.info("Detected providers: ${providers.items}")
        providers.sortedItems.comparatorProperty().bind(settings.searchOrderProperty.map { it!!.toComparator() })
        providers
    }

    // TODO: Allow enabling / disabling providers? Is this needed?
    val providers: List<GameProvider> get() = _providers

    private val providersById = allProviders.associateBy { it.id }
    private val providerLogos = providersById.mapValues { it.value.logo.toImage() }

    operator fun get(id: ProviderId) = providersById[id]!!

    // TODO: Create a ProviderWithLogo class instead?
    fun logo(id: ProviderId) = providerLogos[id]!!
}