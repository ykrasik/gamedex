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
import com.gitlab.ykrasik.gamedex.app.api.util.debounce
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.game.GameEvent
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.util.Try
import com.gitlab.ykrasik.gamedex.util.findTransform
import com.gitlab.ykrasik.gamedex.util.replaceWhere
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
    override fun present(view: GameDetailsView) = object : ViewSession() {
        init {
            view.gameParams.forEach { (game, allGames) ->
                view.currentGameIndex *= allGames.indexOfFirst { it.id == game.id }
            }
            view.currentGameIndex.forEach { currentGameIndex ->
                view.canViewNextGame *= Try { check(currentGameIndex + 1 < view.gameParams.value.games.size) { "No more games!" } }
                view.canViewPrevGame *= Try { check(currentGameIndex - 1 >= 0) { "No more games!" } }
            }

            view.viewNextGameActions.subscribe().debounce(100).forEach { onViewNextGame() }
            view.viewPrevGameActions.subscribe().debounce(100).forEach { onViewPrevGame() }
            view.hideViewActions.forEach { hideView() }

            eventBus.forEach<GameEvent.Updated> { e ->
                if (!isShowing) return@forEach

                val (game, allGames) = view.gameParams.value
                val updatedGame = e.games.findTransform({ it.second }) { it.id == game.id } ?: return@forEach
                view.gameParams *= ViewGameParams(updatedGame, allGames.replaceWhere(updatedGame) { it.id == game.id })
            }
            eventBus.forEach<GameEvent.Deleted> { e ->
                if (!isShowing) return@forEach

                val game = view.gameParams.value.game
                if (e.games.any { it.id == game.id }) {
                    hideView()
                }
            }
        }

        private fun onViewNextGame() {
            view.canViewNextGame.assert()
            navigate(+1)
        }

        private fun onViewPrevGame() {
            view.canViewPrevGame.assert()
            navigate(-1)
        }

        private fun navigate(offset: Int) {
            view.currentGameIndex.value += offset
            view.gameParams.modify { copy(game = games[view.currentGameIndex.value]) }
        }

        override suspend fun onShown() {
            // This is a workaround to cover the case where the game is re-synced.
            // Re-syncing will hide this view (which will make it not update the game on GameEvent.Updated)
            // this view will then be re-opened again after syncing, but will show the old game, unless we run this code.
            val game = view.gameParams.value.game
            if (game.id != Game.Null.id) {
                val upToDateGame = gameService[game.id]
                if (game != upToDateGame) {
                    view.gameParams.modify { copy(game = upToDateGame) }
                }
            }
        }

        private fun hideView() {
            eventBus.requestHideView(view)
        }
    }
}