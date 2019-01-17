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

import com.gitlab.ykrasik.gamedex.app.api.provider.RedownloadGamesView
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.provider.RedownloadGameService
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/05/2018
 * Time: 13:08
 */
@Singleton
class RedownloadGamesPresenter @Inject constructor(
    private val settingsService: SettingsService,
    private val redownloadGameService: RedownloadGameService,
    private val taskService: TaskService,
    private val eventBus: EventBus
) : Presenter<RedownloadGamesView> {
    override fun present(view: RedownloadGamesView) = object : ViewSession() {
        init {
//            view.redownloadGamesCondition.forEach { setCanAccept() }
            view.redownloadGamesConditionIsValid.forEach { setCanAccept() }
            view.acceptActions.forEach { onAccept() }
            view.cancelActions.forEach { onCancel() }
        }

        override suspend fun onShow() {
            view.redownloadGamesCondition *= settingsService.providerGeneral.redownloadGamesCondition
            setCanAccept()
        }

        private fun setCanAccept() {
            view.canAccept *= view.redownloadGamesConditionIsValid.value
        }

        private suspend fun onAccept() {
            view.canAccept.assert()

            val condition = view.redownloadGamesCondition.value
            settingsService.providerGeneral.modify { copy(redownloadGamesCondition = condition) }

            finished()

            taskService.execute(redownloadGameService.redownloadGames(condition))
        }

        private fun onCancel() {
            finished()
        }

        private fun finished() = eventBus.viewFinished(view)
    }
}