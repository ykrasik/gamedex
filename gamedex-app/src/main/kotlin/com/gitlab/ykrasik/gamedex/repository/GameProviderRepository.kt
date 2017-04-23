package com.gitlab.ykrasik.gamedex.repository

import com.gitlab.ykrasik.gamedex.GameProvider
import com.gitlab.ykrasik.gamedex.GameProviderType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 07/04/2017
 * Time: 21:30
 */
@Singleton
class GameProviderRepository @Inject constructor(allProviders: MutableSet<GameProvider>) {
    // TODO: Allow enabling / disabling providers? Is this needed?
    val providers = run {
        check(allProviders.isNotEmpty()) { "No providers are active! Please activate at least 1 provider." }
        allProviders.sortedBy { it.info.type.ordinal }
    }

    private val providersByType = allProviders.associateBy { it.info.type }

    operator fun get(type: GameProviderType) = providersByType[type]
}