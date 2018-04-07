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

package com.gitlab.ykrasik.gamedex.core.api.library

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.LibraryData
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.core.api.util.ListObservable
import java.io.File

/**
 * User: ykrasik
 * Date: 01/04/2018
 * Time: 14:09
 */
interface LibraryRepository {
    val libraries: ListObservable<Library>

    operator fun get(id: Int): Library
    operator fun get(platform: Platform, name: String): Library

    fun add(request: AddLibraryRequest): Library
    suspend fun addAll(requests: List<AddLibraryRequest>, afterEach: (Library) -> Unit): List<Library>

    fun replace(source: Library, target: Library)

    fun delete(library: Library)
    fun deleteAll(libraries: List<Library>)

    fun invalidate()
}

data class AddLibraryRequest(
    val path: File,
    val data: LibraryData
)