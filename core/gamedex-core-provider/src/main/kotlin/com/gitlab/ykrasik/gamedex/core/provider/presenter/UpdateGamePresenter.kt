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

import com.gitlab.ykrasik.gamedex.app.api.provider.ViewCanUpdateGame
import com.gitlab.ykrasik.gamedex.core.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.core.provider.UpdateGameService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.core.view.Presenter
import com.gitlab.ykrasik.gamedex.core.view.ViewSession
import com.gitlab.ykrasik.gamedex.util.IsValid
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateGamePresenter @Inject constructor(
    private val updateGameService: UpdateGameService,
    private val gameProviderService: GameProviderService,
    private val taskService: TaskService,
) : Presenter<ViewCanUpdateGame> {
    override fun present(view: ViewCanUpdateGame) = object : ViewSession() {
        init {
            view::canUpdateGame *= view.game.onlyChangesFromView().combine(gameProviderService.enabledProviders.items) { game, enabledProviders ->
                IsValid {
                    check(enabledProviders.any { it.supports(game.platform) }) { "Enable at least 1 provider which supports the platform '${game.platform}'!" }
                }
            }

            view::updateGameActions.forEach {
                view.canUpdateGame.assert()
                taskService.execute(updateGameService.updateGame(it))
            }
        }
    }
}