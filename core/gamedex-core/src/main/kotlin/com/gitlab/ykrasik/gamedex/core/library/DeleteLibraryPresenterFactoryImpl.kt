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
import com.gitlab.ykrasik.gamedex.app.api.library.DeleteLibraryPresenter
import com.gitlab.ykrasik.gamedex.app.api.library.DeleteLibraryPresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.library.ViewCanDeleteLibrary
import com.gitlab.ykrasik.gamedex.app.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.core.api.game.GameService
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.launchOnUi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/05/2018
 * Time: 13:30
 */
@Singleton
class DeleteLibraryPresenterFactoryImpl @Inject constructor(
    private val libraryService: LibraryService,
    private val gameService: GameService,
    private val taskRunner: TaskRunner
) : DeleteLibraryPresenterFactory {
    override fun present(view: ViewCanDeleteLibrary): DeleteLibraryPresenter = object : DeleteLibraryPresenter {
        override fun deleteLibrary(library: Library) = launchOnUi {
            val gamesToBeDeleted = gameService.games.filter { it.library.id == library.id }
            if (view.confirmDeleteLibrary(library, gamesToBeDeleted)) {
                taskRunner.runTask(libraryService.delete(library))
            }
        }
    }
}