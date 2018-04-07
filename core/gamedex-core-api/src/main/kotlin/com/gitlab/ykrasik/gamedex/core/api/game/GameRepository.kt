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

package com.gitlab.ykrasik.gamedex.core.api.game

import com.gitlab.ykrasik.gamedex.core.api.task.Task
import com.gitlab.ykrasik.gamedex.core.api.util.ListObservable
import com.gitlab.ykrasik.gamedex.*

/**
 * User: ykrasik
 * Date: 01/04/2018
 * Time: 14:11
 */
interface GameRepository {
    val games: ListObservable<Game>

    operator fun get(id: Int): Game

    fun add(request: AddGameRequest): Game
    suspend fun addAll(requests: List<AddGameRequest>, task: Task): List<Game>

    fun replace(source: Game, target: RawGame): Game

    fun delete(game: Game)
    fun deleteAll(games: List<Game>)

    fun deleteAllUserData()

    fun invalidate()
}

data class AddGameRequest(
    val metadata: Metadata,
    val providerData: List<ProviderData>,
    val userData: UserData?
)