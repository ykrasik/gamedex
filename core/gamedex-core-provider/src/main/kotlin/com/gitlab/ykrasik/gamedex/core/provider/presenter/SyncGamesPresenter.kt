/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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

import com.gitlab.ykrasik.gamedex.app.api.provider.*
import com.gitlab.ykrasik.gamedex.core.CommonData
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.provider.GameSearchEvent
import com.gitlab.ykrasik.gamedex.core.provider.SyncGamesEvent
import com.gitlab.ykrasik.gamedex.core.provider.SyncPathRequest
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.provider.id
import com.gitlab.ykrasik.gamedex.provider.supports
import com.gitlab.ykrasik.gamedex.util.findCircular
import com.gitlab.ykrasik.gamedex.util.logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 17/10/2018
 * Time: 09:26
 */
@Singleton
class SyncGamesPresenter @Inject constructor(
    private val commonData: CommonData,
    private val settingsService: SettingsService,
    private val eventBus: EventBus
) : Presenter<SyncGamesView> {
    private val log = logger()

    override fun present(view: SyncGamesView) = object : ViewSession() {
        init {
            // TODO: Consider launching a job here that can be cancelled.
            eventBus.forEach<SyncGamesEvent.RequestSync> { onSyncGamesRequested(it.requests, it.isAllowSmartChooseResults) }
            eventBus.forEach<GameSearchEvent.Updated> { onGameSearchStateUpdated(it.state) }
            view.currentState.forEach { onCurrentStateChanged(it) }
            view.restartStateActions.forEach { onRestart(it) }
            view.cancelActions.forEach { onCancel() }
        }

        private fun onSyncGamesRequested(requests: List<SyncPathRequest>, isAllowSmartChooseResults: Boolean) {
            check(requests.isNotEmpty()) { "No games to sync!" }
            val state = requests.asSequence()
                .mapNotNull(::initState)
                .mapIndexed { i, state -> state.copy(index = i) }
                .toList()
            if (state.isEmpty()) {
                val message = if (requests.size == 1) {
                    "Error syncing game: No providers enabled for platform '${requests.first().platform}'!"
                } else {
                    "Error syncing ${requests.size} games: No providers enabled that support their platform!"
                }
                view.cancelledMessage(message)
                return
            }

            start()
            view.isAllowSmartChooseResults *= isAllowSmartChooseResults
            view.numProcessed *= 0
            view.state.setAll(state)
            view.currentState *= null
            startGameSearch(view.state.first())
        }

        private fun onGameSearchStateUpdated(newState: GameSearchState) {
            check(view.isGameSyncRunning.value || newState.isFinished) { "Received search update event when not syncing games" }
            val prevState = view.state[newState.index]
            view.state[newState.index] = newState

            // This can be called for already finished states (for example, viewing results of previous completed searches).
            // Only do this when the state transitions to 'finished' for the first time.
            if (prevState.isFinished || !newState.isFinished) return

            view.numProcessed.value += 1
            val nextUnfinishedState = view.state.findCircular(startIndex = newState.index) { !it.isFinished }
            if (nextUnfinishedState != null) {
                startGameSearch(nextUnfinishedState)
            } else {
                if (view.state.size == 1 && newState.status == GameSearchStatus.Cancelled) {
                    onCancel()
                } else {
                    finish(success = true)
                    val message = finishedMessage()
                    log.info("Done: $message")
                    if (view.state.size != 1) {
                        view.successMessage(message)
                    }
                }
            }
        }

        private fun onCurrentStateChanged(currentState: GameSearchState?) {
            if (currentState != null) {
                startGameSearch(currentState)
            }
        }

        private fun onRestart(state: GameSearchState) {
            check(view.isGameSyncRunning.value) { "Game sync not running!" }
            val newState = state.providerOrder.fold(
                state.copy(
                    status = GameSearchStatus.Running,
                    currentProvider = null
                )
            ) { accState, providerId ->
                // Set the choice of all provider searches to null
                val search = accState.lastSearchFor(providerId)
                if (search != null && state.existingGame?.isProviderExcluded(providerId) != true) {
                    accState.modifyHistory(providerId) { this + search.copy(choice = null) }
                } else {
                    accState
                }
            }
            view.state[state.index] = newState
            view.numProcessed.value -= 1
            startGameSearch(newState)
        }

        private fun onCancel() {
            finish(success = false)
            val message = finishedMessage()
            log.info("Cancelled: $message")
            if (view.state.size != 1) {
                view.cancelledMessage(message)
            }
        }

        private fun start() {
            check(!view.isGameSyncRunning.value) { "Game sync already running!" }
            view.isGameSyncRunning *= true
            eventBus.send(SyncGamesEvent.Started)
        }

        private fun finish(success: Boolean) {
            check(view.isGameSyncRunning.value) { "Game sync not running!" }
            view.isGameSyncRunning *= false

            view.currentState.value?.let { currentState ->
                // This will update the current search that it's finished.
                startGameSearch(currentState.copy(status = if (success) GameSearchStatus.Success else GameSearchStatus.Cancelled))
            }
            eventBus.send(SyncGamesEvent.Finished)
        }

        private fun startGameSearch(state: GameSearchState) {
            view.currentState *= state
            eventBus.send(GameSearchEvent.Started(state, view.isAllowSmartChooseResults.value))
        }

        private fun initState(request: SyncPathRequest): GameSearchState? {
            val providersToSync = commonData.enabledProviders.asSequence()
                .filter { it.supports(request.platform) }
                .map { it.id }
                .toList()
                .sortedBy { settingsService.providerOrder.search.indexOf(it) }

            if (providersToSync.isEmpty()) {
                log.debug("Skipping ${request.libraryPath}, game=${request.existingGame} because no enabled providers support the platform '${request.platform}'.")
                return null
            }

            val initialHistory = mutableMapOf<ProviderId, List<GameSearch>>()
            request.existingGame?.let { existingGame ->
                if (request.syncOnlyTheseProviders.isNotEmpty()) {
                    providersToSync.forEach { providerId ->
                        if (providerId !in request.syncOnlyTheseProviders) {
                            // This provider is not in the list of exclusive providers we want to sync.
                            // Set the initial history to contain the existing game data for this provider.
                            val providerGameData = existingGame.rawGame.providerData.find { it.header.providerId == providerId } ?: return@forEach
                            val syntheticSearchResult = ProviderSearchResult(
                                providerGameId = providerGameData.header.providerGameId,
                                name = providerGameData.gameData.name,
                                description = providerGameData.gameData.description,
                                releaseDate = providerGameData.gameData.releaseDate,
                                criticScore = providerGameData.gameData.criticScore,
                                userScore = providerGameData.gameData.userScore,
                                thumbnailUrl = providerGameData.gameData.thumbnailUrl
                            )
                            initialHistory[providerId] = listOf(
                                GameSearch(
                                    provider = providerId,
                                    query = "",
                                    results = listOf(syntheticSearchResult),
                                    choice = ProviderSearchChoice.Skip
                                )
                            )
                        }
                    }
                }

                existingGame.excludedProviders.forEach { providerId ->
                    initialHistory[providerId] = listOf(
                        GameSearch(
                            provider = providerId,
                            query = "",
                            results = emptyList(),
                            choice = ProviderSearchChoice.Exclude
                        )
                    )
                }
            }

            if (providersToSync.all { initialHistory.containsKey(it) }) {
                // We already have a choice for each of the providers to sync.
                // This choice can be that the provider is excluded or that it already has a result.
                // In either case, there are no providers left to sync for this game
                log.debug("Skipping ${request.libraryPath}, game=${request.existingGame} because all providers have an initial state: " +
                    providersToSync.map { it to initialHistory.getValue(it).first().choice })
                return null
            }

            return GameSearchState(
                index = -1,
                libraryPath = request.libraryPath,
                providerOrder = providersToSync,
                currentProvider = null,
                history = initialHistory,
                status = GameSearchStatus.Running,
                existingGame = request.existingGame
            )
        }

        private fun finishedMessage(): String {
            val numSuccessful = view.state.count { it.status == GameSearchStatus.Success }
            return "Successfully synced $numSuccessful / ${view.state.size} games."
        }
    }
}