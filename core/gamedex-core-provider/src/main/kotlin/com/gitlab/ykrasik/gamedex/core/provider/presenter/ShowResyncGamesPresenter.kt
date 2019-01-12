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

import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.app.api.provider.ViewCanResyncGames
import com.gitlab.ykrasik.gamedex.core.CommonData
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 31/12/2018
 * Time: 15:48
 */
@Singleton
class ShowResyncGamesPresenter @Inject constructor(
    private val commonData: CommonData,
    private val viewManager: ViewManager,
    private val eventBus: EventBus
) : Presenter<ViewCanResyncGames> {
    override fun present(view: ViewCanResyncGames) = object : ViewSession() {
        init {
            commonData.isGameSyncRunning.disableWhenTrue(view.canResyncGames) { "Game sync in progress!" }

            view.resyncGamesActions.forEach {
                view.canResyncGames.assert()

                val resyncGamesView = viewManager.showResyncGamesView()
                eventBus.awaitViewFinished(resyncGamesView)
                viewManager.hide(resyncGamesView)
            }
        }
    }
}