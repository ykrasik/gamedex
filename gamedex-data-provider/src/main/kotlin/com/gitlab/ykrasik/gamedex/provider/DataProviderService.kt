package com.gitlab.ykrasik.gamedex.provider

import com.github.ykrasik.gamedex.common.datamodel.GameData
import com.github.ykrasik.gamedex.common.datamodel.GamePlatform
import com.github.ykrasik.gamedex.common.datamodel.ImageData
import com.github.ykrasik.gamedex.common.util.logger
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 13/10/2016
 * Time: 13:29
 */
interface DataProviderService {
    fun fetch(name: String, platform: GamePlatform, path: File): ProviderGame?
}

@Singleton
class DataProviderServiceImpl @Inject constructor(
    private val providers: MutableSet<DataProvider>,
    private val chooser: GameSearchChooser
) : DataProviderService {
    private val log by logger()

    override fun fetch(name: String, platform: GamePlatform, path: File): ProviderGame? {
        val providers = sortProviders()

        val context = SearchContext(name, path)
        val fetchResults = providers.map { provider ->
            val results = provider.search(name, platform)
            val result = chooser.choose(provider.info, results, context) ?: return null
            provider.fetch(result)
        }
        return fetchResults.merge()
    }

    private fun sortProviders(): List<DataProvider> {
        check(providers.isNotEmpty()) { "No providers are active! Please activate at least 1 provider." }
        return providers.sortedBy { it.info.type.basicDataPriority }
    }

    private fun List<ProviderFetchResult>.merge(): ProviderGame? {
        check(this.isNotEmpty()) { "No provider managed to return any GameData!" }

        val resultsByBasicPriority = this.sortedBy { it.providerData.type.basicDataPriority }
        val resultsByScorePriority = this.sortedBy { it.providerData.type.scorePriority }
        val resultsByImagePriority = this.sortedBy { it.providerData.type.imagePriorty }

        val name = resultsByBasicPriority.first().gameData.name
        log.debug { "Processing: '$name'..." }

        val gameData = GameData(
            name = name,
            description = resultsByBasicPriority.findFirst("description") { it.gameData.description },
            releaseDate = resultsByBasicPriority.findFirst("releaseDate") { it.gameData.releaseDate },

            criticScore = resultsByScorePriority.findFirst("criticScore") { it.gameData.criticScore },
            userScore = resultsByScorePriority.findFirst("userScore") { it.gameData.userScore },

            genres = this.flatMapTo(mutableSetOf<String>()) { it.gameData.genres }.toList()
        )

        // TODO: Maybe there's a better way of doing this?
        // FIXME: Merge screenshots, so the first ones are from the first priority provider, then fill out with screenshots from other providers
        val imageData = ImageData(
            thumbnailUrl = resultsByImagePriority.findFirst("thumbnail") { it.imageData.thumbnailUrl },
            posterUrl = resultsByImagePriority.findFirst("poster") { it.imageData.posterUrl },
            screenshot1Url = resultsByImagePriority.findFirst("screenshot1Url") { it.imageData.screenshot1Url },
            screenshot2Url = resultsByImagePriority.findFirst("screenshot2Url") { it.imageData.screenshot2Url },
            screenshot3Url = resultsByImagePriority.findFirst("screenshot3Url") { it.imageData.screenshot3Url },
            screenshot4Url = resultsByImagePriority.findFirst("screenshot4Url") { it.imageData.screenshot4Url },
            screenshot5Url = resultsByImagePriority.findFirst("screenshot5Url") { it.imageData.screenshot5Url },
            screenshot6Url = resultsByImagePriority.findFirst("screenshot6Url") { it.imageData.screenshot6Url },
            screenshot7Url = resultsByImagePriority.findFirst("screenshot7Url") { it.imageData.screenshot7Url },
            screenshot8Url = resultsByImagePriority.findFirst("screenshot8Url") { it.imageData.screenshot8Url },
            screenshot9Url = resultsByImagePriority.findFirst("screenshot9Url") { it.imageData.screenshot9Url },
            screenshot10Url = resultsByImagePriority.findFirst("screenshot10Url") { it.imageData.screenshot10Url }
        )

        val providerData = this.map { it.providerData }

        return ProviderGame(gameData, imageData, providerData)
    }

    private fun <T> List<ProviderFetchResult>.findFirst(field: String, extractor: (ProviderFetchResult) -> T?): T? {
        val fetchResult = this.firstOrNull { extractor(it) != null }
        return if (fetchResult != null) {
            val value = extractor(fetchResult)
            log.debug { "[$field][${fetchResult.providerData.type}]: $value" }
            value
        } else {
            log.debug { "[$field]: Empty." }
            null
        }
    }
}