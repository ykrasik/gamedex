package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
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
    suspend fun search(taskData: ProviderTaskData, constraints: SearchConstraints): List<ProviderData>?

    suspend fun download(taskData: ProviderTaskData, headers: List<ProviderHeader>): List<ProviderData>

    data class ProviderTaskData(
        val task: Task<*>,
        val name: String,
        val platform: Platform,
        val path: File
    )

    data class SearchConstraints(
        val mode: SearchMode = SearchMode.askIfNonExact,
        val onlySearch: List<GameProviderType> = emptyList()
    ) {
        enum class SearchMode(val key: String) {
            askIfNonExact("If non-exact: Ask"),
            alwaysAsk("Always ask"),
            skip("If non-exact: Skip"),
            proceedWithout("If non-exact: Proceed without")
        }
    }
}

@Singleton
class GameProviderServiceImpl @Inject constructor(
    private val providerRepository: GameProviderRepository,
    private val chooser: SearchChooser
) : GameProviderService {

    private val metaDataRegex = "(\\[.*?\\])".toRegex()

    override suspend fun search(taskData: GameProviderService.ProviderTaskData,
                                constraints: GameProviderService.SearchConstraints): List<ProviderData>? =
        try {
            SearchContext(taskData, constraints).search()
        } catch (e: CancelSearchException) {
            null
        }

    // Remove all metaData enclosed with '[]' from the file name and collapse all spaces into a single space.
    private fun String.normalizeName(): String = this.replace(metaDataRegex, "").collapseSpaces().replace(" - ", ": ").trim()

    private inner class SearchContext(
        private val taskData: GameProviderService.ProviderTaskData,
        private val constraints: GameProviderService.SearchConstraints
    ) {
        private var searchedName = taskData.name.normalizeName()
        private var canAutoContinue = searchMode != GameProviderService.SearchConstraints.SearchMode.alwaysAsk
        private val previouslyDiscardedResults: MutableSet<ProviderSearchResult> = mutableSetOf()
        private var userExactMatch: String? = null

        private val task get() = taskData.task
        private val platform get() = taskData.platform
        private val searchMode get() = constraints.mode

        private fun shouldSearch(provider: GameProvider): Boolean =
            constraints.onlySearch.isEmpty() || constraints.onlySearch.contains(provider.type)

        // TODO: Support a back button somehow, it's needed...
        suspend fun search(): List<ProviderData> {
            val results = providerRepository.providers.filter { shouldSearch(it) }.mapNotNull { search(it) }
            return download(taskData.copy(name = searchedName), results)
        }

        private suspend fun search(provider: GameProvider): ProviderHeader? {
            // TODO: Instead of writing the platform the provider, draw their logos.
            task.progress.message = "[$platform][${provider.type}] Searching '$searchedName'..."
            val results = provider.search(searchedName, platform)
            task.progress.message = "[$platform][${provider.type}] Searching '$searchedName': ${results.size} results."

            fun findExactMatch(target: String): ProviderSearchResult? = results.find { it.name.equals(target, ignoreCase = true) }

            val choice = if (searchMode == GameProviderService.SearchConstraints.SearchMode.alwaysAsk) {
                chooseResult(provider, results)
            } else if (userExactMatch != null) {
                val providerExactMatch = findExactMatch(userExactMatch!!)
                if (providerExactMatch != null) {
                    return providerExactMatch.toHeader(provider)
                } else {
                    chooseResult(provider, results)
                }
            } else {
                if (canAutoContinue && results.size == 1 && findExactMatch(searchedName) != null) {
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
            when (constraints.mode) {
                GameProviderService.SearchConstraints.SearchMode.skip -> return SearchChooser.Choice.Cancel
                GameProviderService.SearchConstraints.SearchMode.proceedWithout -> return SearchChooser.Choice.ProceedWithout
                GameProviderService.SearchConstraints.SearchMode.askIfNonExact,
                GameProviderService.SearchConstraints.SearchMode.alwaysAsk -> Unit // Proceed to ask chooser
            }

            val (filteredResults, results) = allSearchResults.partition { result ->
                previouslyDiscardedResults.any { it.name.equals(result.name, ignoreCase = true) }
            }

            val chooseSearchResultData = SearchChooser.Data(
                searchedName, taskData.path, provider.type, results = results, filteredResults = filteredResults
            )
            return chooser.choose(chooseSearchResultData)
        }

        private fun ProviderSearchResult.toHeader(provider: GameProvider) = ProviderHeader(
            type = provider.type,
            apiUrl = apiUrl,
            siteUrl = ""   // Unused for our purposes.
        )
    }

    override suspend fun download(taskData: GameProviderService.ProviderTaskData, headers: List<ProviderHeader>): List<ProviderData> {
        val task = taskData.task
        val name = taskData.name
        val platform = taskData.platform

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