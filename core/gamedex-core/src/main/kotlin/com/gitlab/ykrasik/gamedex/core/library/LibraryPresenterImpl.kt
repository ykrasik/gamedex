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
import com.gitlab.ykrasik.gamedex.app.api.library.LibraryPresenter
import com.gitlab.ykrasik.gamedex.app.api.library.LibraryView
import com.gitlab.ykrasik.gamedex.app.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.core.BasePresenter
import com.gitlab.ykrasik.gamedex.core.api.game.GameService
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 15/04/2018
 * Time: 08:31
 */
@Singleton
class LibraryPresenterImpl @Inject constructor(
    private val libraryService: LibraryService,
    private val gameService: GameService,
    private val taskRunner: TaskRunner
) : BasePresenter<LibraryView.Event, LibraryView>(), LibraryPresenter {

    override fun initView(view: LibraryView) {
        view.libraries = libraryService.libraries
    }

    override suspend fun handleEvent(event: LibraryView.Event) = when (event) {
        LibraryView.Event.AddLibraryClicked -> handleAddLibraryClicked()
        is LibraryView.Event.EditLibraryClicked -> handleEditLibraryClicked(event.library)
        is LibraryView.Event.DeleteLibraryClicked -> handleDeleteLibraryClicked(event.library)
    }

    private suspend fun handleAddLibraryClicked() {
        val data = view.showAddLibraryView()
        if (data != null) {
            taskRunner.runTask(libraryService.add(data))
        }
    }

    private suspend fun handleEditLibraryClicked(library: Library) {
        val data = view.showEditLibraryView(library)
        if (data != null) {
            taskRunner.runTask(libraryService.replace(library, data))
        }
    }

    private suspend fun handleDeleteLibraryClicked(library: Library) {
        val gamesToBeDeleted = gameService.games.filter { it.library.id == library.id }
        if (view.confirmDeleteLibrary(library, gamesToBeDeleted)) {
            taskRunner.runTask(libraryService.delete(library))
        }
    }
}