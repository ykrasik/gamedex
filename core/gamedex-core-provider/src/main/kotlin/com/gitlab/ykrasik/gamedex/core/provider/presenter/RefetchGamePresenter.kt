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
import com.gitlab.ykrasik.gamedex.app.api.provider.ViewCanRefetchGame
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.provider.RefetchGameService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RefetchGamePresenter @Inject constructor(
    private val refetchGameService: RefetchGameService,
    private val taskService: TaskService
) : Presenter<ViewCanRefetchGame> {
    override fun present(view: ViewCanRefetchGame) = object : ViewSession() {
        init {
            view.refetchGameActions.forEach { refetchGame(it) }
        }

        private suspend fun refetchGame(game: Game) {
            taskService.execute(refetchGameService.refetchGame(game))
        }
    }
}