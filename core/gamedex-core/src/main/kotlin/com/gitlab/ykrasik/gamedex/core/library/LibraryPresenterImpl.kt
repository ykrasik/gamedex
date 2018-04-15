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
import com.gitlab.ykrasik.gamedex.core.api.library.*
import com.gitlab.ykrasik.gamedex.core.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.core.api.task.TaskType
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
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
    private lateinit var libraryView: LibraryView

    // FIXME: what happens on rebind? close existing subscriptions.
    override fun bindView(libraryView: LibraryView) {
        this.libraryView = libraryView

        launch(CommonPool) {
            // TODO: Consider better ways of doing this.
            libraryView.inputActions.send(LibraryViewAction.Init(libraryRepository.libraries))

            libraryView.outputEvents.consumeEach { event ->
                when (event) {
                    LibraryViewEvent.AddLibraryClicked ->
                        sendAction(LibraryViewAction.ShowAddLibraryView)
                    is LibraryViewEvent.AddLibraryViewClosed ->
                        if (event.request != null) {
                            addLibrary(event.request!!)
                        }

                    is LibraryViewEvent.EditLibraryClicked ->
                        sendAction(LibraryViewAction.ShowEditLibraryView(event.library))
                    is LibraryViewEvent.EditLibraryViewClosed ->
                        if (event.updatedLibrary != null) {
                            replaceLibrary(event.library, event.updatedLibrary!!)
                        }

                    is LibraryViewEvent.DeleteLibraryClicked ->
                        sendAction(LibraryViewAction.ShowDeleteLibraryConfirmDialog(event.library, gamesToBeDeleted(event.library)))
                    is LibraryViewEvent.DeleteLibraryConfirmDialogClosed ->
                        if (event.confirm) {
                            deleteLibrary(event.library)
                        }
                }
            }
        }
    }

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

    private suspend fun sendAction(action: LibraryViewAction) = libraryView.inputActions.send(action)
}