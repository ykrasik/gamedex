package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.preferences.GamePreferences
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 13/10/2016
 * Time: 13:29
 */
interface GameProviderService {
    // TODO: I don't want this to suspend, see if it's possible to make it Produce results to the calling class.
    suspend fun search(name: String, platform: Platform, path: File, progress: TaskProgress, isNewSearch: Boolean): List<RawGameData>?

    fun fetch(name: String, platform: Platform, providerData: List<ProviderData>, progress: TaskProgress): List<RawGameData>
}

@Singleton
class DataProviderServiceImpl @Inject constructor(
    private val providerRepository: GameProviderRepository,
    private val preferences: GamePreferences,
    private val chooser: GameSearchChooser
) : GameProviderService {

    override suspend fun search(name: String, platform: Platform, path: File, progress: TaskProgress, isNewSearch: Boolean): List<RawGameData>? =
        try {
            SearchContext(platform, path, progress, isNewSearch, name).search()
        } catch (e: CancelSearchException) {
            null
        }

    private inner class SearchContext(
        private val platform: Platform,
        private val path: File,
        private val progress: TaskProgress,
        private val isNewSearch: Boolean,
        private var searchedName: String
    ) {
        private var autoProceed = isNewSearch

//        val previouslySelectedResults: MutableSet<ProviderSearchResult> = mutableSetOf()
        val previouslyDiscardedResults: MutableSet<ProviderSearchResult> = mutableSetOf()

        suspend fun search(): List<RawGameData> = providerRepository.providers.mapNotNull { search(it) }

        private suspend fun search(provider: GameProvider): RawGameData? {
            progress.message = "[${provider.info.name}][$platform] Searching '$searchedName'..."
            val results = provider.search(searchedName, platform)
            progress.message = "[${provider.info.name}][$platform] Searching '$searchedName': ${results.size} results."

            val filteredResults = filterResults(results)
            val choice = chooseResult(provider, filteredResults)

            return when (choice) {
                is SearchResultChoice.Ok -> {
                    val chosenResult = choice.result
                    previouslyDiscardedResults += filteredResults.filter { it != chosenResult }
//                    previouslySelectedResults += chosenResult
                    provider.fetch(chosenResult.apiUrl, platform)
                }
                is SearchResultChoice.ProceedCarefully -> {
                    val chosenResult = choice.result
                    previouslyDiscardedResults.clear()
                    autoProceed = false
//                    previouslySelectedResults += chosenResult
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

        private suspend fun chooseResult(provider: GameProvider, results: List<ProviderSearchResult>): SearchResultChoice {
            return when {
                results.size == 1 && autoProceed -> SearchResultChoice.Ok(results.first())
                results.size != 1 && autoProceed && preferences.handsFreeMode -> SearchResultChoice.Cancel
                else -> {
                    val chooseSearchResultData = ChooseSearchResultData(searchedName, path, provider.info.type, isNewSearch, results)
                    chooser.choose(chooseSearchResultData)
                }
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

    override fun fetch(name: String, platform: Platform, providerData: List<ProviderData>, progress: TaskProgress): List<RawGameData> =
        providerData.map { providerData ->
            val provider = providerRepository[providerData]
            progress.message = "[${provider.info.name}][$platform] Fetching '$name'..."
            val result = provider.fetch(providerData.apiUrl, platform)
            progress.message = "[${provider.info.name}][$platform] Fetching '$name'...: Done."
            result
        }

    private class CancelSearchException : RuntimeException()
}

sealed class SearchResultChoice {
    data class Ok(val result: ProviderSearchResult) : SearchResultChoice()
    data class ProceedCarefully(val result: ProviderSearchResult) : SearchResultChoice()
    data class NewSearch(val newSearch: String) : SearchResultChoice()
    object Cancel : SearchResultChoice()
    object ProceedWithout : SearchResultChoice()
}