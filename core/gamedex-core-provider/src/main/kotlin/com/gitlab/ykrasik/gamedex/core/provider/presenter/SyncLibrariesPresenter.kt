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

import com.gitlab.ykrasik.gamedex.LibraryPath
import com.gitlab.ykrasik.gamedex.app.api.provider.ViewCanSyncLibraries
import com.gitlab.ykrasik.gamedex.core.CommonData
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.library.SyncLibraryService
import com.gitlab.ykrasik.gamedex.core.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.core.provider.SyncGamesRequestedEvent
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.util.Try
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 30/12/2018
 * Time: 13:36
 */
@Singleton
class SyncLibrariesPresenter @Inject constructor(
    private val gameProviderService: GameProviderService,
    private val commonData: CommonData,
    private val syncLibraryService: SyncLibraryService,
    private val taskService: TaskService,
    private val eventBus: EventBus
) : Presenter<ViewCanSyncLibraries> {
    override fun present(view: ViewCanSyncLibraries) = object : ViewSession() {
        init {
            commonData.contentLibraries.itemsChannel
                .combineLatest(gameProviderService.enabledProviders.itemsChannel)
                .combineLatest(commonData.isGameSyncRunning)
                .forEach {
                    val (libraries, enabledProviders) = it.first
                    val isGameSyncRunning = it.second
                    val platformsWithEnabledProviders = gameProviderService.platformsWithEnabledProviders
                    view.canSyncLibraries *= Try {
                        check(!isGameSyncRunning) { "Game sync in progress!" }
                        check(libraries.isNotEmpty()) { "Please add at least 1 library!" }
                        check(enabledProviders.isNotEmpty()) { "Please enable at least 1 provider!" }
                        check(libraries.any { it.platform in platformsWithEnabledProviders }) { "Please enable a provider that supports your platform!" }
                    }
                }
            view.syncLibrariesActions.forEach { onSyncLibrariesStarted() }
        }

        private suspend fun onSyncLibrariesStarted() {
            view.canSyncLibraries.assert()

            val newPaths = taskService.execute(syncLibraryService.detectNewPaths())
            if (newPaths.isNotEmpty()) {
                startGameSync(newPaths)
            }
        }

        private fun startGameSync(paths: List<LibraryPath>) {
            eventBus.send(SyncGamesRequestedEvent(paths.map { it to null }, isAllowSmartChooseResults = true))
        }
    }
}