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

package com.gitlab.ykrasik.gamedex.core.library

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.LibraryData
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryRepository
import com.gitlab.ykrasik.gamedex.core.api.util.ListObservableImpl
import com.gitlab.ykrasik.gamedex.core.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.util.logger
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 19/03/2018
 * Time: 19:57
 */
@Singleton
class LibraryRepositoryImpl @Inject constructor(private val persistenceService: PersistenceService) : LibraryRepository {
    private val log = logger()

    // TODO: Add a RealLibraries and use it in GamePresenter.
    override val libraries = ListObservableImpl(fetchLibraries())

    private fun fetchLibraries(): List<Library> {
        log.info("Fetching libraries...")
        val libraries = persistenceService.fetchLibraries()
        log.info("Fetched ${libraries.size} libraries.")
        return libraries
    }

    override fun add(data: LibraryData): Library {
        val library = persistenceService.insertLibrary(data)
        libraries += library
        return library
    }

    override suspend fun addAll(data: List<LibraryData>, afterEach: (Library) -> Unit): List<Library> {
        val libraries = data.map { libraryData ->
            async(CommonPool) {
                persistenceService.insertLibrary(libraryData).also(afterEach)
            }
        }.map {
            it.await()
        }

        this.libraries += libraries
        return libraries
    }

    override fun update(library: Library, data: LibraryData) {
        val updatedLibrary = library.copy(data = data)
        library.verifySuccess { persistenceService.updateLibrary(updatedLibrary) }
        libraries.replace(library, updatedLibrary)
    }

    override fun delete(library: Library) {
        library.verifySuccess { persistenceService.deleteLibrary(library.id) }
        libraries -= library
    }

    override fun deleteAll(libraries: List<Library>) {
        if (libraries.isEmpty()) return

        require(persistenceService.deleteLibraries(libraries.map { it.id }) == libraries.size) { "Not all libraries to be deleted existed: $libraries" }
        this.libraries -= libraries
    }

    override fun invalidate() {
        // Re-fetch from persistence
        libraries.set(fetchLibraries())
    }

    override fun get(id: Int): Library = libraries.find { it.id == id }
        ?: throw IllegalArgumentException("Library doesn't exist: id=$id")

    override fun get(platform: Platform, name: String) = libraries.find { it.platform == platform && it.name == name }
        ?: throw IllegalArgumentException("Library doesn't exist: platform=$platform, name=$name")

    private fun Library.verifySuccess(f: () -> Boolean) = require(f()) { "Library doesn't exist: $this" }
}