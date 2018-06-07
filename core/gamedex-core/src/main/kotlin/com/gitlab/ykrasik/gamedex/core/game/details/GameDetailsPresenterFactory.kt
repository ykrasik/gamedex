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

package com.gitlab.ykrasik.gamedex.core.game.details

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.app.api.game.GameDetailsView
import com.gitlab.ykrasik.gamedex.app.api.util.ListItemRemovedEvent
import com.gitlab.ykrasik.gamedex.app.api.util.ListItemSetEvent
import com.gitlab.ykrasik.gamedex.app.api.util.ListItemsRemovedEvent
import com.gitlab.ykrasik.gamedex.app.api.util.ListItemsSetEvent
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.PresenterFactory
import com.gitlab.ykrasik.gamedex.core.api.game.GameService
import com.gitlab.ykrasik.gamedex.core.api.image.ImageRepository
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 29/04/2018
 * Time: 20:22
 */
@Singleton
class GameDetailsPresenterFactory @Inject constructor(
    private val gameService: GameService,
    private val imageRepository: ImageRepository,
    private val viewManager: ViewManager
) : PresenterFactory<GameDetailsView> {
    override fun present(view: GameDetailsView) = object : Presenter() {
        init {
            gameService.games.changesChannel.subscribeOnUi { event ->
                if (!showing) return@subscribeOnUi

                val game = view.game
                when (event) {
                    is ListItemRemovedEvent -> {
                        if (event.item == game) close()
                    }
                    is ListItemsRemovedEvent -> {
                        if (event.items.contains(game)) close()
                    }
                    is ListItemSetEvent -> {
                        if (event.item.id == game.id) {
                            view.game = event.item
                            onShow()
                        }
                    }
                    is ListItemsSetEvent -> {
                        val relevantGame = event.items.find { it.id == game.id }
                        if (relevantGame != null) {
                            view.game = relevantGame
                            onShow()
                        }
                    }
                    else -> {
                        // ignored
                    }
                }
            }
        }

        override fun onShow() {
            view.game.let { game ->
                view.displayWebPage(youTubeSearchUrl(game))
                view.poster = game.posterUrl?.let { posterUrl ->
                    imageRepository.fetchImage(posterUrl, game.id, persistIfAbsent = false)
                }
            }
        }

        private fun youTubeSearchUrl(game: Game) =
            "https://www.youtube.com/results?search_query=${URLEncoder.encode("${game.name} ${game.platform} gameplay", "utf-8")}"

        private fun close() {
            viewManager.closeGameDetailsView(view)
        }
    }
}