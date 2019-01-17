/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.core.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.core.util.ListObservableImpl
import com.gitlab.ykrasik.gamedex.util.logger
import com.gitlab.ykrasik.gamedex.util.time
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 19/03/2018
 * Time: 19:57
 */
@Singleton
class LibraryRepository @Inject constructor(private val persistenceService: PersistenceService) {
    private val log = logger()

    val libraries = ListObservableImpl(fetchLibraries())

    private fun fetchLibraries(): List<Library> =
        log.time("Fetching libraries...", { time, libraries -> "${libraries.size} libraries in $time" }) {
            persistenceService.fetchLibraries()
        }

    fun add(data: LibraryData): Library {
        val library = persistenceService.insertLibrary(data)
        libraries += library
        return library
    }

    suspend fun addAll(data: List<LibraryData>, afterEach: (Library) -> Unit): List<Library> {
        val libraries = data.map { libraryData ->
            GlobalScope.async(Dispatchers.IO) {
                persistenceService.insertLibrary(libraryData).also(afterEach)
            }
        }.map {
            it.await()
        }

        this.libraries += libraries
        return libraries
    }

    fun update(library: Library, data: LibraryData): Library {
        val updatedLibrary = library.copy(data = data)
        library.verifySuccess { persistenceService.updateLibrary(updatedLibrary) }
        libraries.replace(library, updatedLibrary)
        return updatedLibrary
    }

    fun delete(library: Library) {
        library.verifySuccess { persistenceService.deleteLibrary(library.id) }
        libraries -= library
    }

    fun deleteAll(libraries: List<Library>) {
        if (libraries.isEmpty()) return

        require(persistenceService.deleteLibraries(libraries.map { it.id }) == libraries.size) { "Not all libraries to be deleted existed: $libraries" }
        this.libraries -= libraries
    }

    fun invalidate() {
        // Re-fetch from persistence
        log.debug("Invalidating libraries...")
        libraries.clear()
        libraries.addAll(fetchLibraries())
    }

    private fun Library.verifySuccess(f: () -> Boolean) = require(f()) { "Library doesn't exist: $this" }
}