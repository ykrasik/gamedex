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

package com.gitlab.ykrasik.gamedex.app.api.game.edit

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameDataOverride
import com.gitlab.ykrasik.gamedex.GameDataType
import com.gitlab.ykrasik.gamedex.app.api.PresenterFactory

/**
 * User: ykrasik
 * Date: 02/05/2018
 * Time: 10:01
 */
interface EditGamePresenter {
    fun editGame(game: Game, initialTab: GameDataType)
}

interface ViewCanEditGame {
    fun showEditGameView(game: Game, initialTab: GameDataType): EditGameDetailsChoice
}

sealed class EditGameDetailsChoice {
    data class Override(val overrides: Map<GameDataType, GameDataOverride>) : EditGameDetailsChoice()
    object Cancel : EditGameDetailsChoice()
    object Clear : EditGameDetailsChoice()
}

interface EditGamePresenterFactory : PresenterFactory<ViewCanEditGame, EditGamePresenter>