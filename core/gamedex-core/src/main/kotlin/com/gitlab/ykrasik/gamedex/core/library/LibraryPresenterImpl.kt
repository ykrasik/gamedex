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
import com.gitlab.ykrasik.gamedex.core.api.library.AddLibraryRequest
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryPresenter
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryRepository
import com.gitlab.ykrasik.gamedex.core.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.core.api.task.TaskType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 15/04/2018
 * Time: 08:31
 */
@Singleton
class LibraryPresenterImpl @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val taskRunner: TaskRunner
) : LibraryPresenter {
    override suspend fun addLibrary(request: AddLibraryRequest): Library = taskRunner.runTask("Add Library", TaskType.Quick) {
        doneMessage { "Added Library: '${request.data.name}'." }
        libraryRepository.add(request)
    }

    override suspend fun replaceLibrary(source: Library, target: Library) = taskRunner.runTask("Edit Library", TaskType.Quick) {
        doneMessage { "Updated Library: '${source.name}'." }
        libraryRepository.replace(source, target)
    }

    override suspend fun deleteLibrary(library: Library) = taskRunner.runTask("Delete Library", TaskType.Quick) {
        doneMessage { "Deleted Library: '${library.name}'." }
        libraryRepository.delete(library)
    }
}