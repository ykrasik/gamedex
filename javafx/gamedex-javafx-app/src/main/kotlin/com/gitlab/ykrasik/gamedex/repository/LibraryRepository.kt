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

package com.gitlab.ykrasik.gamedex.repository

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.LibraryData
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.util.logger
import javafx.collections.ObservableList
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.withContext
import tornadofx.observable
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 25/12/2016
 * Time: 19:47
 */
@Singleton
class LibraryRepository @Inject constructor(private val persistenceService: PersistenceService) {
    private val log = logger()

    val libraries: ObservableList<Library> = fetchLibraries()

    private fun fetchLibraries(): ObservableList<Library> {
        log.info("Fetching libraries...")
        val libraries = persistenceService.fetchLibraries()
        log.info("Fetched ${libraries.size} libraries.")
        return libraries.observable()
    }

    suspend fun add(request: AddLibraryRequest) = withContext(CommonPool) {
        val library = persistenceService.insertLibrary(request.path, request.data)
        withContext(JavaFx) {
            libraries += library
        }
        library
    }

    suspend fun addAll(requests: List<AddLibraryRequest>): List<Library> = withContext(CommonPool) {
        val libraries = requests.map { request ->
            persistenceService.insertLibrary(request.path, request.data)
        }
        withContext(JavaFx) {
            this.libraries += libraries
        }
        libraries
    }

    suspend fun update(library: Library): Library = withContext(JavaFx) {
        withContext(CommonPool) {
            persistenceService.updateLibrary(library)
        }

        removeById(library.id)
        libraries += library
        library
    }

    suspend fun delete(library: Library) {
        log.info("Deleting '${library.name}'...")
        withContext(CommonPool) {
            persistenceService.deleteLibrary(library.id)
        }
        withContext(JavaFx) {
            check(libraries.remove(library)) { "Error! Library doesn't exist: $library" }
        }
        log.info("Deleting '${library.name}': Done.")
    }

    suspend fun deleteAll(libraries: List<Library>) = withContext(CommonPool) {
        if (libraries.isEmpty()) return@withContext

        persistenceService.deleteLibraries(libraries.map { it.id })

        withContext(JavaFx) {
            this.libraries.setAll(this.libraries.filterNot { library -> libraries.any { it.id == library.id } }.observable())
        }
    }

    suspend fun invalidate() = withContext(JavaFx) {
        // Re-fetch all libraries from persistence
        libraries.setAll(fetchLibraries())
    }

    operator fun get(id: Int): Library = libraries.find { it.id == id } ?: throw IllegalStateException("No library found for id: $id!")
    fun getBy(platform: Platform, name: String) = libraries.find { it.platform == platform && it.name == name } ?:
        throw IllegalStateException("No library found for platform=$platform, name=$name!")

    private fun removeById(id: Int) {
        check(libraries.removeIf { it.id == id }) { "Error! Doesn't exist: Library($id)" }
    }
}

data class AddLibraryRequest(
    val path: File,
    val data: LibraryData
)