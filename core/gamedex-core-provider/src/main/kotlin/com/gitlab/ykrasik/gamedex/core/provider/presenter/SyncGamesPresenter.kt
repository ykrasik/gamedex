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
import com.gitlab.ykrasik.gamedex.core.CommonData
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.provider.GameSearchEvent
import com.gitlab.ykrasik.gamedex.core.provider.SyncGamesEvent
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import com.gitlab.ykrasik.gamedex.provider.id
import com.gitlab.ykrasik.gamedex.provider.supports
import com.gitlab.ykrasik.gamedex.util.findCircular
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
    private val commonData: CommonData,
    private val settingsService: SettingsService,
    private val eventBus: EventBus
) : Presenter<SyncGamesView> {
    private val log = logger()

    override fun present(view: SyncGamesView) = object : ViewSession() {
        init {
            // TODO: Consider launching a job here that can be cancelled.
            eventBus.forEach<SyncGamesEvent.Requested> { onSyncGamesStarted(it.paths, it.isAllowSmartChooseResults) }
            eventBus.forEach<GameSearchEvent.Updated> { onGameSearchStateUpdated(it.state) }
            view.currentState.forEach { onCurrentStateChanged(it) }
            view.restartStateActions.forEach { onRestart(it) }
            view.cancelActions.forEach { onCancel() }
        }

        private fun onSyncGamesStarted(paths: List<Pair<LibraryPath, Game?>>, isAllowSmartChooseResults: Boolean) {
            check(paths.isNotEmpty()) { "No games to sync!" }
            val platformsWithEnabledProviders = commonData.platformsWithEnabledProviders
            val pathsWithEnabledProviders = paths.filter { (path, _) -> platformsWithEnabledProviders.contains(path.library.platform) }

            start()
            view.isAllowSmartChooseResults *= isAllowSmartChooseResults
            view.numProcessed *= 0
            view.state.setAll(pathsWithEnabledProviders.mapIndexed { i, (path, game) -> initState(i, path, game) })
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
                if (search != null && state.game?.isProviderExcluded(providerId) != true) {
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

        private fun initState(index: Int, libraryPath: LibraryPath, game: Game?): GameSearchState {
            // TODO: Move this logic inside GameProviderService?
            val providersToSearch = commonData.enabledProviders
                .sortedBy { settingsService.providerOrder.search.indexOf(it.id) }
                .filter { provider -> provider.supports(libraryPath.library.platform) }
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
                index = index,
                libraryPath = libraryPath,
                providerOrder = providersToSearch,
                currentProvider = null,
                history = initialHistory,
                status = GameSearchStatus.Running,
                game = game
            )
        }

        private fun finishedMessage(): String {
            val numSuccessful = view.state.count { it.status == GameSearchStatus.Success }
            return "Successfully synced $numSuccessful / ${view.state.size} games."
        }
    }
}