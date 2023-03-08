/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.core.library

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.LibraryData
import com.gitlab.ykrasik.gamedex.core.CoreEvent
import com.gitlab.ykrasik.gamedex.core.task.Task
import com.gitlab.ykrasik.gamedex.core.util.ListObservable
import java.io.File

/**
 * User: ykrasik
 * Date: 26/04/2018
 * Time: 19:34
 */
interface LibraryService {
    val libraries: ListObservable<Library>

    operator fun get(id: Int): Library
    operator fun get(name: String): Library?
    operator fun get(path: File): Library?

    fun add(data: LibraryData): Task<Library>
    fun addAll(data: List<LibraryData>): Task<List<Library>>

    fun replace(library: Library, data: LibraryData): Task<Library>

    fun delete(library: Library): Task<Unit>
    fun deleteAll(libraries: List<Library>): Task<Unit>
}

sealed class LibraryEvent : CoreEvent {
    data class Added(val libraries: List<Library>) : LibraryEvent()
    data class Deleted(val libraries: List<Library>) : LibraryEvent()
    data class Updated(val libraries: List<Pair<Library, Library>>) : LibraryEvent()
}