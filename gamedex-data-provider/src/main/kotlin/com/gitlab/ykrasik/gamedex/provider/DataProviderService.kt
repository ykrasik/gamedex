package com.gitlab.ykrasik.gamedex.provider

import com.github.ykrasik.gamedex.datamodel.GamePlatform
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 13/10/2016
 * Time: 13:29
 */
interface DataProviderService {
    fun fetch(name: String, platform: GamePlatform, chooser: (List<SearchResult>) -> SearchResult?): ProviderGameData?
}

@Singleton
class DataProviderServiceImpl @Inject constructor(private val providers: MutableSet<DataProvider>): DataProviderService {
    override fun fetch(name: String, platform: GamePlatform, chooser: (List<SearchResult>) -> SearchResult?): ProviderGameData? {
        // FIXME: Support more then 1 provider!
        val provider = providers.first()
        val results = provider.search(name, platform)
        val result = chooser(results) ?: return null
        return provider.fetch(result)
    }
}