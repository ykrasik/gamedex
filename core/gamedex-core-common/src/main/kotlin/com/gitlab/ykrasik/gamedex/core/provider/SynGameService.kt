/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.core.provider

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.LibraryPath
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.core.task.Task
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import kotlinx.coroutines.flow.StateFlow

/**
 * User: ykrasik
 * Date: 22/06/2019
 * Time: 15:43
 */
interface SyncGameService {
    val isGameSyncRunning: StateFlow<Boolean>

    suspend fun syncGame(game: Game)

    fun detectGamesWithMissingProviders(filter: Filter, syncOnlyMissingProviders: Boolean): Task<List<SyncPathRequest>>

    suspend fun syncGames(requests: List<SyncPathRequest>, isAllowSmartChooseResults: Boolean)
}

data class SyncPathRequest(
    val libraryPath: LibraryPath,
    val existingGame: Game? = null,
    val syncOnlyTheseProviders: List<ProviderId> = emptyList(),  // If empty, sync all providers.
) {
    val library get() = libraryPath.library
    val path get() = libraryPath.path
    val platform get() = libraryPath.platform
}