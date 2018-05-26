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

package com.gitlab.ykrasik.gamedex.core.game.discover

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.game.RediscoverGameView
import com.gitlab.ykrasik.gamedex.app.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.PresenterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/05/2018
 * Time: 10:05
 */
@Singleton
class RediscoverGamePresenterFactory @Inject constructor(
    private val gameDiscoveryService: GameDiscoveryService,
    private val taskRunner: TaskRunner
) : PresenterFactory<RediscoverGameView> {
    override fun present(view: RediscoverGameView) = object : Presenter() {
        init {
            view.rediscoverGameActions.actionOnUi { rediscoverGame(it) }
        }

        private suspend fun rediscoverGame(game: Game) {
            taskRunner.runTask(gameDiscoveryService.rediscoverGame(game))
        }
    }
}