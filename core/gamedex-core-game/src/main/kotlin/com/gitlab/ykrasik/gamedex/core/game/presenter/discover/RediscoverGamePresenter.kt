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

package com.gitlab.ykrasik.gamedex.core.game.presenter.discover

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanRediscoverGame
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.game.GameDiscoveryService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/05/2018
 * Time: 10:05
 */
@Singleton
class RediscoverGamePresenter @Inject constructor(
    private val gameDiscoveryService: GameDiscoveryService,
    private val taskService: TaskService
) : Presenter<ViewCanRediscoverGame> {
    override fun present(view: ViewCanRediscoverGame) = object : ViewSession() {
        init {
            view.rediscoverGameActions.forEach { rediscoverGame(it) }
        }

        private suspend fun rediscoverGame(game: Game) {
            taskService.execute(gameDiscoveryService.rediscoverGame(game))
        }
    }
}