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

package com.gitlab.ykrasik.gamedex.app.api.general

import com.gitlab.ykrasik.gamedex.GameId
import com.gitlab.ykrasik.gamedex.util.FileSize
import kotlinx.coroutines.experimental.channels.ReceiveChannel

/**
 * User: ykrasik
 * Date: 22/09/2018
 * Time: 14:27
 */
interface CleanupCacheView {
    val cleanupCacheActions: ReceiveChannel<Unit>

    fun confirmDeleteStaleCache(staleCache: StaleCache): Boolean
}

data class StaleCache(
    val images: Map<String, FileSize>,
    val fileStructure: Map<GameId, FileSize>
) {
    val isEmpty = images.isEmpty() && fileStructure.isEmpty()

    val staleImagesSizeTaken get() = images.values.fold(FileSize(0)) { acc, next -> acc + next }
    val staleFileStructureSizeTaken get() = fileStructure.values.fold(FileSize(0)) { acc, next -> acc + next }
}