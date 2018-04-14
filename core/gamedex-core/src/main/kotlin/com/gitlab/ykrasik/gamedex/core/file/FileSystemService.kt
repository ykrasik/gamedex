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

import com.gitlab.ykrasik.gamedex.core.api.file.FileSystemService
import com.gitlab.ykrasik.gamedex.util.FileSize
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
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
    private val fileNameHandler: FileNameHandler
) : FileSystemService {
    // TODO: If first calculation returned 0 (due to file not being available), it will be cached and not be re-calculated.
    private val sizeCache = mutableMapOf<File, Deferred<FileSize>>()

    override fun size(file: File): Deferred<FileSize> =
        sizeCache.getOrPut(file) {
            async(CommonPool) {
                file.sizeTaken()
            }
        }

    private fun File.sizeTaken() = FileSize(walkBottomUp().fold(0L) { acc, f -> if (f.isFile) acc + f.length() else acc })

    // TODO: Have a reference to libraryRepo & gameRepo and calc the excludedDirectories from it.
    override fun detectNewDirectories(dir: File, excludedDirectories: Set<File>) = newDirectoryDetector.detectNewDirectories(dir, excludedDirectories)

    override fun analyzeFolderName(rawName: String) = fileNameHandler.analyze(rawName)

    override fun fromFileName(name: String) = fileNameHandler.fromFileName(name)
    override fun toFileName(name: String) = fileNameHandler.toFileName(name)
}