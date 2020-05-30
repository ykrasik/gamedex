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

package com.gitlab.ykrasik.gamedex.core.game.presenter

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.game.GameDetailsView
import com.gitlab.ykrasik.gamedex.app.api.game.ViewGameParams
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.flowOf
import com.gitlab.ykrasik.gamedex.core.game.GameEvent
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.view.Presenter
import com.gitlab.ykrasik.gamedex.core.view.ViewSession
import com.gitlab.ykrasik.gamedex.util.*
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 29/04/2018
 * Time: 20:22
 */
@Singleton
class GameDetailsPresenter @Inject constructor(
    private val gameService: GameService,
    private val eventBus: EventBus
) : Presenter<GameDetailsView> {
    private val log = logger()

    override fun present(view: GameDetailsView) = object : ViewSession() {
        private var gameParams by view.gameParams

        init {
            view.gameParams.onlyChangesFromView().forEach(debugName = "onGameChanged") {
                log.trace(it.game.rawGame.toJsonStr(pretty = true))
            }

            view::currentGameIndex *= view.gameParams.allValues().map { (game, allGames) ->
                allGames.indexOfFirst { it.id == game.id }
            }
            view::canViewNextGame *= view.currentGameIndex.map { index ->
                IsValid {
                    check(index + 1 < gameParams.games.size) { "No more games!" }
                }
            }
            view::canViewPrevGame *= view.currentGameIndex.map { index ->
                IsValid {
                    check(index - 1 >= 0) { "No more games!" }
                }
            }

            view.viewNextGameActions.debounce(100).forEach(debugName = "onViewNextGame") {
                view.canViewNextGame.assert()
                navigate(+1)
            }
            view.viewPrevGameActions.debounce(100).forEach(debugName = "onViewPrevGame") {
                view.canViewPrevGame.assert()
                navigate(-1)
            }
            view::hideViewActions.forEach { hideView() }

            this::isShowing.forEach {
                if (it) {
                    // This is a workaround to cover the case where the game is re-synced.
                    // Re-syncing will hide this view (which will make it not update the game on GameEvent.Updated)
                    // this view will then be re-opened again after syncing, but will show the old game, unless we run this code.
                    val game = gameParams.game
                    if (game.id != Game.Null.id) {
                        val upToDateGame = gameService[game.id]
                        if (game != upToDateGame) {
                            gameParams = gameParams.copy(game = upToDateGame)
                        }
                    }
                }
            }

            eventBus.flowOf<GameEvent.Updated>().forEach(debugName = "onGameUpdated") { e ->
                if (!isShowing.value) return@forEach

                val updatedGame = e.games.findTransform({ it.second }) { it.id == gameParams.game.id } ?: return@forEach
                view.gameParams /= ViewGameParams(updatedGame, gameParams.games.replaceWhere(updatedGame) { it.id == gameParams.game.id })
            }
            eventBus.flowOf<GameEvent.Deleted>().forEach(debugName = "onGameDeleted") { e ->
                if (!isShowing.value) return@forEach

                if (e.games.any { it.id == gameParams.game.id }) {
                    hideView()
                }
            }
        }

        private fun navigate(offset: Int) {
            gameParams = gameParams.copy(game = gameParams.games[view.currentGameIndex.value + offset])
        }

        private fun hideView() {
            gameParams = ViewGameParams.Null
            eventBus.requestHideView(view)
        }
    }
}