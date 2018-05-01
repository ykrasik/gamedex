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

package com.gitlab.ykrasik.gamedex.app.api.library

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.LibraryData
import com.gitlab.ykrasik.gamedex.app.api.Presenter
import com.gitlab.ykrasik.gamedex.app.api.View
import com.gitlab.ykrasik.gamedex.app.api.util.ListObservable

/**
 * User: ykrasik
 * Date: 15/04/2018
 * Time: 08:10
 */
interface LibraryView : View {
    var libraries: ListObservable<Library>   // TODO: Make this a val and bind to it.

    fun showAddLibraryView(): LibraryData?
    fun showEditLibraryView(library: Library): LibraryData?
    fun confirmDeleteLibrary(library: Library, gamesToBeDeleted: List<Game>): Boolean
}

interface LibraryPresenter : Presenter<LibraryView> {
    fun onAddLibrary()
    fun onEditLibrary(library: Library)
    fun onDeleteLibrary(library: Library)
}