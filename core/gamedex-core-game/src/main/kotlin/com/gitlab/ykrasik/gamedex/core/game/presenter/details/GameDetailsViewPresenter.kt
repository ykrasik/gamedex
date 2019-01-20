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

package com.gitlab.ykrasik.gamedex.core.game.presenter.details

import com.gitlab.ykrasik.gamedex.app.api.game.GameDetailsView
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.util.ListEvent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 29/04/2018
 * Time: 20:22
 */
@Singleton
class GameDetailsViewPresenter @Inject constructor(
    private val gameService: GameService,
    private val eventBus: EventBus
) : Presenter<GameDetailsView> {
    override fun present(view: GameDetailsView) = object : ViewSession() {
        init {
            view.game *= null
            view.hideViewActions.forEach { finished() }

            gameService.games.changesChannel.forEach { e ->
                val game = view.game.value ?: return@forEach
                when (e) {
                    is ListEvent.ItemRemoved -> {
                        if (e.item == game) finished()
                    }
                    is ListEvent.ItemsRemoved -> {
                        if (e.items.contains(game)) finished()
                    }
                    is ListEvent.ItemSet -> {
                        if (e.item.id == game.id) {
                            val item = e.item
                            view.game *= item
                        }
                    }
                    is ListEvent.ItemsSet -> {
                        val relevantGame = e.items.find { it.id == game.id }
                        if (relevantGame != null) {
                            view.game *= relevantGame
                        }
                    }
                    else -> {
                        // Ignored
                    }
                }
            }
        }

        private fun finished() {
            view.game *= null
            eventBus.viewFinished(view)
        }
    }
}