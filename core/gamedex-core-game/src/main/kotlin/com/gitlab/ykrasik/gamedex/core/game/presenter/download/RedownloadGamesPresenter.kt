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

package com.gitlab.ykrasik.gamedex.core.game.presenter.download

import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.app.api.game.RedownloadGamesView
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.filter.FilterContextFactory
import com.gitlab.ykrasik.gamedex.core.game.GameDownloadService
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.util.IsValid
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
    private val filterContextFactory: FilterContextFactory,
    private val gameService: GameService,
    private val gameDownloadService: GameDownloadService,
    private val taskService: TaskService,
    private val eventBus: EventBus
) : Presenter<RedownloadGamesView> {
    override fun present(view: RedownloadGamesView) = object : ViewSession() {
        init {
            view.redownloadGamesCondition.forEach { setCanAccept() }
            view.redownloadGamesConditionIsValid.forEach { setCanAccept() }
            view.acceptActions.forEach { onAccept() }
            view.cancelActions.forEach { onCancel() }
        }

        override fun onShow() {
            view.redownloadGamesCondition *= settingsService.game.redownloadGamesCondition
            setCanAccept()
        }

        private fun setCanAccept() {
            view.canAccept *= IsValid {
                check(view.redownloadGamesCondition.value !is Filter.True) { "Please select a condition!" }
                view.redownloadGamesConditionIsValid.value.get()
            }
        }

        private suspend fun onAccept() {
            check(view.canAccept.value.isSuccess) { "Accepting not allowed right now!" }
            finished()

            val condition = view.redownloadGamesCondition.value
            settingsService.game.modify { copy(redownloadGamesCondition = view.redownloadGamesCondition.value) }

            val context = filterContextFactory.create(emptyList())
            val games = gameService.games.filter { condition.evaluate(it, context) }.sortedBy { it.name }
            taskService.execute(gameDownloadService.redownloadGames(games.sortedBy { it.name }))
        }

        private fun onCancel() {
            finished()
        }

        private fun finished() = eventBus.viewFinished(view)
    }
}