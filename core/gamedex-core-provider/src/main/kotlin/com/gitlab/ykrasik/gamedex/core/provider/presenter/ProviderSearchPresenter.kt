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

package com.gitlab.ykrasik.gamedex.core.provider.presenter

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.app.api.provider.*
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.file.FileSystemService
import com.gitlab.ykrasik.gamedex.core.game.AddGameRequest
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.core.provider.GameSearchStartedEvent
import com.gitlab.ykrasik.gamedex.core.provider.GameSearchUpdatedEvent
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.gitlab.ykrasik.gamedex.util.Modifier
import com.gitlab.ykrasik.gamedex.util.and
import com.gitlab.ykrasik.gamedex.util.setAll
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
    private val eventBus: EventBus
) : Presenter<ProviderSearchView> {
    override fun present(view: ProviderSearchView) = object : ViewSession() {
        private var state by view.state

        init {
            eventBus.forEach<GameSearchStartedEvent> { onGameSearchStarted(it.state, it.isAllowSmartChooseResults) }
            view.changeProviderActions.forEach { onChangeProvider(it) }
            view.choiceActions.forEach { onChoice(it, isUserAction = true) }
            view.query.forEach { onQueryChanged(it) }
            view.selectedSearchResult.forEach { onSelectedSearchResultChanged(it) }
            view.isFilterPreviouslyDiscardedResults.forEach { onIsFilterPreviouslyDiscardedResultsChanged(it) }

            view.canExcludeSearchResult *= IsValid.valid
        }

        private suspend fun onGameSearchStarted(state: GameSearchState, isAllowSmartChooseResults: Boolean) {
            this.state = state
            view.isAllowSmartChooseResults *= isAllowSmartChooseResults

            if (!state.isFinished) {
                view.canSmartChooseResult *= isAllowSmartChooseResults
                view.canChangeState *= IsValid.valid
                val initialProvider = state.currentProvider ?: state.nextPossibleProviderWithoutChoice ?: state.providerOrder.first()
                val query = state.lastSearchFor(initialProvider)?.query ?: fileSystemService.analyzeFolderName(state.libraryPath.path.name).gameName
                changeProviderAndQuery(initialProvider, query)
            } else {
                // We can also re-visit an already completed state.
                val disabled = IsValid.invalid("Already finished!")
                view.canSmartChooseResult *= false
                view.canChangeState *= disabled
                view.canSearchCurrentQuery *= disabled
                view.canAcceptSearchResult *= disabled
                view.canToggleFilterPreviouslyDiscardedResults *= disabled
                view.isFilterPreviouslyDiscardedResults *= false

                state.currentProvider?.let { currentProvider ->
                    onChangeProvider(currentProvider)
                }
            }
        }

        private suspend fun onChangeProvider(providerId: ProviderId) {
            updateProvider(providerId)

            val lastSearch = state.lastSearchFor(providerId)
            when {
                lastSearch != null -> displaySearch(lastSearch, isFilterPreviouslyDiscardedResults = true)
                !state.isFinished -> search(providerId, query = lastGoodQuery)
                else -> {
                    view.searchResults.clear()
                    view.query *= ""
                    view.selectedSearchResult *= null
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
                displaySearch(searchToAdd, isFilterPreviouslyDiscardedResults = true)
            } else {
                search(providerId, query)
            }
        }

        private suspend fun search(provider: ProviderId, query: String) {
            val results = taskService.execute(gameProviderService.search(provider, query, state.libraryPath.library.platform))
            val currentSearch = GameSearch(provider = provider, query = query, results = results, choice = null)
            modifyState { modifyHistory(provider) { this + currentSearch } }
            displaySearch(currentSearch, isFilterPreviouslyDiscardedResults = true)
        }

        private suspend fun onChoice(choice: ProviderSearchChoice, isUserAction: Boolean) {
            view.canChangeState.assert()

            var nextQuery = ""
            var shouldAdvanceToNextProvider = true
            var shouldCancel = false
            when (choice) {
                is ProviderSearchChoice.Accept -> {
                    if (isUserAction) view.canAcceptSearchResult.assert()
                    view.canSmartChooseResult *= view.isAllowSmartChooseResults.value
                    nextQuery = choice.result.name
                }
                is ProviderSearchChoice.NewSearch -> {
                    if (isUserAction) view.canSearchCurrentQuery.assert()
                    view.canSmartChooseResult *= false
                    nextQuery = choice.newQuery
                    shouldAdvanceToNextProvider = false
                }
                is ProviderSearchChoice.Skip -> {
                    nextQuery = lastGoodQuery
                }
                is ProviderSearchChoice.Exclude -> {
                    if (isUserAction) view.canExcludeSearchResult.assert()
                    nextQuery = lastGoodQuery
                }
                is ProviderSearchChoice.Cancel -> {
                    shouldCancel = true
                }
            }

            val currentSearch = state.currentSearch!!
            val newSearch = currentSearch.copy(choice = choice)
            modifyState {
                if (currentSearch.choice == null) {
                    // No result for this provider yet, we can safely assume last history entry belongs to this provider.
                    modifyCurrentSearch { newSearch }
                } else {
                    // There's already a result for this provider, which means this is an attempt to replace the existing result.
                    // Add a new history entry with the new choice.
                    modifyCurrentProviderHistory { this + newSearch }
                }
            }

            if (shouldCancel) {
                finishSearch(game = state.game, success = false)
                return
            }

            val nextProvider = if (shouldAdvanceToNextProvider) state.nextPossibleProviderWithoutChoice else state.currentProvider
            if (nextProvider != null) {
                changeProviderAndQuery(nextProvider, nextQuery)
            } else {
                onFinished()
            }
        }

        private suspend fun displaySearch(gameSearch: GameSearch, isFilterPreviouslyDiscardedResults: Boolean) {
            view.query *= gameSearch.query
            onQueryChanged(gameSearch.query)

            setSearchResults(gameSearch, isFilterPreviouslyDiscardedResults)
            view.canToggleFilterPreviouslyDiscardedResults *= IsValid {
                check(view.searchResults != gameSearch.results) { "No previous results to filter!" }
            }
            view.isFilterPreviouslyDiscardedResults *= isFilterPreviouslyDiscardedResults && view.canToggleFilterPreviouslyDiscardedResults.value.isSuccess

            view.selectedSearchResult *= (gameSearch.choice as? ProviderSearchChoice.Accept)?.result
            onSelectedSearchResultChanged(view.selectedSearchResult.value)

            if (!(view.canSmartChooseResult.value && gameSearch.choice == null)) return

            // Try to smart-choose a result

            // See if any of the current results is an exact match with a result the user chose before.
            val acceptedResult = firstAcceptedResult
            if (acceptedResult != null) {
                val providerExactMatch = gameSearch.results.find { result ->
                    result.name.equals(acceptedResult.name, ignoreCase = true) && result.releaseDate == acceptedResult.releaseDate
                }
                if (providerExactMatch != null) {
                    return onChoice(ProviderSearchChoice.Accept(providerExactMatch), isUserAction = false)
                }
            }

            // No accepted results yet, but maybe this result is an exact match with the search query.
            if (gameSearch.results.size == 1) {
                val result = gameSearch.results.first()
                if (result.name.equals(gameSearch.query, ignoreCase = true)) {
                    return onChoice(ProviderSearchChoice.Accept(result), isUserAction = false)
                }
            }
        }

        private fun onIsFilterPreviouslyDiscardedResultsChanged(isFilterPreviouslyDiscardedResults: Boolean) {
            view.canToggleFilterPreviouslyDiscardedResults.assert()
            val currentSearch = checkNotNull(state.currentSearch) { "Cannot toggle isFilterPreviouslyDiscardedResults without results!" }
            setSearchResults(currentSearch, isFilterPreviouslyDiscardedResults)
            view.selectedSearchResult *= (currentSearch.choice as? ProviderSearchChoice.Accept)?.result
        }

        private fun setSearchResults(result: GameSearch, isFilterPreviouslyDiscardedResults: Boolean) {
            val resultsToShow = if (isFilterPreviouslyDiscardedResults) {
                val resultsWithoutQuery = state.history.asSequence().flatMap { (_, results) ->
                    results.asSequence()
                        .filter { it.query != result.query }
                        .flatMap { it.results.asSequence() }
                }
                val acceptedResults = state.choicesOfType<ProviderSearchChoice.Accept>().map { (_, choice) -> choice.result }
                val previouslyDiscardedResults = (resultsWithoutQuery - acceptedResults).map { it.name to it.releaseDate }.toSet()
                result.results.filter { !previouslyDiscardedResults.contains(it.name to it.releaseDate) }
            } else {
                result.results
            }
            if (view.searchResults != resultsToShow) {
                view.searchResults.setAll(resultsToShow)
            }
        }

        private suspend fun onFinished() {
            val libraryPath = state.libraryPath
            val acceptedResults = state.choicesOfType<ProviderSearchChoice.Accept>().map { (providerId, choice) -> providerId to choice.result }
            val excludedProviders = state.choicesOfType<ProviderSearchChoice.Exclude>().map { (providerId, _) -> providerId }.toList()
            val name = acceptedResults.map { (_, result) -> result.name }.firstOrNull() ?: libraryPath.path.name
            val providerHeaders = acceptedResults.map { (providerId, result) -> ProviderHeader(providerId, result.apiUrl, timestamp = Timestamp.now) }.toList()
            val providerData = taskService.execute(gameProviderService.download(name, libraryPath.library.platform, providerHeaders))
            val task = state.game.let { existingGame ->
                if (existingGame == null) {
                    gameService.add(
                        AddGameRequest(
                            metadata = Metadata(
                                libraryId = libraryPath.library.id,
                                path = libraryPath.relativePath.toString(),
                                timestamp = Timestamp.now
                            ),
                            providerData = providerData,
                            userData = UserData(excludedProviders = excludedProviders)
                        )
                    )
                } else {
                    val rawGame = existingGame.rawGame.copy(
                        providerData = providerData,
                        userData = existingGame.userData.copy(excludedProviders = excludedProviders)
                    )
                    gameService.replace(existingGame, rawGame)
                }
            }

            val game = taskService.execute(task)
            finishSearch(game, success = true)
        }

        private fun finishSearch(game: Game?, success: Boolean) = modifyState {
            copy(game = game, status = if (success) GameSearchStatus.Success else GameSearchStatus.Cancelled)
        }

        private fun onQueryChanged(query: String) {
            view.canSearchCurrentQuery *= view.canChangeState.value.and(IsValid {
                check(query != state.currentSearch?.query) { "Same query!" }
            })
        }

        private fun onSelectedSearchResultChanged(selectedResult: ProviderSearchResult?) {
            view.canAcceptSearchResult *= view.canChangeState.value.and(IsValid {
                check(selectedResult != null) { "No result selected!" }
            })
        }

        private fun updateProvider(provider: ProviderId) {
            if (state.currentProvider != provider) {
                modifyState { copy(currentProvider = provider) }
            }
        }

        private inline fun modifyState(crossinline f: Modifier<GameSearchState>) {
            state = f(state)
            eventBus.send(GameSearchUpdatedEvent(state))
        }

        private val firstAcceptedResult: ProviderSearchResult?
            get() = state.choicesOfType<ProviderSearchChoice.Accept>()
                .map { (_, choice) -> choice.result }
                .firstOrNull()

        private val lastGoodQuery: String
            get() = firstAcceptedResult?.name ?: state.history.asSequence().map { (_, results) -> results.last().query }.first()

        private val GameSearchState.nextPossibleProviderWithoutChoice: ProviderId?
            get() = providerOrder.firstOrNull {
                lastSearchFor(it)?.choice?.isResult != true
            }
    }
}