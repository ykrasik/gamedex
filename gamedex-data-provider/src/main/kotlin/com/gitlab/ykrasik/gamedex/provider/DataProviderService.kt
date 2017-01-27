package com.gitlab.ykrasik.gamedex.provider

import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.datamodel.GameData
import com.github.ykrasik.gamedex.datamodel.GameImageData
import com.github.ykrasik.gamedex.datamodel.GamePlatform
import com.github.ykrasik.gamedex.datamodel.GameProviderData
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
    fun fetch(name: String, platform: GamePlatform, path: File): Triple<List<GameProviderData>, GameData, GameImageData>?
}

@Singleton
class DataProviderServiceImpl @Inject constructor(
    private val providers: MutableSet<DataProvider>,
    private val chooser: GameSearchChooser
) : DataProviderService {
    private val log by logger()

    override fun fetch(name: String, platform: GamePlatform, path: File): Triple<List<GameProviderData>, GameData, GameImageData>? {
        check(providers.isNotEmpty()) { "No providers are active! Please activate at least 1 provider." }

        val context = SearchContext(name, path)
        val fetchResults = providers.map { provider ->
            val results = provider.search(name, platform)
            val result = chooser.choose(provider.info, results, context) ?: return null
            provider.fetch(result)
        }
        return fetchResults.merge()
    }

    private fun List<ProviderFetchResult>.merge(): Triple<List<GameProviderData>, GameData, GameImageData>? {
        check(this.isNotEmpty()) { "No provider managed to return any GameData!" }

        val basicData = this.sortedBy { it.providerData.type.basicDataPriority }
        val scoreData = this.sortedBy { it.providerData.type.scorePriority }
        val imageData = this.sortedBy { it.providerData.type.imagePriorty }

        val providerData = this.map { it.providerData }

        val gameName = basicData.first().gameData.name
        val gameData = GameData(
            name = gameName,
            description = basicData.findFirst(gameName, "description") { it.gameData.description },
            releaseDate = basicData.findFirst(gameName, "releaseDate") { it.gameData.releaseDate },

            criticScore = scoreData.findFirst(gameName, "criticScore") { it.gameData.criticScore },
            userScore = scoreData.findFirst(gameName, "userScore") { it.gameData.userScore },

            genres = this.flatMapTo(mutableSetOf<String>()) { it.gameData.genres }.toList()
        )

        // TODO: Maybe there's a better way of doing this?
        val gameImageData = GameImageData(
            thumbnailUrl = imageData.findFirst(gameName, "thumbnail") { it.imageData.thumbnailUrl },
            posterUrl = imageData.findFirst(gameName, "poster") { it.imageData.posterUrl },
            screenshot1Url = imageData.findFirst(gameName, "screenshot1Url") { it.imageData.screenshot1Url },
            screenshot2Url = imageData.findFirst(gameName, "screenshot2Url") { it.imageData.screenshot2Url },
            screenshot3Url = imageData.findFirst(gameName, "screenshot3Url") { it.imageData.screenshot3Url },
            screenshot4Url = imageData.findFirst(gameName, "screenshot4Url") { it.imageData.screenshot4Url },
            screenshot5Url = imageData.findFirst(gameName, "screenshot5Url") { it.imageData.screenshot5Url },
            screenshot6Url = imageData.findFirst(gameName, "screenshot6Url") { it.imageData.screenshot6Url },
            screenshot7Url = imageData.findFirst(gameName, "screenshot7Url") { it.imageData.screenshot7Url },
            screenshot8Url = imageData.findFirst(gameName, "screenshot8Url") { it.imageData.screenshot8Url },
            screenshot9Url = imageData.findFirst(gameName, "screenshot9Url") { it.imageData.screenshot9Url },
            screenshot10Url = imageData.findFirst(gameName, "screenshot10Url") { it.imageData.screenshot10Url }
        )

        return Triple(providerData, gameData, gameImageData)
    }

    private fun <T> List<ProviderFetchResult>.findFirst(gameName: String, field: String, extractor: (ProviderFetchResult) -> T?): T? {
        val fetchResult = this.firstOrNull { extractor(it) != null }
        return if (fetchResult != null) {
            val value = extractor(fetchResult)
            log.debug { "[$gameName][$field][${fetchResult.providerData.type}]: $value" }
            value
        } else {
            log.debug { "[$gameName][$field]: Empty." }
            null
        }
    }
}