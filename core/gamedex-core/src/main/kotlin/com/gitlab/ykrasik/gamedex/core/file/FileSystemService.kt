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

package com.gitlab.ykrasik.gamedex.core.file

import com.gitlab.ykrasik.gamedex.FileStructure
import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.core.api.file.FileSystemService
import com.gitlab.ykrasik.gamedex.core.cache.Cache
import com.gitlab.ykrasik.gamedex.util.FileSize
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 30/06/2017
 * Time: 20:09
 */
@Singleton
class FileSystemServiceImpl @Inject constructor(
    private val newDirectoryDetector: NewDirectoryDetector,
    private val fileNameHandler: FileNameHandler,
    private val fileStructureCache: Cache<Game, FileStructure>
) : FileSystemService {
    override fun structure(game: Game): FileStructure {
        val structure = fileStructureCache[game]

        // Refresh the cache, regardless of whether we got a hit or not - our cached result could already be invalid.
        launch(CommonPool) {
            val newStructure = calcStructure(game.path)
            if (newStructure != null && newStructure != structure) {
                fileStructureCache[game] = newStructure
            }
        }

        // FIXME: This will first return cached result and only on 2nd call return updated result, find a better solution.
        return structure ?: FileStructure.NotAvailable
    }

    private fun calcStructure(file: File): FileStructure? {
        if (!file.exists()) return null

        return if (file.isDirectory) {
            val children = file.listFiles().map { calcStructure(it)!! }
            FileStructure(
                name = file.name,
                size = children.fold(FileSize.Empty) { acc, f -> acc + f.size },
                isDirectory = true,
                children = children
            )
        } else {
            FileStructure(
                name = file.name,
                size = FileSize(file.length()),
                isDirectory = false,
                children = emptyList()
            )
        }
    }

    // TODO: Have a reference to libraryRepo & gameRepo and calc the excludedDirectories from it.
    override fun detectNewDirectories(dir: File, excludedDirectories: Set<File>) = newDirectoryDetector.detectNewDirectories(dir, excludedDirectories)

    override fun analyzeFolderName(rawName: String) = fileNameHandler.analyze(rawName)

    override fun fromFileName(name: String) = fileNameHandler.fromFileName(name)
    override fun toFileName(name: String) = fileNameHandler.toFileName(name)

    // FIXME: Allow syncing cache to existing games, should be called on each game change by... someone.
}