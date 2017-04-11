package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.datamodel.DataProviderType
import com.gitlab.ykrasik.gamedex.provider.DataProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 07/04/2017
 * Time: 21:30
 */
@Singleton
class DataProviderRepository @Inject constructor(allProviders: MutableSet<DataProvider>) {
    // TODO: Allow enabling / disabling providers
    private val _providers = run {
        check(allProviders.isNotEmpty()) { "No providers are active! Please activate at least 1 provider." }
        allProviders.sortedBy { it.info.type.basicDataPriority }
    }

    init {
        println(allProviders)
    }

    private val providersByType = allProviders.associateBy { it.info.type }

    val providers: List<DataProvider> get() = _providers

    operator fun get(type: DataProviderType) = providersByType[type]
}
