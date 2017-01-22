package com.gitlab.ykrasik.gamedex.provider

import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.datamodel.GamePlatform
import com.github.ykrasik.gamedex.datamodel.ProviderGameData
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 13/10/2016
 * Time: 13:29
 */
interface DataProviderService {
    fun fetch(name: String, platform: GamePlatform, path: File): List<ProviderGameData>?
}

@Singleton
class DataProviderServiceImpl @Inject constructor(
    private val providers: MutableSet<DataProvider>,
    private val chooser: GameSearchChooser
) : DataProviderService {
    private val log by logger()

    override fun fetch(name: String, platform: GamePlatform, path: File): List<ProviderGameData>? {
        check(providers.isNotEmpty()) { "No providers are active! Please activate at least 1 provider." }

        val context = SearchContext(name, path)
        return providers.map { provider ->
            val results = provider.search(name, platform)
            val result = chooser.choose(provider.info, results, context) ?: return null
            provider.fetch(result)
        }
    }
}