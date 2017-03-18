package com.gitlab.ykrasik.gamedex.provider

import com.gitlab.ykrasik.gamedex.common.datamodel.GameData
import com.gitlab.ykrasik.gamedex.common.datamodel.GamePlatform
import com.gitlab.ykrasik.gamedex.common.datamodel.ImageUrls
import com.gitlab.ykrasik.gamedex.common.util.logger
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 13/10/2016
 * Time: 13:29
 */
interface DataProviderService {
    suspend fun fetch(name: String, platform: GamePlatform, path: File): ProviderGame?
}

@Singleton
class DataProviderServiceImpl @Inject constructor(
    private val providers: MutableSet<DataProvider>,
    private val chooser: GameSearchChooser
) : DataProviderService {
    private val log by logger()

    private val maxScreenshots = 10

    override suspend fun fetch(name: String, platform: GamePlatform, path: File): ProviderGame? {
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

        val thumbnailUrl = resultsByImagePriority.findFirst("thumbnail") { it.imageUrls.thumbnailUrl }
        val posterUrl = resultsByImagePriority.findFirst("poster") { it.imageUrls.posterUrl }
        val screenshotUrls = resultsByImagePriority.asSequence().flatMap { it.imageUrls.screenshotUrls.asSequence() }.take(maxScreenshots).toList()
        val imageData = ImageUrls(
            thumbnailUrl = thumbnailUrl ?: posterUrl,
            posterUrl = posterUrl ?: thumbnailUrl,
            screenshotUrls = screenshotUrls
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