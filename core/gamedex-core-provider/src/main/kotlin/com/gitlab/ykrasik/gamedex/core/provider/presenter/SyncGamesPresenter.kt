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

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.LibraryPath
import com.gitlab.ykrasik.gamedex.app.api.provider.*
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.provider.*
import com.gitlab.ykrasik.gamedex.provider.supports
import com.gitlab.ykrasik.gamedex.util.logger
import com.gitlab.ykrasik.gamedex.util.setAll
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 17/10/2018
 * Time: 09:26
 */
@Singleton
class SyncGamesPresenter @Inject constructor(
    private val gameProviderService: GameProviderService,
    private val eventBus: EventBus
) : Presenter<SyncGamesView> {
    private val log = logger()

    override fun present(view: SyncGamesView) = object : ViewSession() {
        init {
            // TODO: Consider launching a job here that can be cancelled.
            eventBus.forEach<SyncGamesRequestedEvent> { onSyncGamesStarted(it.paths, it.isAllowSmartChooseResults) }
            eventBus.forEach<GameSearchUpdatedEvent> { onGameSearchStateUpdated(it.state) }
            view.currentLibraryPath.forEach { onCurrentPathChanged(it!!) }
            view.restartLibraryPathActions.forEach { onRestart(it) }
            view.cancelActions.forEach { onCancel() }
        }

        private fun onSyncGamesStarted(paths: List<Pair<LibraryPath, Game?>>, isAllowSmartChooseResults: Boolean) {
            check(!paths.isEmpty()) { "No games to sync!" }
            val platformsWithEnabledProviders = gameProviderService.platformsWithEnabledProviders()
            val pathsWithEnabledProviders = paths.filter { (path, _) -> platformsWithEnabledProviders.contains(path.library.platform) }

            start()
            view.isAllowSmartChooseResults *= isAllowSmartChooseResults
            view.numProcessed *= 0
            view.pathsToProcess.setAll(pathsWithEnabledProviders.map { it.first })
            view.state.clear()
            view.state.putAll(pathsWithEnabledProviders.map { (path, game) -> path to initState(path, game) })
            view.currentLibraryPath *= null
            startGameSearch(paths.first().first)
        }

        private fun onGameSearchStateUpdated(newState: GameSearchState) {
            check(view.isGameSyncRunning.value || newState.isFinished) { "Received search update event when not syncing games" }
            val prevState = view.state.put(newState.libraryPath, newState)

            // This can be called for already finished states (for example, viewing results of previous completed searches).
            // Only do this when the state transitions to 'finished' for the first time.
            if (prevState?.isFinished != true && newState.isFinished) {
                view.numProcessed.value += 1
                val nextLibraryPath = findNextUnfinishedPath(from = newState.libraryPath)
                if (nextLibraryPath != null) {
                    startGameSearch(nextLibraryPath)
                } else {
                    if (view.pathsToProcess.size == 1 && newState.status == GameSearchStatus.Cancelled) {
                        onCancel()
                    } else {
                        finish(success = true)
                        val message = finishedMessage()
                        log.info("Done: $message")
                        if (view.pathsToProcess.size != 1) {
                            view.successMessage(message)
                        }
                    }
                }
            }
        }

        private fun findNextUnfinishedPath(from: LibraryPath): LibraryPath? {
            val fromIndex = view.pathsToProcess.indexOf(from)
            check(fromIndex != -1) { "Path not found: $from" }
            var iter = fromIndex
            do {
                iter = (iter + 1) % view.pathsToProcess.size
                val currentPath = view.pathsToProcess[iter]
                val currentState = view.state[currentPath]
                if (currentState == null || !currentState.isFinished) {
                    return currentPath
                }
            } while (iter != fromIndex)
            return null
        }

        private fun onCurrentPathChanged(currentLibraryPath: LibraryPath) {
            startGameSearch(currentLibraryPath)
        }

        private fun onRestart(libraryPath: LibraryPath) {
            check(view.isGameSyncRunning.value) { "Game sync not running!" }
            val currentState = view.state[libraryPath]!!
            val newState = currentState.providerOrder.fold(
                currentState.copy(
                    status = GameSearchStatus.Running,
                    currentProvider = currentState.providerOrder.first()
                )
            ) { state, providerId ->
                // Set the choice of all provider searches to null
                val search = state.lastSearchFor(providerId)
                if (search != null) {
                    state.modifyHistory(providerId) { this + search.copy(choice = null) }
                } else {
                    state
                }
            }
            view.state += libraryPath to newState
            view.numProcessed.value -= 1
            startGameSearch(libraryPath)
        }

        private fun onCancel() {
            finish(success = false)
            val message = finishedMessage()
            log.info("Cancelled: $message")
            if (view.pathsToProcess.size != 1) {
                view.cancelledMessage(message)
            }
        }

        private fun start() {
            check(!view.isGameSyncRunning.value) { "Game sync already running!" }
            view.isGameSyncRunning *= true
            eventBus.send(SyncGamesStartedEvent)
        }

        private fun finish(success: Boolean) {
            check(view.isGameSyncRunning.value) { "Game sync not running!" }
            view.isGameSyncRunning *= false

            // TODO: This is probably unnecessary.
            if (!success) {
                view.state.forEach { libraryPath, state ->
                    if (!state.isFinished) {
                        view.state += libraryPath to state.copy(status = GameSearchStatus.Cancelled)
                    }
                }
            }
            view.currentLibraryPath.value?.let { currentLibraryPath ->
                // This will update the current search that it's finished.
                startGameSearch(currentLibraryPath)
            }
            eventBus.send(SyncGamesFinishedEvent)
        }

        private fun startGameSearch(libraryPath: LibraryPath) {
            view.currentLibraryPath *= libraryPath
            val state = view.state[libraryPath]!!
            eventBus.send(GameSearchStartedEvent(state, view.isAllowSmartChooseResults.value))
        }

        private fun initState(libraryPath: LibraryPath, game: Game?): GameSearchState {
            // TODO: Move this logic inside GameProviderService?
            val providersToSearch = gameProviderService.enabledProviders
//                .sortedBy { settingsService.providerOrder.search.indexOf(it.id) }
                .filter { provider -> provider.supports(libraryPath.library.platform) }//&& !excludedProviders.contains(provider.id) }
                .map { it.id }

            val initialHistory = (game?.excludedProviders ?: emptyList()).map { providerId ->
                providerId to listOf(
                    GameSearch(
                        provider = providerId,
                        query = "",
                        results = emptyList(),
                        choice = ProviderSearchChoice.Exclude
                    )
                )
            }.toMap()

            return GameSearchState(
                libraryPath = libraryPath,
                providerOrder = providersToSearch,
                currentProvider = null,
                history = initialHistory,
                status = GameSearchStatus.Running,
                game = game
            )
        }

        private fun finishedMessage(): String {
            val numSuccessful = view.state.count { (_, state) -> state.status == GameSearchStatus.Success }
            return "Successfully synced $numSuccessful / ${view.pathsToProcess.size} games."
        }
    }
}