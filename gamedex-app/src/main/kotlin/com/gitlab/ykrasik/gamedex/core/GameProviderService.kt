package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.ui.Task
import com.gitlab.ykrasik.gamedex.ui.view.game.search.ChooseSearchResultFragment
import com.gitlab.ykrasik.gamedex.util.collapseSpaces
import com.gitlab.ykrasik.gamedex.util.now
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
    suspend fun search(taskData: ProviderTaskData, excludedProviders: List<ProviderId>): SearchResults?

    suspend fun download(taskData: ProviderTaskData, headers: List<ProviderHeader>): List<ProviderData>

    data class ProviderTaskData(
        val task: Task<*>,
        val name: String,
        val platform: Platform,
        val path: File
    )

    data class SearchResults(
        val providerData: List<ProviderData>,
        val excludedProviders: List<ProviderId>
    ) {
        fun isEmpty(): Boolean = providerData.isEmpty() && excludedProviders.isEmpty()
    }
}

@Singleton
class GameProviderServiceImpl @Inject constructor(
    private val providerRepository: GameProviderRepository,
    private val settings: GameSettings,
    private val chooser: SearchChooser
) : GameProviderService {

    private val metaDataRegex = "(\\[.*?\\])".toRegex()

    override suspend fun search(taskData: GameProviderService.ProviderTaskData,
                                excludedProviders: List<ProviderId>): GameProviderService.SearchResults? =
        try {
            SearchContext(taskData, excludedProviders).search()
        } catch (e: CancelSearchException) {
            null
        }

    // Remove all metaData enclosed with '[]' from the file name and collapse all spaces into a single space.
    private fun String.normalizeName(): String = this.replace(metaDataRegex, "").collapseSpaces().replace(" - ", ": ").trim()

    private inner class SearchContext(
        private val taskData: GameProviderService.ProviderTaskData,
        private val excludedProviders: List<ProviderId>
    ) {
        private var searchedName = taskData.name.normalizeName()
        private var canAutoContinue = chooseResults != GameSettings.ChooseResults.alwaysChoose
        private val previouslyDiscardedResults = mutableSetOf<ProviderSearchResult>()
        private val newlyExcludedProviders = mutableListOf<ProviderId>()
        private var userExactMatch: String? = null

        private val task get() = taskData.task
        private val platform get() = taskData.platform
        private val chooseResults get() = settings.chooseResults

        init {
            task.platform = platform
        }

        // TODO: Support a back button somehow, it's needed...
        suspend fun search(): GameProviderService.SearchResults {
            val results = providerRepository.providers.filter { shouldSearch(it) }.mapNotNull { search(it) }
            val name = userExactMatch ?: searchedName
            val providerData = download(taskData.copy(name = name), results)
            return GameProviderService.SearchResults(providerData, newlyExcludedProviders)
        }

        private fun shouldSearch(provider: GameProvider): Boolean {
            return provider.supportedPlatforms.contains(platform) && !excludedProviders.contains(provider.id)
        }

        private suspend fun search(provider: GameProvider): ProviderHeader? {
            task.providerLogo = providerRepository.logo(provider.id)
            task.progress.message = "[$platform][${provider.id}] Searching '$searchedName'..."
            val results = provider.search(searchedName, platform)
            task.progress.message = "[$platform][${provider.id}] Searching '$searchedName': ${results.size} results."

            fun findExactMatch(target: String): ProviderSearchResult? = results.find { it.name.equals(target, ignoreCase = true) }

            val choice = if (chooseResults == GameSettings.ChooseResults.alwaysChoose) {
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
                is SearchChooser.Choice.ExcludeProvider -> {
                    newlyExcludedProviders += provider.id
                    null
                }
                SearchChooser.Choice.ProceedWithout -> null
                SearchChooser.Choice.Cancel -> throw CancelSearchException()
            }
        }

        private suspend fun chooseResult(provider: GameProvider, allSearchResults: List<ProviderSearchResult>): SearchChooser.Choice {
            // We only get here when we have no exact matches.
            when (chooseResults!!) {
                GameSettings.ChooseResults.skipIfNonExact -> return SearchChooser.Choice.Cancel
                GameSettings.ChooseResults.proceedWithoutIfNonExact -> return SearchChooser.Choice.ProceedWithout
                GameSettings.ChooseResults.chooseIfNonExact,
                GameSettings.ChooseResults.alwaysChoose -> Unit // Proceed to ask chooser
            }

            val (filteredResults, results) = allSearchResults.partition { result ->
                previouslyDiscardedResults.any { it.name.equals(result.name, ignoreCase = true) }
            }

            val chooseSearchResultData = SearchChooser.Data(
                searchedName, taskData.path, taskData.platform, provider.id, results = results, filteredResults = filteredResults
            )
            return chooser.choose(chooseSearchResultData)
        }

        private fun ProviderSearchResult.toHeader(provider: GameProvider) = ProviderHeader(provider.id, apiUrl, updateDate = now)
    }

    override suspend fun download(taskData: GameProviderService.ProviderTaskData, headers: List<ProviderHeader>): List<ProviderData> {
        val task = taskData.task
        val name = taskData.name
        val platform = taskData.platform

        task.platform = platform
        task.progress.message = "[$platform] Downloading '$name'..."
        return headers.map { header ->
            async(task.context) {
                if (task.result.isCancelled) throw CancellationException()
                providerRepository[header.id].download(header.apiUrl, platform)
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
        val platform: Platform,
        val providerId: ProviderId,
        val results: List<ProviderSearchResult>,
        val filteredResults: List<ProviderSearchResult>
    )

    sealed class Choice {
        data class ExactMatch(val result: ProviderSearchResult) : Choice()
        data class NotExactMatch(val result: ProviderSearchResult) : Choice()
        data class NewSearch(val newSearch: String) : Choice()
        data class ExcludeProvider(val provider: ProviderId) : Choice()
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