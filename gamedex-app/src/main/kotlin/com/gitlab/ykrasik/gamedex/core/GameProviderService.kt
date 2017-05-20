package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.ui.Task
import com.gitlab.ykrasik.gamedex.ui.fragment.ChooseSearchResultFragment
import com.gitlab.ykrasik.gamedex.util.collapseSpaces
import kotlinx.coroutines.experimental.CancellationException
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
    suspend fun search(task: Task<*>, name: String, platform: Platform, path: File, isSearchAgain: Boolean): List<ProviderData>?

    suspend fun download(task: Task<*>, name: String, platform: Platform, headers: List<ProviderHeader>): List<ProviderData>
}

@Singleton
class GameProviderServiceImpl @Inject constructor(
    private val providerRepository: GameProviderRepository,
    private val settings: GameSettings,
    private val chooser: SearchChooser
) : GameProviderService {

    private val metaDataRegex = "(\\[.*?\\])".toRegex()

    override suspend fun search(task: Task<*>, name: String, platform: Platform, path: File, isSearchAgain: Boolean): List<ProviderData>? =
        try {
            SearchContext(task, platform, path, isSearchAgain, name.normalizeName()).search()
        } catch (e: CancelSearchException) {
            null
        }

    // Remove all metaData enclosed with '[]' from the file name and collapse all spaces into a single space.
    private fun String.normalizeName(): String = this.replace(metaDataRegex, "").collapseSpaces().replace(" - ", ": ").trim()

    private inner class SearchContext(
        private val task: Task<*>,
        private val platform: Platform,
        private val path: File,
        private val isSearchAgain: Boolean,
        private var searchedName: String
    ) {
        private var canAutoContinue = !isSearchAgain
        private val previouslyDiscardedResults: MutableSet<ProviderSearchResult> = mutableSetOf()
        private var userExactMatch: String? = null

        // TODO: Support a back button somehow, it's needed...
        suspend fun search(): List<ProviderData> {
            val results = providerRepository.providers.mapNotNull { search(it) }
            return download(task, searchedName, platform, results)
        }

        private suspend fun search(provider: GameProvider): ProviderHeader? {
            // TODO: Instead of writing the platform the provider, draw their logos.
            task.progress.message = "[$platform][${provider.type}] Searching '$searchedName'..."
            val results = provider.search(searchedName, platform)
            task.progress.message = "[$platform][${provider.type}] Searching '$searchedName': ${results.size} results."

            val choice = if (isSearchAgain) {
                // If 'search again', always display all results.
                chooseResult(provider, results)
            } else if (userExactMatch != null) {
                val providerExactMatch = results.find { it.name.equals(userExactMatch, ignoreCase = true) }
                if (providerExactMatch != null) {
                    return providerExactMatch.toHeader(provider)
                } else {
                    chooseResult(provider, results)
                }
            } else {
                if (canAutoContinue && results.size == 1 && results.first().name.equals(searchedName, ignoreCase = true)) {
                    return results.first().toHeader(provider)
                } else {
                    chooseResult(provider, results)
                }
            }

            fun ProviderSearchResult.markChosen() = let {
                previouslyDiscardedResults += results
                previouslyDiscardedResults -= this
                this.toHeader(provider)
            }

            return when (choice) {
                is SearchChooser.Choice.ExactMatch -> choice.result.apply { userExactMatch = name; searchedName = name }.markChosen()
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
            if (!isSearchAgain && settings.handsFreeMode) return SearchChooser.Choice.Cancel

            val (filteredResults, results) = allSearchResults.partition { result ->
                previouslyDiscardedResults.any { it.name.equals(result.name, ignoreCase = true) }
            }

            val chooseSearchResultData = SearchChooser.Data(
                searchedName, path, provider.type, results = results, filteredResults = filteredResults
            )
            return chooser.choose(chooseSearchResultData)
        }

        private fun ProviderSearchResult.toHeader(provider: GameProvider) = ProviderHeader(
            type = provider.type,
            apiUrl = apiUrl,
            siteUrl = ""   // Unused for our purposes.
        )
    }

    override suspend fun download(task: Task<*>, name: String, platform: Platform, headers: List<ProviderHeader>): List<ProviderData> {
        return headers.map { header ->
            async(task.context) {
                // TODO: Instead of writing the platform the provider, draw their logos.
                task.progress.message = "[$platform][${header.type}] Downloading '$name'..."
                if (task.result.isCancelled) throw CancellationException()
                providerRepository[header].download(header.apiUrl, platform)
            }
        }.map { it.await() }
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