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
import com.gitlab.ykrasik.gamedex.app.api.util.ListObservable
import com.gitlab.ykrasik.gamedex.app.api.util.Task
import java.io.File

/**
 * User: ykrasik
 * Date: 26/04/2018
 * Time: 19:34
 */
interface LibraryService {
    // TODO: Add a RealLibraries and use it in GamePresenter.
    val libraries: ListObservable<Library>
    val realLibraries: ListObservable<Library>
//    val platformLibraries: ListObservable<Library>

    operator fun get(id: Int): Library
    operator fun get(platform: Platform, name: String): Library

    fun add(data: LibraryData): Task<Library>
    fun addAll(data: List<LibraryData>): Task<List<Library>>

    fun update(library: Library, data: LibraryData): Task<Unit>

    fun delete(library: Library): Task<Unit>
    fun deleteAll(libraries: List<Library>): Task<Unit>

    fun invalidate(): Task<Unit>

    fun isAvailableNewName(platform: Platform, newName: String): Boolean
    fun isAvailableUpdatedName(library: Library, updatedName: String): Boolean

    fun isAvailableNewPath(newPath: File): Boolean
    fun isAvailableUpdatedPath(library: Library, updatedPath: File): Boolean
}