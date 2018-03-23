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

package com.gitlab.ykrasik.gamdex.core.api.file

import com.gitlab.ykrasik.gamedex.util.FileSize
import io.reactivex.Observable
import java.io.File

/**
 * User: ykrasik
 * Date: 01/04/2018
 * Time: 14:04
 */
interface FileSystemService {
    fun size(file: File): Observable<FileSize>  // TODO: Make this a channel
    fun sizeSync(file: File): FileSize

    // TODO: Make this a channel
    fun detectNewDirectories(dir: File, excludedDirectories: Set<File>): List<File>
}