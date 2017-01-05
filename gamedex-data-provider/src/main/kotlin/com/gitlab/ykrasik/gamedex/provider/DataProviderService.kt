package com.gitlab.ykrasik.gamedex.provider

import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.datamodel.GameData
import com.github.ykrasik.gamedex.datamodel.GameImageData
import com.github.ykrasik.gamedex.datamodel.GamePlatform
import com.github.ykrasik.gamedex.datamodel.ProviderData
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 13/10/2016
 * Time: 13:29
 */
interface DataProviderService {
    // TODO: Create an object for this?
    fun fetch(name: String, platform: GamePlatform, path: File): Pair<GameData, GameImageData>?
}

@Singleton
class DataProviderServiceImpl @Inject constructor(
    private val providers: MutableSet<DataProvider>,
    private val chooser: GameSearchChooser
) : DataProviderService {
    private val log by logger()

    override fun fetch(name: String, platform: GamePlatform, path: File): Pair<GameData, GameImageData>? {
        check(providers.isNotEmpty()) { "No providers are active! Please activate at least 1 provider." }

        val context = SearchContext(name, path)
        val fetchResults = providers.map { provider ->
            val results = provider.search(name, platform)
            val result = chooser.choose(provider.info, results, context) ?: return null
            provider.fetch(result)
        }
        return fetchResults.merge(name)
    }

    private fun List<ProviderFetchResult>.merge(gameName: String): Pair<GameData, GameImageData>? {
        check(this.isNotEmpty()) { "No provider managed to return any GameData!" }

        val basicData = this.sortedBy { it.type.basicDataPriority }
        val scoreData = this.sortedBy { it.type.scorePriority }
        val imageData = this.sortedBy { it.type.imagePriorty }

        val gameData = GameData(
            name = basicData.first().name,
            description = basicData.findFirst(gameName, "description") { it.description },
            releaseDate = basicData.findFirst(gameName, "releaseDate") { it.releaseDate },

            criticScore = scoreData.findFirst(gameName, "criticScore") { it.criticScore },
            userScore = scoreData.findFirst(gameName, "userScore") { it.userScore },

            genres = this.flatMapTo(mutableSetOf<String>()) { it.genres }.toList(),
            providerData = this.map {
                ProviderData(
                    type = it.type,
                    detailUrl = it.detailUrl
                )
            }
        )

        // TODO: Maybe there's a better way of doing this?
        val gameImageData = GameImageData(
            thumbnailUrl = imageData.findFirst(gameName, "thumbnail") { it.imageData.thumbnailUrl },
            posterUrl = imageData.findFirst(gameName, "poster") { it.imageData.posterUrl },
            screenshotUrls = emptyList()    // TODO: Support screenshots.
        )

        return Pair(gameData, gameImageData)
    }

    private fun <T> List<ProviderFetchResult>.findFirst(gameName: String, field: String, extractor: (ProviderFetchResult) -> T?): T? {
        val providerData = this.firstOrNull { extractor(it) != null }
        return if (providerData != null) {
            val value = extractor(providerData)
            log.debug { "[$gameName][$field][${providerData.type}]: $value" }
            value
        } else {
            log.debug { "[$gameName][$field]: Empty." }
            null
        }
    }
}