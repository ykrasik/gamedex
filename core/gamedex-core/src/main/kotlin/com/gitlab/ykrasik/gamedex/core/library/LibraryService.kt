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
import com.gitlab.ykrasik.gamdex.core.api.library.LibraryService
import com.gitlab.ykrasik.gamdex.core.api.task.Progress
import com.gitlab.ykrasik.gamdex.core.api.task.Task
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.Platform
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 31/03/2018
 * Time: 20:10
 */
@Singleton
class LibraryServiceImpl @Inject constructor(private val libraryRepository: LibraryRepository) : LibraryService {
    override val libraries = libraryRepository.libraries

    override fun get(id: Int) = libraryRepository[id]
    override fun get(platform: Platform, name: String) = libraryRepository[platform, name]

    override fun add(request: AddLibraryRequest) = Task("Add $request") {
        libraryRepository.add(request)
    }

    override fun addAll(requests: List<AddLibraryRequest>, progress: Progress) = Task("Add ${requests.size} Libraries") {
        libraryRepository.addAll(requests, progress)
    }

    override fun replace(source: Library, target: Library) = Task("Replace $source with $target") {
        libraryRepository.replace(source, target)
    }

    override fun delete(library: Library) = Task("Delete $library") {
        libraryRepository.delete(library)
    }

    override fun deleteAll(libraries: List<Library>) = Task("Delete ${libraries.size} Libraries") {
        libraryRepository.deleteAll(libraries)
    }

    override fun invalidate() = Task {
        libraryRepository.invalidate()
    }
}