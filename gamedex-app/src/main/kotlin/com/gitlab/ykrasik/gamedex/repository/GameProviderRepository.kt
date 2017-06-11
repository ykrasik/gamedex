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
        val providers = allProviders.map { GameProviderWithLogo(it) }.sortedFiltered()
        log.info("Detected providers: ${providers.items.sortedBy { it.id }}")
        providers.sortedItems.comparatorProperty().bind(settings.searchOrderProperty.map { it!!.toComparator() })
        providers
    }

    // TODO: Allow enabling / disabling providers? Is this needed?
    val providers: List<GameProviderWithLogo> get() = _providers

    private val providersById = _providers.associateBy { it.id }

    operator fun get(id: ProviderId) = providersById[id]!!
}

class GameProviderWithLogo(private val provider: GameProvider) : GameProvider by provider {
    val logoImage = provider.logo.toImage()
    override fun toString() = provider.toString()
}