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

package com.gitlab.ykrasik.gamdex.core.api.library

import com.gitlab.ykrasik.gamdex.core.api.task.Progress
import com.gitlab.ykrasik.gamdex.core.api.task.Task
import com.gitlab.ykrasik.gamdex.core.api.util.ListObservable
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.LibraryData
import com.gitlab.ykrasik.gamedex.Platform
import java.io.File

/**
 * User: ykrasik
 * Date: 01/04/2018
 * Time: 14:09
 */
// TODO: Services should only suspend, Controllers / Presenters should create tasks.
interface LibraryService {
    val libraries: ListObservable<Library>

    operator fun get(id: Int): Library
    operator fun get(platform: Platform, name: String): Library

    fun add(request: AddLibraryRequest): Task<Library>
    fun addAll(requests: List<AddLibraryRequest>, progress: Progress): Task<List<Library>>

    fun replace(source: Library, target: Library): Task<Unit>

    fun delete(library: Library): Task<Unit>
    fun deleteAll(libraries: List<Library>): Task<Unit>

    fun invalidate(): Task<Unit>    // TODO: Instead, can use a setAll command
}

data class AddLibraryRequest(
    val path: File,
    val data: LibraryData
)