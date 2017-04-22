package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.DataProvider
import com.gitlab.ykrasik.gamedex.GamePlatform
import com.gitlab.ykrasik.gamedex.ProviderFetchResult
import com.gitlab.ykrasik.gamedex.ProviderSearchResult
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 13/10/2016
 * Time: 13:29
 */
interface DataProviderService {
    suspend fun fetch(name: String, platform: GamePlatform, path: File): List<ProviderFetchResult>?
}

@Singleton
class DataProviderServiceImpl @Inject constructor(
    private val providerRepository: DataProviderRepository,
    private val chooser: GameSearchChooser
) : DataProviderService {

    override suspend fun fetch(name: String, platform: GamePlatform, path: File): List<ProviderFetchResult>? =
        try {
            SearchContext(platform, path).fetch(name)
        } catch (e: CancelSearchException) {
            null
        }

    private inner class SearchContext(private val platform: GamePlatform, private val path: File) {
        val previouslySelectedResults: MutableSet<ProviderSearchResult> = mutableSetOf()
        val previouslyDiscardedResults: MutableSet<ProviderSearchResult> = mutableSetOf()

        suspend fun fetch(searchedName: String): List<ProviderFetchResult> {
            return providerRepository.providers.mapNotNull { fetch(it, searchedName) }
        }

        private suspend fun fetch(provider: DataProvider, searchedName: String): ProviderFetchResult? {
            val results = provider.search(searchedName, platform)
            val filteredResults = filterResults(results)
            val chooseSearchResultData = ChooseSearchResultData(searchedName, path, provider.info, filteredResults)
            val choice = chooser.choose(chooseSearchResultData)
            return when (choice) {
                is SearchResultChoice.Ok -> {
                    val chosenResult = choice.result
                    previouslyDiscardedResults += filteredResults.filter { it != chosenResult }
                    previouslySelectedResults += chosenResult
                    provider.fetch(chosenResult.apiUrl, platform)
                }
                is SearchResultChoice.NewSearch -> {
                    previouslyDiscardedResults += filteredResults
                    fetch(provider, choice.newSearch)
                }
                SearchResultChoice.ProceedWithout -> null
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