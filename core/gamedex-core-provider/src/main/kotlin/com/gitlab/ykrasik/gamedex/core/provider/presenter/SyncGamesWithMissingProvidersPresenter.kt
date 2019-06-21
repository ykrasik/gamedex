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

import com.gitlab.ykrasik.gamedex.app.api.provider.SyncGamesWithMissingProvidersView
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.provider.BulkSyncGamesFilterRepository
import com.gitlab.ykrasik.gamedex.core.provider.SyncGameService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.util.IsValid
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/05/2018
 * Time: 09:51
 */
@Singleton
class SyncGamesWithMissingProvidersPresenter @Inject constructor(
    private val repo: BulkSyncGamesFilterRepository,
    private val syncGameService: SyncGameService,
    private val taskService: TaskService,
    private val eventBus: EventBus
) : Presenter<SyncGamesWithMissingProvidersView> {
    override fun present(view: SyncGamesWithMissingProvidersView) = object : ViewSession() {
        init {
//            view.bulkSyncGamesFilter.forEach { setCanAccept() }
            view.bulkSyncGamesFilterIsValid.forEach { setCanAccept() }
            view.canAccept *= IsValid.valid
            view.syncOnlyMissingProviders *= true
            view.acceptActions.forEach { onAccept() }
            view.cancelActions.forEach { onCancel() }
        }

        override suspend fun onShown() {
            view.bulkSyncGamesFilter *= repo.bulkSyncGamesFilter.peek()
            setCanAccept()
        }

        private fun setCanAccept() {
            view.canAccept *= view.bulkSyncGamesFilterIsValid.value
        }

        private suspend fun onAccept() {
            view.canAccept.assert()

            val filter = view.bulkSyncGamesFilter.value
            repo.update(filter)

            hideView()

            val requests = taskService.execute(syncGameService.detectGamesWithMissingProviders(filter, view.syncOnlyMissingProviders.value))
            syncGameService.syncGames(requests, isAllowSmartChooseResults = false)
        }

        private fun onCancel() {
            hideView()
        }

        private fun hideView() = eventBus.requestHideView(view)
    }
}