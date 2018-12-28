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

package com.gitlab.ykrasik.gamedex.core.game.presenter.details

import com.gitlab.ykrasik.gamedex.app.api.game.GameView
import com.gitlab.ykrasik.gamedex.app.api.util.ListItemRemovedEvent
import com.gitlab.ykrasik.gamedex.app.api.util.ListItemSetEvent
import com.gitlab.ykrasik.gamedex.app.api.util.ListItemsRemovedEvent
import com.gitlab.ykrasik.gamedex.app.api.util.ListItemsSetEvent
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.image.ImageService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 29/04/2018
 * Time: 20:22
 */
@Singleton
class GameViewPresenter @Inject constructor(
    private val gameService: GameService,
    private val imageService: ImageService,
    private val eventBus: EventBus
) : Presenter<GameView> {
    override fun present(view: GameView) = object : ViewSession() {
        init {
            gameService.games.changesChannel.forEach { event ->
                if (!isShowing) return@forEach

                val game = view.game.value
                when (event) {
                    is ListItemRemovedEvent -> {
                        if (event.item == game) finished()
                    }
                    is ListItemsRemovedEvent -> {
                        if (event.items.contains(game)) finished()
                    }
                    is ListItemSetEvent -> {
                        if (event.item.id == game.id) {
                            val item = event.item
                            view.game *= item
                            onShow()
                        }
                    }
                    is ListItemsSetEvent -> {
                        val relevantGame = event.items.find { it.id == game.id }
                        if (relevantGame != null) {
                            view.game *= relevantGame
                            onShow()
                        }
                    }
                    else -> {
                        // Ignored
                    }
                }
            }
        }

        override fun onShow() {
            view.poster *= view.game.value.posterUrl?.let { posterUrl ->
                imageService.fetchImage(posterUrl, persistIfAbsent = false)
            }
        }

        private fun finished() = eventBus.viewFinished(view)
    }
}