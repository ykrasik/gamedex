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

package com.gitlab.ykrasik.gamedex.core.provider.presenter

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.provider.ViewCanRedownloadGame
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.provider.RedownloadGameService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RedownloadGamePresenter @Inject constructor(
    private val redownloadGameService: RedownloadGameService,
    private val taskService: TaskService
) : Presenter<ViewCanRedownloadGame> {
    override fun present(view: ViewCanRedownloadGame) = object : ViewSession() {
        init {
            view.redownloadGameActions.forEach { redownloadGame(it) }
        }

        private suspend fun redownloadGame(game: Game) {
            taskService.execute(redownloadGameService.redownloadGame(game))
        }
    }
}