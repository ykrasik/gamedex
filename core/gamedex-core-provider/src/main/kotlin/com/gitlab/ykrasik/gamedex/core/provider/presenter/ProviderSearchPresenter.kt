/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.core.provider.presenter

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.app.api.provider.GameSearchState
import com.gitlab.ykrasik.gamedex.app.api.provider.ProviderSearchView
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.file.FileSystemService
import com.gitlab.ykrasik.gamedex.core.flowOf
import com.gitlab.ykrasik.gamedex.core.game.AddGameRequest
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.core.provider.GameSearchEvent
import com.gitlab.ykrasik.gamedex.core.settings.GeneralSettingsRepository
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.core.view.Presenter
import com.gitlab.ykrasik.gamedex.core.view.ViewSession
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.*
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 12/10/2018
 * Time: 21:53
 */
@Singleton
class ProviderSearchPresenter @Inject constructor(
    private val gameProviderService: GameProviderService,
    private val fileSystemService: FileSystemService,
    private val gameService: GameService,
    private val taskService: TaskService,
    private val settingsRepo: GeneralSettingsRepository,
    private val eventBus: EventBus,
) : Presenter<ProviderSearchView> {
    override fun present(view: ProviderSearchView) = object : ViewSession() {
        private var state by view.state

        init {
            eventBus.flowOf<GameSearchEvent.Started>().forEach(debugName = "onGameSearchStarted") {
                onGameSearchStarted(it.state, it.isAllowSmartChooseResults)
            }

            view::changeProviderActions.forEach { onChangeProvider(it) }
            view::choiceActions.forEach { onChoice(it, isUserAction = true) }
            view::canSearchCurrentQuery *= view.canChangeState.combine(view.query.allValues()) { canChangeState, query ->
                canChangeState and IsValid {
                    check(query != state.currentProviderSearch?.query) { "Same query!" }
                }
            }
            view::canAcceptSearchResult *= view.canChangeState.combine(view.selectedSearchResult.allValues()) { canChangeState, selectedResult ->
                canChangeState and IsValid {
                    check(selectedResult != null) { "No result selected!" }
                }
            }
            view::fetchSearchResultActions.forEach { onFetchSearchResult(it) }
            view.isFilterPreviouslyDiscardedResults.onlyChangesFromView().forEach(debugName = "onFilterPreviouslyDiscardedResultsChanged") {
                onIsFilterPreviouslyDiscardedResultsChanged(it)
            }
            view::showMoreResultsActions.forEach { onShowMoreResults() }
        }

        private suspend fun onGameSearchStarted(state: GameSearchState, isAllowSmartChooseResults: Boolean) {
            this.state = state
            view.isAllowSmartChooseResults /= isAllowSmartChooseResults

            if (!state.isFinished) {
                view.canSmartChooseResult /= isAllowSmartChooseResults
                view.canChangeState /= IsValid.valid
                val initialProvider = state.currentProvider ?: state.nextPossibleProviderWithoutChoice ?: state.providerOrder.first()
                val query = state.lastSearchFor(initialProvider)?.query ?: fileSystemService.analyzeFolderName(state.libraryPath.path.name).processedName
                changeProviderAndQuery(initialProvider, query)
            } else {
                // We can also re-visit an already completed state.
                val disabled = IsValid.invalid("Already finished!")
                view.canSmartChooseResult /= false
                view.canChangeState /= disabled
                view.canSearchCurrentQuery /= disabled
                view.canAcceptSearchResult /= disabled
                view.canToggleFilterPreviouslyDiscardedResults /= disabled
                view.isFilterPreviouslyDiscardedResults /= false

                state.currentProvider?.let { currentProvider ->
                    onChangeProvider(currentProvider)
                }
            }
        }

        private suspend fun onChangeProvider(providerId: ProviderId) {
            updateProvider(providerId)

            val lastSearch = state.lastSearchFor(providerId)
            when {
                lastSearch != null -> displaySearch(lastSearch)
                !state.isFinished -> search(providerId, query = lastGoodQuery, offset = 0)
                else -> {
                    view.searchResults /= emptyList()
                    view.query /= ""
                    view.selectedSearchResult.valueFromPresenter = null
                }
            }
        }

        private suspend fun changeProviderAndQuery(providerId: ProviderId, query: String) {
            updateProvider(providerId)

            val previousSearch = state.historyFor(providerId).findLast { it.query == query }
            if (previousSearch != null) {
                // We have results for this search in the history. Re-add this search as the latest in the history and display it.
                val searchToAdd = previousSearch.copy(choice = null)
                modifyState { modifyHistory(providerId) { this + searchToAdd } }
                displaySearch(searchToAdd)
            } else {
                search(providerId, query, offset = 0)
            }
        }

        private suspend fun search(provider: ProviderId, query: String, offset: Int) {
            val limit = settingsRepo.searchResultLimit.value
            val response = taskService.execute(
                gameProviderService.search(
                    providerId = provider,
                    query = query,
                    platform = state.libraryPath.library.platform,
                    offset = offset,
                    limit = limit
                )
            )
            val results = response.results
            val canShowMoreResults = response.canShowMoreResults ?: results.size == limit
            val currentSearch = if (offset == 0) {
                GameSearchState.ProviderSearch(
                    provider = provider,
                    query = query,
                    offset = offset,
                    results = results,
                    canShowMoreResults = canShowMoreResults,
                    choice = null
                )
            } else {
                state.currentProviderSearch!!.let {
                    it.copy(results = it.results + results, offset = offset, canShowMoreResults = canShowMoreResults)
                }
            }
            modifyState {
                modifyHistory(provider) {
                    if (offset == 0) {
                        this + currentSearch
                    } else {
                        this.replaceAtIndex(size - 1, currentSearch)
                    }
                }
            }
            displaySearch(currentSearch)
        }

        private suspend fun onChoice(choice: GameSearchState.ProviderSearch.Choice, isUserAction: Boolean) {
            view.canChangeState.assert()

            var nextQuery = ""
            var shouldAdvanceToNextProvider = true
            var shouldCancel = false
            when (choice) {
                is GameSearchState.ProviderSearch.Choice.Accept -> {
                    if (isUserAction) view.canAcceptSearchResult.assert()
                    view.canSmartChooseResult /= view.isAllowSmartChooseResults.value
                    nextQuery = choice.result.name
                }
                is GameSearchState.ProviderSearch.Choice.NewSearch -> {
                    if (isUserAction) view.canSearchCurrentQuery.assert()
                    view.canSmartChooseResult /= false
                    nextQuery = choice.newQuery
                    shouldAdvanceToNextProvider = false
                }
                is GameSearchState.ProviderSearch.Choice.Skip -> {
                    nextQuery = lastGoodQuery
                }
                is GameSearchState.ProviderSearch.Choice.Exclude -> {
                    if (isUserAction) view.canExcludeSearchResult.assert()
                    nextQuery = lastGoodQuery
                }
                is GameSearchState.ProviderSearch.Choice.Cancel -> {
                    shouldCancel = true
                }
                else -> error("Invalid choice: $choice")
            }

            modifyState {
                val currentSearch = state.currentProviderSearch!!
                val newSearch = currentSearch.copy(choice = choice)
                val stateWithChoice = if (currentSearch.choice == null) {
                    // No result for this provider yet, add the choice to the provider's last search.
                    modifyCurrentProviderSearch { newSearch }
                } else {
                    // There's already a result for this provider, which means this is an attempt to replace the existing result.
                    // Add a new history entry with the new choice.
                    modifyCurrentProviderHistory { this + newSearch }
                }
                if (shouldCancel) {
                    stateWithChoice.copy(status = GameSearchState.Status.Cancelled)
                } else {
                    stateWithChoice
                }
            }

            if (shouldCancel) return

            val nextProvider = if (shouldAdvanceToNextProvider) state.nextPossibleProviderWithoutChoice else state.currentProvider
            if (nextProvider != null) {
                changeProviderAndQuery(nextProvider, nextQuery)
            } else {
                onFinished()
            }
        }

        private suspend fun displaySearch(search: GameSearchState.ProviderSearch) {
            view.query /= search.query

            setSearchResults(search, isFilterPreviouslyDiscardedResults = true)
            view.canToggleFilterPreviouslyDiscardedResults /= IsValid {
                check(view.searchResults.value != search.results) { "No previous results to filter!" }
            }
            view.isFilterPreviouslyDiscardedResults /= view.canToggleFilterPreviouslyDiscardedResults.value.isSuccess

            view.selectedSearchResult /= (search.choice as? GameSearchState.ProviderSearch.Choice.Accept)?.result

            view.canShowMoreResults /= IsValid {
                check(search.canShowMoreResults) { "More results not available!" }
            }

            if (!(view.canSmartChooseResult.value && search.choice == null)) return

            // Try to smart-choose a result

            // See if any of the current results is an exact match with a result the user chose before.
            val acceptedResult = firstAcceptedResult
            if (acceptedResult != null) {
                val providerExactMatch = search.results.find { result ->
                    result.name.equals(acceptedResult.name, ignoreCase = true) && result.releaseDate == acceptedResult.releaseDate
                }
                if (providerExactMatch != null) {
                    return onChoice(GameSearchState.ProviderSearch.Choice.Accept(providerExactMatch), isUserAction = false)
                }
            }

            // No accepted results yet, but maybe this result is an exact match with the search query.
            if (search.results.size == 1) {
                val result = search.results.first()
                if (result.name.equals(search.query, ignoreCase = true)) {
                    return onChoice(GameSearchState.ProviderSearch.Choice.Accept(result), isUserAction = false)
                }
            }
        }

        private fun onIsFilterPreviouslyDiscardedResultsChanged(isFilterPreviouslyDiscardedResults: Boolean) {
            view.canToggleFilterPreviouslyDiscardedResults.assert()
            val currentSearch = checkNotNull(state.currentProviderSearch) { "Cannot toggle isFilterPreviouslyDiscardedResults without results!" }
            setSearchResults(currentSearch, isFilterPreviouslyDiscardedResults)
            view.selectedSearchResult /= (currentSearch.choice as? GameSearchState.ProviderSearch.Choice.Accept)?.result
        }

        private fun setSearchResults(result: GameSearchState.ProviderSearch, isFilterPreviouslyDiscardedResults: Boolean) {
            val resultsToShow = if (isFilterPreviouslyDiscardedResults) {
                val resultsWithoutQuery = state.history.asSequence().flatMap { (_, results) ->
                    results.asSequence()
                        .filter { it.query != result.query && it.choice != null }
                        .flatMap { it.results.asSequence() }
                }
                val acceptedResults = state.choicesOfType<GameSearchState.ProviderSearch.Choice.Accept>().map { (_, choice) -> choice.result }
                val presetResults = state.choicesOfType<GameSearchState.ProviderSearch.Choice.Preset>().map { (_, choice) -> choice.result }
                val previouslyDiscardedResults = (resultsWithoutQuery - acceptedResults - presetResults).map { it.name to it.releaseDate }.toSet()
                result.results.filter { (it.name to it.releaseDate) !in previouslyDiscardedResults }
            } else {
                result.results
            }
            if (view.searchResults != resultsToShow) {
                view.searchResults /= resultsToShow
            }
        }

        private suspend fun onShowMoreResults() {
            view.canShowMoreResults.assert()
            val currentSearch = state.currentProviderSearch!!
            search(currentSearch.provider, currentSearch.query, currentSearch.offset + settingsRepo.searchResultLimit.value)
        }

        private suspend fun onFinished() {
            val libraryPath = state.libraryPath
            val existingGame = state.existingGame

            // Init providerData with the existing game data
            val providerData = mutableMapOf<ProviderId, ProviderData>()
            existingGame?.providerData?.associateByTo(providerData) { it.providerId }

            // Copy preset results to providerData, overwriting any existing data.
            state.choicesOfType<GameSearchState.ProviderSearch.Choice.Preset>().associateByTo(providerData, { it.first }) { it.second.data }

            val acceptedResults = state.choicesOfType<GameSearchState.ProviderSearch.Choice.Accept>().map { (providerId, choice) -> providerId to choice.result }
            val excludedProviders = state.choicesOfType<GameSearchState.ProviderSearch.Choice.Exclude>().map { (providerId, _) -> providerId }.toList()

            val providerHeadersToFetch = acceptedResults
                .map { (providerId, result) -> ProviderHeader(providerId, result.providerGameId) }
                .toList()

            val name = providerData.values.firstOrNull()?.gameData?.name
                ?: acceptedResults.firstOrNull()?.second?.name
                ?: libraryPath.path.name

            // Fetch providerData from accepted search results
            val fetchedProviderData = if (providerHeadersToFetch.isNotEmpty()) {
                taskService.execute(gameProviderService.fetch(name, libraryPath.library.platform, providerHeadersToFetch))
            } else {
                emptyList()
            }
            fetchedProviderData.associateByTo(providerData) { it.providerId }

            val finalProviderData = providerData.values.sortedBy { it.providerId }

            val task = if (existingGame == null) {
                gameService.add(
                    AddGameRequest(
                        metadata = Metadata(
                            libraryId = libraryPath.library.id,
                            path = libraryPath.relativePath.toString(),
                            timestamp = Timestamp.now
                        ),
                        providerData = finalProviderData,
                        userData = UserData(excludedProviders = excludedProviders)
                    )
                )
            } else {
                val rawGame = existingGame.rawGame.copy(
                    providerData = finalProviderData,
                    userData = existingGame.userData.copy(excludedProviders = excludedProviders)
                )
                gameService.replace(existingGame, rawGame)
            }
            val game = taskService.execute(task)
            modifyState { copy(existingGame = game, status = GameSearchState.Status.Success) }
        }

        private suspend fun onFetchSearchResult(searchResult: GameProvider.SearchResult) {
            val fetchResult = taskService.execute(
                gameProviderService.fetch(
                    name = searchResult.name,
                    platform = state.libraryPath.library.platform,
                    headers = listOf(
                        ProviderHeader(
                            providerId = state.currentProvider!!,
                            providerGameId = searchResult.providerGameId
                        )
                    )
                )
            ).first()
            val updatedSearchResult = searchResult.copy(
                description = fetchResult.gameData.description,
                releaseDate = fetchResult.gameData.releaseDate,
                criticScore = fetchResult.gameData.criticScore,
                userScore = fetchResult.gameData.userScore,
                thumbnailUrl = fetchResult.gameData.thumbnailUrl ?: fetchResult.gameData.posterUrl ?: fetchResult.gameData.screenshotUrls.firstOrNull()
            )
            view.searchResults /= view.searchResults.value.replace(searchResult, updatedSearchResult)
            modifyState {
                modifyCurrentProviderSearch {
                    copy(results = view.searchResults.value)
                }
            }
        }

        private suspend fun updateProvider(provider: ProviderId) {
            if (state.currentProvider != provider) {
                modifyState { copy(currentProvider = provider) }
            }
        }

        private suspend inline fun modifyState(crossinline f: Modifier<GameSearchState>) {
            state = f(state)
            eventBus.emit(GameSearchEvent.Updated(state))
        }

        private val firstAcceptedResult: GameProvider.SearchResult?
            get() = state.choicesOfType<GameSearchState.ProviderSearch.Choice.Accept>()
                .map { (_, choice) -> choice.result }
                .firstOrNull()

        private val lastGoodQuery: String
            get() = firstAcceptedResult?.name
                ?: state.history.asSequence().mapNotNull { (_, results) -> results.last().query.takeIf { it.isNotBlank() } }.firstOrNull()
                ?: ""

        private val GameSearchState.nextPossibleProviderWithoutChoice: ProviderId?
            get() = providerOrder.firstOrNull {
                lastSearchFor(it)?.choice?.isResult != true
            }
    }
}