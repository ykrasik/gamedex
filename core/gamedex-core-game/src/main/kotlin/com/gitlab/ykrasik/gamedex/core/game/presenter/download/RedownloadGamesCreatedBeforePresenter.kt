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

import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanRedownloadGamesCreatedBefore
import com.gitlab.ykrasik.gamedex.core.Presentation
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.game.GameDownloadService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/05/2018
 * Time: 13:11
 */
@Singleton
class RedownloadGamesCreatedBeforePresenter @Inject constructor(
    private val gameDownloadService: GameDownloadService,
    private val taskService: TaskService
) : Presenter<ViewCanRedownloadGamesCreatedBefore> {
    override fun present(view: ViewCanRedownloadGamesCreatedBefore) = object : Presentation() {
        init {
            view.redownloadGamesCreatedBeforeActions.forEach {
                taskService.execute(gameDownloadService.redownloadGamesCreatedBeforePeriod())
            }
        }
    }
}