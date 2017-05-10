package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.preferences.GamePreferences
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.ui.fragment.ChooseSearchResultFragment
import com.gitlab.ykrasik.gamedex.util.collapseSpaces
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.run
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
    suspend fun search(name: String, platform: Platform, path: File, progress: TaskProgress, isSearchAgain: Boolean): List<ProviderData>?

    fun fetch(name: String, platform: Platform, providerHeaders: List<ProviderHeader>, progress: TaskProgress): List<ProviderData>
}

@Singleton
class GameProviderServiceImpl @Inject constructor(
    private val providerRepository: GameProviderRepository,
    private val preferences: GamePreferences,
    private val chooser: SearchChooser
) : GameProviderService {

    private val metaDataRegex = "(\\[.*?\\])".toRegex()

    override suspend fun search(name: String, platform: Platform, path: File, progress: TaskProgress, isSearchAgain: Boolean): List<ProviderData>? =
        try {
            SearchContext(platform, path, progress, isSearchAgain, name.normalizeName()).search()
        } catch (e: CancelSearchException) {
            null
        }

    // Remove all metaData enclosed with '[]' from the file name and collapse all spaces into a single space.
    private fun String.normalizeName(): String = this.replace(metaDataRegex, "").collapseSpaces().replace(" - ", ": ").trim()

    private inner class SearchContext(
        private val platform: Platform,
        private val path: File,
        private val progress: TaskProgress,
        private val isSearchAgain: Boolean,
        private var searchedName: String
    ) {
        private var canAutoContinue = !isSearchAgain
        private val previouslyDiscardedResults: MutableSet<ProviderSearchResult> = mutableSetOf()
        private var userExactMatch: String? = null

        // TODO: Support a back button somehow, it's needed...
        suspend fun search(): List<ProviderData> {
            val results = providerRepository.providers.map { it to search(it) }

            progress.message = "Downloading game data..."
            return results.mapNotNull { (provider, searchResult) ->
                searchResult?.let {
                    async(CommonPool) {
                        provider.fetch(it.apiUrl, platform)
                    }
                }
            }.map { it.await() }
        }

        private suspend fun search(provider: GameProvider): ProviderSearchResult? {
            progress.message = "[${provider.name}][$platform] Searching '$searchedName'..."
            val results = provider.search(searchedName, platform)
            progress.message = "[${provider.name}][$platform] Searching '$searchedName': ${results.size} results."

            val choice = if (isSearchAgain) {
                // If 'search again', always display all results.
                chooseResult(provider, results)
            } else if (userExactMatch != null) {
                val providerExactMatch = results.find { it.name.equals(userExactMatch, ignoreCase = true) }
                if (providerExactMatch != null) {
                    return providerExactMatch
                } else {
                    chooseResult(provider, results)
                }
            } else {
                if (canAutoContinue && results.size == 1 && results.first().name.equals(searchedName, ignoreCase = true)) {
                    return results.first()
                } else {
                    chooseResult(provider, results)
                }
            }

            fun ProviderSearchResult.markChosen() = apply {
                previouslyDiscardedResults += results
                previouslyDiscardedResults -= this
            }

            return when (choice) {
                is SearchChooser.Choice.ExactMatch -> choice.result.markChosen().apply {
                    userExactMatch = name
                    searchedName = name
                }
                is SearchChooser.Choice.NotExactMatch -> choice.result.markChosen()
                is SearchChooser.Choice.NewSearch -> {
                    searchedName = choice.newSearch
                    canAutoContinue = false
                    search(provider)
                }
                SearchChooser.Choice.ProceedWithout -> null
                SearchChooser.Choice.Cancel -> throw CancelSearchException()
            }
        }

        private suspend fun chooseResult(provider: GameProvider, allSearchResults: List<ProviderSearchResult>): SearchChooser.Choice {
            // We only get here when we have no exact matches.
            if (!isSearchAgain && preferences.handsFreeMode) return SearchChooser.Choice.Cancel

            val (filteredResults, results) = allSearchResults.partition { result ->
                previouslyDiscardedResults.any { it.name.equals(result.name, ignoreCase = true) }
            }

            val chooseSearchResultData = SearchChooser.Data(
                searchedName, path, provider.type, results = results, filteredResults = filteredResults
            )
            return chooser.choose(chooseSearchResultData)
        }
    }

    override fun fetch(name: String, platform: Platform, providerHeaders: List<ProviderHeader>, progress: TaskProgress): List<ProviderData> =
        providerHeaders.map { header ->
            val provider = providerRepository[header]
            progress.message = "[${provider.name}][$platform] Fetching '$name'..."
            val result = provider.fetch(header.apiUrl, platform)
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
        object Cancel : Choice()
    }
}

@Singleton
class UISearchChooser : SearchChooser {
    override suspend fun choose(data: SearchChooser.Data) = run(JavaFx) {
        ChooseSearchResultFragment(data).show()
    }
}