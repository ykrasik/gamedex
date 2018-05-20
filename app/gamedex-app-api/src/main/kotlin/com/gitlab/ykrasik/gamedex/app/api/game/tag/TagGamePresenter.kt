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

package com.gitlab.ykrasik.gamedex.app.api.game.tag

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.PresenterFactory

/**
 * User: ykrasik
 * Date: 02/05/2018
 * Time: 22:02
 */
interface TagGamePresenter {
    fun tagGame(game: Game)
}

interface ViewCanTagGame {
    fun showTagGameView(game: Game): TagGameChoice
}

sealed class TagGameChoice {
    data class Select(val tags: List<String>) : TagGameChoice()
    object Cancel : TagGameChoice()
}

interface TagGamePresenterFactory : PresenterFactory<ViewCanTagGame, TagGamePresenter>