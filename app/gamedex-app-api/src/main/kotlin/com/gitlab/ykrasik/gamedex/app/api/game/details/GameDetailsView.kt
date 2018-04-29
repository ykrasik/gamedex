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

package com.gitlab.ykrasik.gamedex.app.api.game.details

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameDataOverride
import com.gitlab.ykrasik.gamedex.GameDataType
import com.gitlab.ykrasik.gamedex.app.api.Presenter
import com.gitlab.ykrasik.gamedex.app.api.View
import com.gitlab.ykrasik.gamedex.app.api.image.Image
import kotlinx.coroutines.experimental.Deferred

/**
 * User: ykrasik
 * Date: 29/04/2018
 * Time: 20:09
 */
interface GameDetailsView : View {
    var game: Game

    var poster: Deferred<Image>?

    fun displayWebPage(url: String)

    fun showEditGameView(game: Game, initialTab: GameDataType): EditGameDetailsChoice
    fun showTagView(game: Game): TagGameChoice
    fun showConfirmDeleteGame(game: Game): DeleteGameChoice

    fun goBack()
}

interface GameDetailsPresenter : Presenter<GameDetailsView> {
    fun onShow(game: Game)

    fun onEditGameDetails(initialTab: GameDataType)
    fun onTag()
    fun onRediscoverGame()
    fun onRedownloadGame()
    fun onDeleteGame()
}

sealed class EditGameDetailsChoice {
    data class Override(val overrides: Map<GameDataType, GameDataOverride>) : EditGameDetailsChoice()
    object Cancel : EditGameDetailsChoice()
    object Clear : EditGameDetailsChoice()
}


sealed class TagGameChoice {
    data class Select(val tags: List<String>) : TagGameChoice()
    object Cancel : TagGameChoice()
}

sealed class DeleteGameChoice {
    data class Confirm(val fromFileSystem: Boolean) : DeleteGameChoice()
    object Cancel : DeleteGameChoice()
}