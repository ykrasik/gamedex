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
import com.gitlab.ykrasik.gamedex.GameDataType
import com.gitlab.ykrasik.gamedex.app.api.game.details.GameDetailsPresenter
import com.gitlab.ykrasik.gamedex.app.api.game.details.GameDetailsView
import com.gitlab.ykrasik.gamedex.app.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.core.BasePresenter
import com.gitlab.ykrasik.gamedex.core.api.image.ImageRepository
import com.gitlab.ykrasik.gamedex.core.game.common.CommonGamePresenterOps
import com.gitlab.ykrasik.gamedex.core.game.discover.GameDiscoveryService
import com.gitlab.ykrasik.gamedex.core.game.download.GameDownloadService
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 29/04/2018
 * Time: 20:22
 */
@Singleton
class GameDetailsPresenterImpl @Inject constructor(
    private val commonOps: CommonGamePresenterOps,
    private val imageRepository: ImageRepository,
    private val gameDiscoveryService: GameDiscoveryService,
    private val gameDownloadService: GameDownloadService,
    taskRunner: TaskRunner
) : BasePresenter<GameDetailsView>(taskRunner), GameDetailsPresenter {

    override fun initView(view: GameDetailsView) {
    }

    override fun onShow(game: Game) {
        view.game = game
        view.displayWebPage(youTubeSearchUrl(game))
        view.poster = if (game.posterUrl != null) {
            imageRepository.fetchImage(game.posterUrl!!, game.id, persistIfAbsent = false)
        } else {
            null
        }
    }

    private fun youTubeSearchUrl(game: Game) =
        "https://www.youtube.com/results?search_query=${URLEncoder.encode("${game.name} ${game.platform} gameplay", "utf-8")}"

    override fun onEditGameDetails(initialTab: GameDataType) = handle {
        val newGame = commonOps.editDetails(view.game) { game ->
            view.showEditGameView(game, initialTab)
        }
        if (newGame != null) {
            view.game = newGame
        }
    }

    override fun onTag() = handle {
        val newGame = commonOps.tag(view.game) { game ->
            view.showTagView(game)
        }
        if (newGame != null) {
            view.game = newGame
        }
    }

    override fun onRediscoverGame() = handle {
        val newGame = gameDiscoveryService.rediscoverGame(view.game).runTask()
        if (newGame != null) {
            view.game = newGame
        }
    }

    override fun onRedownloadGame() = handle {
        view.game = gameDownloadService.redownloadGame(view.game).runTask()
    }

    override fun onDeleteGame() = handle {
        val deleted = commonOps.delete(view.game) { game ->
            view.showConfirmDeleteGame(game)
        }
        if (deleted) {
            view.goBack()
        }
    }
}