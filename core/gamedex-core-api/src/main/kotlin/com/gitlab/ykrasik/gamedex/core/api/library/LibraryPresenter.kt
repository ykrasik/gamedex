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

package com.gitlab.ykrasik.gamedex.core.api.library

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.core.api.ViewModel
import com.gitlab.ykrasik.gamedex.core.api.util.ListObservable

/**
 * User: ykrasik
 * Date: 15/04/2018
 * Time: 08:10
 */
interface LibraryPresenter {
    fun present(): LibraryViewModel
}

data class LibraryViewModel(
    val libraries: ListObservable<Library>
) : ViewModel<LibraryViewModel.Event, LibraryViewModel.Action>() {

    sealed class Event {
        object AddLibraryClicked : Event()
        data class AddLibraryViewClosed(val request: AddLibraryRequest?) : Event()

        data class EditLibraryClicked(val library: Library) : Event()
        data class EditLibraryViewClosed(val library: Library, val updatedLibrary: Library?) : Event()

        data class DeleteLibraryClicked(val library: Library) : Event()
        data class DeleteLibraryConfirmDialogClosed(val library: Library, val confirm: Boolean) : Event()
    }

    sealed class Action {
        object ShowAddLibraryView : Action()
        data class ShowEditLibraryView(val library: Library) : Action()
        data class ShowDeleteLibraryConfirmDialog(val library: Library, val gamesToBeDeleted: List<Game>) : Action()
    }
}