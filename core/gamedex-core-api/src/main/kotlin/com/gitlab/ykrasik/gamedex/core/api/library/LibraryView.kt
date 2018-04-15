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
import com.gitlab.ykrasik.gamedex.core.api.util.ListObservable
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel

/**
 * User: ykrasik
 * Date: 15/04/2018
 * Time: 08:10
 */
interface LibraryView {
    val outputEvents: ReceiveChannel<LibraryViewEvent>
    val inputActions: SendChannel<LibraryViewAction>
}

sealed class LibraryViewEvent {
    object AddLibraryClicked : LibraryViewEvent()
    data class AddLibraryViewClosed(val request: AddLibraryRequest?) : LibraryViewEvent()

    data class EditLibraryClicked(val library: Library) : LibraryViewEvent()
    data class EditLibraryViewClosed(val library: Library, val updatedLibrary: Library?) : LibraryViewEvent()

    data class DeleteLibraryClicked(val library: Library) : LibraryViewEvent()
    data class DeleteLibraryConfirmDialogClosed(val library: Library, val confirm: Boolean) : LibraryViewEvent()
}

sealed class LibraryViewAction {
    data class Init(val allLibraries: ListObservable<Library>) : LibraryViewAction()

    object ShowAddLibraryView : LibraryViewAction()
    data class ShowEditLibraryView(val library: Library) : LibraryViewAction()
    data class ShowDeleteLibraryConfirmDialog(val library: Library, val gamesToBeDeleted: List<Game>) : LibraryViewAction()
}