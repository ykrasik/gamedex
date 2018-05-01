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
    taskRunner: TaskRunner
) : BasePresenter<LibraryView>(taskRunner), LibraryPresenter {

    override fun initView(view: LibraryView) {
        view.libraries = libraryService.libraries
    }

    override fun onAddLibrary() = handle {
        val data = view.showAddLibraryView()
        if (data != null) {
            libraryService.add(data).runTask()
        }
    }

    override fun onEditLibrary(library: Library) = handle {
        val data = view.showEditLibraryView(library)
        if (data != null) {
            libraryService.replace(library, data).runTask()
        }
    }

    override fun onDeleteLibrary(library: Library) = handle {
        val gamesToBeDeleted = gameService.games.filter { it.library.id == library.id }
        if (view.confirmDeleteLibrary(library, gamesToBeDeleted)) {
            libraryService.delete(library).runTask()
        }
    }
}