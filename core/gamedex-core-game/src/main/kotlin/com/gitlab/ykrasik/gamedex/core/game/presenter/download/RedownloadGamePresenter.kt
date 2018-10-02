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

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanRedownloadGame
import com.gitlab.ykrasik.gamedex.app.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.core.Presentation
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.game.GameDownloadService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RedownloadGamePresenter @Inject constructor(
    private val gameDownloadService: GameDownloadService,
    private val taskRunner: TaskRunner
) : Presenter<ViewCanRedownloadGame> {
    override fun present(view: ViewCanRedownloadGame) = object : Presentation() {
        init {
            view.redownloadGameActions.forEach { redownloadGame(it) }
        }

        private suspend fun redownloadGame(game: Game) {
            taskRunner.runTask(gameDownloadService.redownloadGame(game))
        }
    }
}