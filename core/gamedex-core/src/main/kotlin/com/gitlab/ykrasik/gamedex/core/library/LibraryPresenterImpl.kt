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
import com.gitlab.ykrasik.gamedex.core.api.game.GameRepository
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryPresenter
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryRepository
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryView
import com.gitlab.ykrasik.gamedex.core.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.core.api.task.TaskType
import com.gitlab.ykrasik.gamedex.core.api.util.launchConsumeEach
import com.gitlab.ykrasik.gamedex.core.api.util.uiThreadDispatcher
import com.gitlab.ykrasik.gamedex.util.InitOnce
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
    private val gameRepository: GameRepository,
    private val taskRunner: TaskRunner
) : LibraryPresenter {
    private var view: LibraryView by InitOnce()
    
    override fun present(view: LibraryView) {
        this.view = view
        view.events.launchConsumeEach(uiThreadDispatcher) { event ->
            try {
                when (event) {
                    is LibraryView.Event.Init -> handleInit()

                    LibraryView.Event.AddLibraryClicked -> handleAddLibraryClicked()
                    is LibraryView.Event.EditLibraryClicked -> handleEditLibraryClicked(event.library)
                    is LibraryView.Event.DeleteLibraryClicked -> handleDeleteLibraryClicked(event.library)
                }
            } catch (e: Exception) {
                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e)
            }
        }
    }

    private fun handleInit() {
        view.libraries = libraryRepository.libraries
    }

    private suspend fun handleAddLibraryClicked() {
        val data = view.showAddLibraryView()
        if (data != null) {
            addLibrary(data)
        }
    }

    private suspend fun handleEditLibraryClicked(library: Library) {
        val data = view.showEditLibraryView(library)
        if (data != null) {
            replaceLibrary(library, data)
        }
    }

    private suspend fun handleDeleteLibraryClicked(library: Library) {
        val gamesToBeDeleted = gameRepository.games.filter { it.library.id == library.id }
        if (view.confirmDeleteLibrary(library, gamesToBeDeleted)) {
            deleteLibrary(library)
        }
    }

    // TODO: Move these to a LibraryService.
    private suspend fun addLibrary(data: LibraryData): Library = taskRunner.runTask("Add Library", TaskType.Quick) {
        doneMessage { "Added Library: '${data.name}'." }
        libraryRepository.add(data)
    }

    private suspend fun replaceLibrary(library: Library, data: LibraryData) = taskRunner.runTask("Update Library", TaskType.Quick) {
        doneMessage { "Updated Library: '${library.name}'." }
        libraryRepository.update(library, data)
    }

    private suspend fun deleteLibrary(library: Library) = taskRunner.runTask("Delete Library", TaskType.Quick) {
        doneMessage { "Deleted Library: '${library.name}'." }
        libraryRepository.delete(library)
    }
}