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
    allProviders: MutableSet<DataProvider>,
    private val chooser: GameSearchChooser
) : DataProviderService {
    private val log by logger()

    private val maxScreenshots = 10

    // TODO: Allow enabling / disabling providers
    private val providers = kotlin.run {
        check(allProviders.isNotEmpty()) { "No providers are active! Please activate at least 1 provider." }
        allProviders.sortedBy { it.info.type.basicDataPriority }
    }

    override suspend fun fetch(name: String, platform: GamePlatform, path: File): ProviderGame? {
        try {
            val context = SearchContext(name, platform, path)
            var hasAtLeastOneResult = false
            val fetchResults = providers.mapIndexedNotNull { i, provider ->
                val isLastProvider = i == (providers.size - 1)
                val canProceedWithout = hasAtLeastOneResult || !isLastProvider
                val result = context.fetch(provider, canProceedWithout)
                if (result != null) hasAtLeastOneResult = true
                result
            }
            return fetchResults.merge()
        } catch (e: CancelSearchException) {
            return null
        }
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

    private inner class SearchContext(
        var searchedName: String,
        val platform: GamePlatform,
        val path: File
    ) {
        val previouslySelectedResults: MutableSet<ProviderSearchResult> = mutableSetOf()
        val previouslyDiscardedResults: MutableSet<ProviderSearchResult> = mutableSetOf()

        suspend fun fetch(provider: DataProvider, canProceedWithout: Boolean): ProviderFetchResult? {
            val results = provider.search(searchedName, platform)
            val filteredResults = filterResults(results)
            val chooseSearchResultData = ChooseSearchResultData(searchedName, path, provider.info, filteredResults, canProceedWithout)
            val choice = chooser.choose(chooseSearchResultData)
            return when (choice) {
                is SearchResultChoice.Ok -> {
                    val chosenResult = choice.result
                    previouslyDiscardedResults += filteredResults.filter { it != chosenResult }
                    previouslySelectedResults += chosenResult
                    provider.fetch(chosenResult)
                }
                is SearchResultChoice.NewSearch -> {
                    searchedName = choice.newSearch
                    previouslyDiscardedResults += filteredResults
                    fetch(provider, canProceedWithout)
                }
                SearchResultChoice.ProceedWithout -> {
                    assert(canProceedWithout) { "Cannot proceed without results from '${provider.info.name}'!" }
                    previouslyDiscardedResults += filteredResults
                    null
                }
                SearchResultChoice.Cancel -> throw CancelSearchException()
            }
        }

        private fun filterResults(results: List<ProviderSearchResult>): List<ProviderSearchResult> {
//            results.forEach { result ->
//                if (previouslySelectedResults.any { it.name.equals(result.name, ignoreCase = true) }) {
//                    return listOf(result)
//                }
//            }
            return results.filterNot { result ->
                previouslyDiscardedResults.any { it.name.equals(result.name, ignoreCase = true) }
            }
        }
    }

    private class CancelSearchException : RuntimeException()
}