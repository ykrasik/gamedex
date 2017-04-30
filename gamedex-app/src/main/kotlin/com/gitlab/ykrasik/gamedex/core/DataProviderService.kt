package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.preferences.UserPreferences
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 13/10/2016
 * Time: 13:29
 */
interface DataProviderService {
    // TODO: I don't want this to suspend, see if it's possible to make it Produce results to the calling class.
    suspend fun search(name: String, platform: Platform, path: File, progress: TaskProgress): List<RawGameData>?

    fun fetch(providerData: List<ProviderData>, platform: Platform): List<RawGameData>
}

@Singleton
class DataProviderServiceImpl @Inject constructor(
    private val providerRepository: GameProviderRepository,
    private val userPreferences: UserPreferences,
    private val chooser: GameSearchChooser
) : DataProviderService {

    override suspend fun search(name: String, platform: Platform, path: File, progress: TaskProgress): List<RawGameData>? =
        try {
            SearchContext(platform, path, progress, name).search()
        } catch (e: CancelSearchException) {
            null
        }

    private inner class SearchContext(
        private val platform: Platform,
        private val path: File,
        private val progress: TaskProgress,
        private var searchedName: String
    ) {
        val previouslySelectedResults: MutableSet<ProviderSearchResult> = mutableSetOf()
        val previouslyDiscardedResults: MutableSet<ProviderSearchResult> = mutableSetOf()

        suspend fun search(): List<RawGameData> {
            return providerRepository.providers.mapNotNull { search(it) }
        }

        private suspend fun search(provider: GameProvider): RawGameData? {
            progress.message = "[${provider.info.name}] Searching: '$searchedName'..."
            val results = provider.search(searchedName, platform)
            progress.message = "[${provider.info.name}] Searching: '$searchedName': ${results.size} results."

            val filteredResults = filterResults(results)
            val choice = if (filteredResults.size == 1) {
                SearchResultChoice.Ok(filteredResults.first())
            } else if (userPreferences.handsFreeMode) {
                SearchResultChoice.Cancel
            } else {
                val chooseSearchResultData = ChooseSearchResultData(searchedName, path, provider.info.type, filteredResults)
                chooser.choose(chooseSearchResultData)
            }
            return when (choice) {
                is SearchResultChoice.Ok -> {
                    val chosenResult = choice.result
                    previouslyDiscardedResults += filteredResults.filter { it != chosenResult }
                    previouslySelectedResults += chosenResult
                    provider.fetch(chosenResult.apiUrl, platform)
                }
                is SearchResultChoice.NewSearch -> {
                    previouslyDiscardedResults += filteredResults
                    searchedName = choice.newSearch
                    search(provider)
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

    override fun fetch(providerData: List<ProviderData>, platform: Platform): List<RawGameData> =
        providerData.map { providerData ->
            val provider = providerRepository[providerData]
            provider.fetch(providerData.apiUrl, platform)
        }

    private class CancelSearchException : RuntimeException()
}

sealed class SearchResultChoice {
    data class Ok(val result: ProviderSearchResult) : SearchResultChoice()
    data class NewSearch(val newSearch: String) : SearchResultChoice()
    object Cancel : SearchResultChoice()
    object ProceedWithout : SearchResultChoice()
}