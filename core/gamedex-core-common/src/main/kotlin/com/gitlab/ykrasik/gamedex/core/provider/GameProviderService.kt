/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 * http://www.apache.org/licenses/LICENSE-2.0                               *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ****************************************************************************/

package com.gitlab.ykrasik.gamedex.core.provider

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.ProviderData
import com.gitlab.ykrasik.gamedex.ProviderHeader
import com.gitlab.ykrasik.gamedex.app.api.game.DiscoverGameChooseResults
import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.app.api.image.ImageFactory
import com.gitlab.ykrasik.gamedex.app.api.settings.Order
import com.gitlab.ykrasik.gamedex.app.api.util.ListObservable
import com.gitlab.ykrasik.gamedex.app.api.util.ListObservableImpl
import com.gitlab.ykrasik.gamedex.app.api.util.sortingWith
import com.gitlab.ykrasik.gamedex.core.file.FileSystemService
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import com.gitlab.ykrasik.gamedex.core.task.Task
import com.gitlab.ykrasik.gamedex.core.task.task
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.provider.ProviderUserAccount
import com.gitlab.ykrasik.gamedex.util.logger
import com.gitlab.ykrasik.gamedex.util.nowTimestamp
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 13/10/2016
 * Time: 13:29
 */
// TODO: Make it stream results back?
interface GameProviderService {
    val allProviders: List<GameProvider>
    val enabledProviders: ListObservable<EnabledGameProvider>

    fun checkAtLeastOneProviderEnabled()

    fun provider(id: ProviderId): GameProvider
    fun isEnabled(id: ProviderId): Boolean

    val logos: Map<ProviderId, Image>

    fun verifyAccount(providerId: ProviderId, account: Map<String, String>): Task<Boolean>

    // TODO: Split the methods here into GameDiscoveryService & GameDownloadService?
    fun search(name: String, platform: Platform, path: File, excludedProviders: List<ProviderId>): Task<SearchResults?>

    fun download(name: String, platform: Platform, headers: List<ProviderHeader>): Task<List<ProviderData>>
}

@Singleton
class GameProviderServiceImpl @Inject constructor(
    repo: GameProviderRepository,
    private val fileSystemService: FileSystemService,
    private val imageFactory: ImageFactory,
    private val settingsService: SettingsService,
    private val chooser: SearchChooser
) : GameProviderService {
    private val log = logger()

    override val allProviders = repo.allProviders
    private val unsortedProviders = ListObservableImpl<EnabledGameProvider>()
    override val enabledProviders = unsortedProviders
        .sortingWith(settingsService.providerOrder.searchChannel.map { it.toComparator() }.subscribe())

    init {
        log.info("Detected providers: $allProviders")

        allProviders.forEach { provider ->
            val providerSettingsRepo = settingsService.providers[provider.id]!!
            providerSettingsRepo.perform { data ->
                val enabledProvider = unsortedProviders.find { it.id == provider.id }
                when {
                    !data.enabled && enabledProvider != null ->
                        unsortedProviders -= enabledProvider

                    data.enabled && enabledProvider == null -> {
                        val account = provider.accountFeature?.createAccount(data.account)
                        unsortedProviders += EnabledGameProvider(provider, account)
                    }
                }
            }
        }

        log.info("Enabled providers: ${unsortedProviders.sortedBy { it.id }}")
    }

    private fun Order.toComparator(): Comparator<EnabledGameProvider> = Comparator { o1, o2 -> get(o1.id)!!.compareTo(get(o2.id)!!) }

    override fun provider(id: ProviderId) = allProviders.find { it.id == id }!!
    override fun isEnabled(id: ProviderId) = enabledProviders.any { it.id == id }

    override val logos = allProviders.map { it.id to imageFactory(it.logo) }.toMap()

    override fun checkAtLeastOneProviderEnabled() =
        check(enabledProviders.isNotEmpty()) {
            "No providers are enabled! Please make sure there's at least 1 enabled provider in the settings menu."
        }

    override fun verifyAccount(providerId: ProviderId, account: Map<String, String>): Task<Boolean> {
        val provider = allProviders.find { it.id == providerId }!!
        val accountFeature = checkNotNull(provider.accountFeature) { "Provider $providerId does not require an account!" }
        return task("Verifying $providerId account...", initialImage = logos[providerId]!!) {
            try {
                val providerAccount = accountFeature.createAccount(account)
                provider.search("TestSearchToVerifyAccount", Platform.pc, providerAccount)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    override fun search(name: String, platform: Platform, path: File, excludedProviders: List<ProviderId>) = task {
        try {
            SearchContext(this, name, platform, path, excludedProviders).search()
        } catch (e: CancelSearchException) {
            null
        }
    }

    private inner class SearchContext(
        private val task: Task<*>,
        name: String,
        private val platform: Platform,
        private val path: File,
        private val excludedProviders: List<ProviderId>
    ) {
        private var searchedName = fileSystemService.analyzeFolderName(name).gameName
        private var canAutoContinue = chooseResults != DiscoverGameChooseResults.alwaysChoose
        private val previouslyDiscardedResults = mutableSetOf<ProviderSearchResult>()
        private val newlyExcludedProviders = mutableListOf<ProviderId>()
        private var userExactMatch: String? = null

        private val chooseResults get() = settingsService.game.discoverGameChooseResults

        // TODO: Support a back button somehow, it's needed...
        suspend fun search(): SearchResults = task.run {
            val providersToSearch = enabledProviders.filter { shouldSearch(it) }
            val results = providersToSearch.mapNotNullWithProgress {
                search(it)
            }
            val name = userExactMatch ?: searchedName
            val providerData = executeSubTask(download(name, platform, results))
            return SearchResults(providerData, newlyExcludedProviders)
        }

        private fun shouldSearch(provider: GameProvider): Boolean =
            provider.supports(platform) && !excludedProviders.contains(provider.id)

        private suspend fun search(provider: EnabledGameProvider): ProviderHeader? {
            task.message = "[${provider.id}] Searching: '$searchedName'..."
            val results = provider.search(searchedName, platform)
            task.message = "[${provider.id}] Searching: '$searchedName'... {results.size} results."

            fun findExactMatch(target: String): ProviderSearchResult? = results.find { it.name.equals(target, ignoreCase = true) }

            val choice = when {
                chooseResults == DiscoverGameChooseResults.alwaysChoose -> chooseResult(provider, results)
                userExactMatch != null -> {
                    val providerExactMatch = findExactMatch(userExactMatch!!)
                    if (providerExactMatch != null) {
                        return providerExactMatch.toHeader(provider)
                    } else {
                        chooseResult(provider, results)
                    }
                }
                canAutoContinue && results.size == 1 && findExactMatch(searchedName) != null ->
                    return results.first().toHeader(provider)
                else -> chooseResult(provider, results)
            }

            fun ProviderSearchResult.markChosen(): ProviderHeader {
                previouslyDiscardedResults += results - this
                return this.toHeader(provider)
            }

            return when (choice) {
                is SearchChooser.Choice.ExactMatch -> choice.result.apply { userExactMatch = name; searchedName = name }.markChosen()
                is SearchChooser.Choice.NotExactMatch -> choice.result.markChosen()
                is SearchChooser.Choice.NewSearch -> {
                    searchedName = choice.newSearch.trim()
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
            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (chooseResults) {
                DiscoverGameChooseResults.skipIfNonExact -> return SearchChooser.Choice.Cancel
                DiscoverGameChooseResults.proceedWithoutIfNonExact -> return SearchChooser.Choice.ProceedWithout
            }

            val (filteredResults, results) = allSearchResults.partition { result ->
                previouslyDiscardedResults.any { it.name.equals(result.name, ignoreCase = true) }
            }

            val chooseSearchResultData = SearchChooser.Data(
                searchedName, path, platform, provider.id, results = results, filteredResults = filteredResults
            )
            return withContext(Dispatchers.Main) {
                chooser.choose(chooseSearchResultData)
            }
        }

        private fun ProviderSearchResult.toHeader(provider: GameProvider) = ProviderHeader(provider.id, apiUrl, timestamp = nowTimestamp)
    }

    override fun download(name: String, platform: Platform, headers: List<ProviderHeader>) = task("Downloading '$name'...") {
        totalItems = headers.size
        headers.map { header ->
            // TODO: Link to task scope.
            GlobalScope.async {
                val providerData = enabledProviders.find { it.id == header.id }!!.download(header.apiUrl, platform)
                incProgress()

                // FIXME: Maybe this logic belongs to the calling class.
                // Retain the original header's createDate.
                providerData.withCreateDate(header.createDate)
            }
        }.map { it.await() }
    }

    private class CancelSearchException : RuntimeException()
}

class EnabledGameProvider(private val provider: GameProvider, private val account: ProviderUserAccount?) : GameProvider by provider {
    fun search(name: String, platform: Platform): List<ProviderSearchResult> = provider.search(name, platform, account)
    fun download(apiUrl: String, platform: Platform): ProviderData = provider.download(apiUrl, platform, account)

    override fun toString() = provider.toString()
}

data class SearchResults(
    val providerData: List<ProviderData>,
    val excludedProviders: List<ProviderId>
) {
    fun isEmpty(): Boolean = providerData.isEmpty() && excludedProviders.isEmpty()
}

// FIXME: Get rid of this... use viewManager.
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