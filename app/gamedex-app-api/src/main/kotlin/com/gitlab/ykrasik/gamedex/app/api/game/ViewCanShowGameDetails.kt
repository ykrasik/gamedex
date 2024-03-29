/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.app.api.game

import com.gitlab.ykrasik.gamedex.Game
import kotlinx.coroutines.flow.Flow

/**
 * User: ykrasik
 * Date: 08/06/2018
 * Time: 09:39
 */
interface ViewCanShowGameDetails {
    val viewGameDetailsActions: Flow<ViewGameParams>
}

data class ViewGameParams(val game: Game, val games: List<Game>) {
    companion object {
        val Null = ViewGameParams(Game.Null, emptyList())
    }
}