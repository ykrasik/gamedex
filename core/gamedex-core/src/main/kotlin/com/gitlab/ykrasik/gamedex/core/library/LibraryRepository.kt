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

import com.gitlab.ykrasik.gamdex.core.api.library.AddLibraryRequest
import com.gitlab.ykrasik.gamdex.core.api.task.Progress
import com.gitlab.ykrasik.gamdex.core.api.util.ListObservable
import com.gitlab.ykrasik.gamdex.core.api.util.SubjectListObservable
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.core.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.util.logger
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.withContext
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

    // TODO: Add a RealLibraries and use it in GamePresenter.
    private val _libraries = SubjectListObservable(fetchLibraries())
    val libraries: ListObservable<Library> = _libraries

    private fun fetchLibraries(): List<Library> {
        log.info("Fetching libraries...")
        val libraries = persistenceService.fetchLibraries()
        log.info("Fetched ${libraries.size} libraries.")
        return libraries
    }

    fun add(request: AddLibraryRequest): Library {
        val library = persistenceService.insertLibrary(request.path, request.data)
        _libraries.add(library)
        return library
    }

    suspend fun addAll(requests: List<AddLibraryRequest>, progress: Progress): List<Library> = withContext(CommonPool) {
        progress.totalWork = requests.size

        val libraries = requests.map { request ->
            async(CommonPool) {
                progress.inc {
                    persistenceService.insertLibrary(request.path, request.data)
                }
            }
        }.map { it.await() }

        _libraries.addAll(libraries)
        libraries
    }

    fun replace(source: Library, target: Library) {
        source.verifySuccess { persistenceService.updateLibrary(target) }
        _libraries.replace(source, target)
    }

    fun delete(library: Library) {
        library.verifySuccess { persistenceService.deleteLibrary(library.id) }
        _libraries.remove(library)
    }

    fun deleteAll(libraries: List<Library>) {
        if (libraries.isEmpty()) return

        require(persistenceService.deleteLibraries(libraries.map { it.id }) == libraries.size) { "Not all libraries to be deleted existed: $libraries" }
        _libraries.removeAll(libraries)
    }

    fun invalidate() {
        // Re-fetch from persistence
        _libraries.set(fetchLibraries())
    }

    operator fun get(id: Int): Library = _libraries.find { it.id == id }
        ?: throw IllegalArgumentException("Library doesn't exist: id=$id")

    operator fun get(platform: Platform, name: String) = _libraries.find { it.platform == platform && it.name == name }
        ?: throw IllegalArgumentException("Library doesn't exist: platform=$platform, name=$name")

    private fun Library.verifySuccess(f: () -> Boolean) = require(f()) { "Library doesn't exist: $this" }
}