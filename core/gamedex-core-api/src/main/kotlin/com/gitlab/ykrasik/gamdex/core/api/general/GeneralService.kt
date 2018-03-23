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

package com.gitlab.ykrasik.gamdex.core.api.general

import com.gitlab.ykrasik.gamdex.core.api.task.Task
import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.util.FileSize
import java.io.File

/**
 * User: ykrasik
 * Date: 01/04/2018
 * Time: 18:17
 */
// TODO: This is almost a presenter, lacks some integration with ui dialogs.
interface GeneralService {
    fun importDatabase(file: File): Task<Unit>
    fun exportDatabase(file: File): Task<Unit>

    fun detectStaleData(): Task<StaleData>
    fun deleteStaleData(staleData: StaleData): Task<Unit>
}

data class StaleData(
    val libraries: List<Library>,
    val games: List<Game>,
    val images: List<Pair<String, FileSize>>
) {
    val isEmpty = libraries.isEmpty() && games.isEmpty() && images.isEmpty()
    val staleImagesSize = images.fold(FileSize(0)) { acc, next -> acc + next.second }
}