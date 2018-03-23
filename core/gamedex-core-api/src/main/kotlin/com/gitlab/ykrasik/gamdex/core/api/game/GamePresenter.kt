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

package com.gitlab.ykrasik.gamdex.core.api.game

import com.gitlab.ykrasik.gamedex.Game

/**
 * User: ykrasik
 * Date: 05/04/2018
 * Time: 10:36
 */
// TODO: Instead of exposing a "do" api, have the views expose an event stream and react to it.
interface GamePresenter {
    suspend fun discoverNewGames()
    suspend fun rediscoverGame(game: Game): Game
    suspend fun rediscoverAllGamesWithMissingProviders()
    // TODO: Remove this, gamePresenter should know which games are sorted/filtered.
    // TODO: Add a rescanFilteredGames thing
    suspend fun rediscoverGames(games: List<Game>)

    suspend fun redownloadAllGames()
    // TODO: Remove this, gamePresenter should know which games are sorted/filtered.
    // TODO: Add a redownloadFilteredGames thing
    suspend fun redownloadGames(games: List<Game>)
    suspend fun redownloadGame(game: Game): Game
}