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

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.core.api.game.GameRepository
import com.gitlab.ykrasik.gamedex.core.api.library.AddLibraryRequest
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryPresenter
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryRepository
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryViewModel
import com.gitlab.ykrasik.gamedex.core.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.core.api.task.TaskType
import com.gitlab.ykrasik.gamedex.core.consumeEvents
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
    override fun present() = LibraryViewModel(libraryRepository.libraries).consumeEvents { event, actions ->
        when (event) {
            LibraryViewModel.Event.AddLibraryClicked ->
                actions.send(LibraryViewModel.Action.ShowAddLibraryView)
            is LibraryViewModel.Event.AddLibraryViewClosed ->
                if (event.request != null) {
                    addLibrary(event.request!!)
                }

            is LibraryViewModel.Event.EditLibraryClicked ->
                actions.send(LibraryViewModel.Action.ShowEditLibraryView(event.library))
            is LibraryViewModel.Event.EditLibraryViewClosed ->
                if (event.updatedLibrary != null) {
                    replaceLibrary(event.library, event.updatedLibrary!!)
                }

            is LibraryViewModel.Event.DeleteLibraryClicked ->
                actions.send(LibraryViewModel.Action.ShowDeleteLibraryConfirmDialog(event.library, gamesToBeDeleted(event.library)))
            is LibraryViewModel.Event.DeleteLibraryConfirmDialogClosed ->
                if (event.confirm) {
                    deleteLibrary(event.library)
                }
        }
    }

    // TODO: Move these to a LibraryService.
    private suspend fun addLibrary(request: AddLibraryRequest): Library = taskRunner.runTask("Add Library", TaskType.Quick) {
        doneMessage { "Added Library: '${request.data.name}'." }
        libraryRepository.add(request)
    }

    private suspend fun replaceLibrary(source: Library, target: Library) = taskRunner.runTask("Edit Library", TaskType.Quick) {
        doneMessage { "Updated Library: '${source.name}'." }
        libraryRepository.replace(source, target)
    }

    private fun gamesToBeDeleted(library: Library): List<Game> =
        gameRepository.games.filter { it.library.id == library.id }

    private suspend fun deleteLibrary(library: Library) = taskRunner.runTask("Delete Library", TaskType.Quick) {
        doneMessage { "Deleted Library: '${library.name}'." }
        libraryRepository.delete(library)
    }
}