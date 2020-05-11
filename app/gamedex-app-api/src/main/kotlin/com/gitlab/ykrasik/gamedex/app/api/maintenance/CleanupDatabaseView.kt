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

package com.gitlab.ykrasik.gamedex.app.api.maintenance

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameId
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.app.api.ConfirmationView
import com.gitlab.ykrasik.gamedex.app.api.util.ViewMutableStateFlow
import com.gitlab.ykrasik.gamedex.util.FileSize

/**
 * User: ykrasik
 * Date: 06/05/2018
 * Time: 12:29
 */
interface CleanupDatabaseView : ConfirmationView {
    val staleData: ViewMutableStateFlow<StaleData>

    val isDeleteLibrariesAndGames: ViewMutableStateFlow<Boolean>
    val isDeleteImages: ViewMutableStateFlow<Boolean>
    val isDeleteFileCache: ViewMutableStateFlow<Boolean>
}

data class StaleData(
    val libraries: List<Library>,
    val games: List<Game>,
    val images: Map<String, FileSize>,
    val fileTrees: Map<GameId, FileSize>
) {
    val isEmpty = this == Null

    val staleImagesSizeTaken get() = images.values.fold(FileSize(0)) { acc, next -> acc + next }
    val staleFileTreesSizeTaken get() = fileTrees.values.fold(FileSize(0)) { acc, next -> acc + next }

    companion object {
        val Null = StaleData(emptyList(), emptyList(), emptyMap(), emptyMap())
    }
}