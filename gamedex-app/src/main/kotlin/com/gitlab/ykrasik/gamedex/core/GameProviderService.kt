package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.preferences.GamePreferences
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.ui.fragment.ChooseSearchResultFragment
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
class GameProviderServiceImpl @Inject constructor(
    private val providerRepository: GameProviderRepository,
    private val preferences: GamePreferences,
    private val chooser: SearchChooser
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
        private val previouslyDiscardedResults: MutableSet<ProviderSearchResult> = mutableSetOf()
        private val latestProviderResults = mutableMapOf<GameProviderType, List<ProviderSearchResult>>()
        private var userExactMatch: String? = null

        suspend fun search(): List<RawGameData> {
            val results = providerRepository.providers.map { it to search(it) }

            // TODO: Fetch async per provider.
            return results.mapNotNull { (provider, searchResult) ->
                searchResult?.let {
                    provider.fetch(it.apiUrl, platform)
                }
            }
        }

        private suspend fun search(provider: GameProvider): ProviderSearchResult? {
            progress.message = "[${provider.name}][$platform] Searching '$searchedName'..."
            val results = provider.search(searchedName, platform)
            latestProviderResults[provider.type] = results
            progress.message = "[${provider.name}][$platform] Searching '$searchedName': ${results.size} results."

            val choice = if (!isNewSearch) {
                chooseResult(provider, results)
            } else if (userExactMatch != null) {
                val providerExactMatch = results.find { it.name.equals(userExactMatch, ignoreCase = true) }
                if (providerExactMatch != null) {
                    return providerExactMatch
                } else {
                    chooseResult(provider, results)
                }
            } else {
                // TODO: pressing 'new search' should prevent this.
                if (results.size == 1 && results.first().name.equals(searchedName, ignoreCase = true)) {
                    return results.first()
                } else {
                    chooseResult(provider, results)
                }
            }

            return when (choice) {
                is SearchChooser.Choice.ExactMatch -> {
                    val chosenResult = choice.result
                    userExactMatch = chosenResult.name
                    previouslyDiscardedResults += results
                    previouslyDiscardedResults -= chosenResult
                    chosenResult
                }
                is SearchChooser.Choice.NotExactMatch -> {
                    val chosenResult = choice.result
                    previouslyDiscardedResults += results
                    previouslyDiscardedResults -= chosenResult
                    chosenResult
                }
                is SearchChooser.Choice.NewSearch -> {
                    searchedName = choice.newSearch
                    search(provider)
                }
                SearchChooser.Choice.ProceedWithout -> null
                SearchChooser.Choice.GoBack -> {
                    // TODO
                    TODO()
                }
                SearchChooser.Choice.Cancel -> throw CancelSearchException()
            }
        }

        private suspend fun chooseResult(provider: GameProvider, allSearchResults: List<ProviderSearchResult>): SearchChooser.Choice {
            // We only get here when we have no exact matches.
            // TODO: Is this too restrictive? Allow handsFreeMode to auto accept in hands free mode?
            if (preferences.handsFreeMode) return SearchChooser.Choice.Cancel

            val (filteredResults, results) = allSearchResults.partition { result ->
                previouslyDiscardedResults.any { it.name.equals(result.name, ignoreCase = true) }
            }

            val chooseSearchResultData = SearchChooser.Data(
                searchedName, path, provider.type, results = results, filteredResults = filteredResults
            )
            return chooser.choose(chooseSearchResultData)
        }
    }

    override fun fetch(name: String, platform: Platform, providerData: List<ProviderData>, progress: TaskProgress): List<RawGameData> =
        providerData.map { providerData ->
            val provider = providerRepository[providerData]
            progress.message = "[${provider.name}][$platform] Fetching '$name'..."
            val result = provider.fetch(providerData.apiUrl, platform)
            progress.message = "[${provider.name}][$platform] Fetching '$name'...: Done."
            result
        }

    private class CancelSearchException : RuntimeException()
}

interface SearchChooser {
    suspend fun choose(data: Data): Choice

    data class Data(
        val name: String,
        val path: File,
        val providerType: GameProviderType,
        val results: List<ProviderSearchResult>,
        val filteredResults: List<ProviderSearchResult>
    )

    sealed class Choice {
        data class ExactMatch(val result: ProviderSearchResult) : Choice()
        data class NotExactMatch(val result: ProviderSearchResult) : Choice()
        data class NewSearch(val newSearch: String) : Choice()
        object ProceedWithout : Choice()
        object GoBack : Choice()
        object Cancel : Choice()
    }
}

@Singleton
class UISearchChooser @Inject constructor() : SearchChooser {
    override suspend fun choose(data: SearchChooser.Data) = ChooseSearchResultFragment(data).show()
}