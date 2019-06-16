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
import com.gitlab.ykrasik.gamedex.app.api.provider.ViewCanResyncGame
import com.gitlab.ykrasik.gamedex.app.api.util.combineLatest
import com.gitlab.ykrasik.gamedex.core.CommonData
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.provider.ResyncGameService
import com.gitlab.ykrasik.gamedex.provider.supports
import com.gitlab.ykrasik.gamedex.util.Try
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/05/2018
 * Time: 10:05
 */
@Singleton
class ResyncGamePresenter @Inject constructor(
    private val resyncGameService: ResyncGameService,
    private val commonData: CommonData
) : Presenter<ViewCanResyncGame> {
    override fun present(view: ViewCanResyncGame) = object : ViewSession() {
        init {
            commonData.enabledProviders.itemsChannel.subscribe()
                .combineLatest(view.gameChannel.subscribe())
                .combineLatest(commonData.isGameSyncRunning.subscribe())
                .forEach {
                    val (enabledProviders, game) = it.first
                    val isGameSyncRunning = it.second

                    view.canResyncGame *= Try {
                        check(!isGameSyncRunning) { "Game sync in progress!" }
                        check(enabledProviders.any { it.supports(game.platform) }) { "Please enable at least 1 provider which supports the platform '${game.platform}'!" }
                    }
                }

            view.resyncGameActions.forEach { resyncGame(it) }
        }

        private fun resyncGame(game: Game) {
            view.canResyncGame.assert()
            resyncGameService.resyncGame(game)
        }
    }
}