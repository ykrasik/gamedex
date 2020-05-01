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

package com.gitlab.ykrasik.gamedex.core.file

import com.gitlab.ykrasik.gamedex.FileTree
import com.gitlab.ykrasik.gamedex.FolderName
import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameId
import com.gitlab.ykrasik.gamedex.util.FileSize
import com.gitlab.ykrasik.gamedex.util.Ref
import java.io.File

/**
 * User: ykrasik
 * Date: 01/04/2018
 * Time: 14:04
 */
interface FileSystemService {
    fun fileTree(gameId: GameId, path: File): Ref<FileTree?>
    fun deleteCachedFileTree(gameId: GameId)
    fun getFileTreeSizeTakenExcept(excludedGames: List<Game>): Map<GameId, FileSize>

    suspend fun move(from: File, to: File)
    suspend fun delete(file: File)

    fun analyzeFolderName(rawName: String): FolderName
    fun toFileName(name: String): String
}