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

import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanRedownloadGamesUpdatedAfter
import com.gitlab.ykrasik.gamedex.app.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.core.Presentation
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.game.GameDownloadService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/05/2018
 * Time: 13:11
 */
@Singleton
class RedownloadGamesUpdatedAfterPresenter @Inject constructor(
    private val gameDownloadService: GameDownloadService,
    private val taskRunner: TaskRunner
) : Presenter<ViewCanRedownloadGamesUpdatedAfter> {
    override fun present(view: ViewCanRedownloadGamesUpdatedAfter) = object : Presentation() {
        init {
            view.redownloadGamesUpdatedAfterActions.forEach {
                taskRunner.runTask(gameDownloadService.redownloadGamesUpdatedAfterPeriod())
            }
        }
    }
}