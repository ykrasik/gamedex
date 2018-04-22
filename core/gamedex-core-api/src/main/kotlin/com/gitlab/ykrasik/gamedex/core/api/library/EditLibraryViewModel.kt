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

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.LibraryData
import com.gitlab.ykrasik.gamedex.core.api.ViewModel
import java.io.File

/**
 * User: ykrasik
 * Date: 21/04/2018
 * Time: 07:05
 */
class EditLibraryViewModel : ViewModel<EditLibraryViewModel.Event, EditLibraryViewModel.Action>() {
    sealed class Event {
        data class Shown(val library: Library?) : Event()
        data class AcceptButtonClicked(val data: LibraryData) : Event()
        object CancelButtonClicked : Event()

        object BrowseClicked : Event()
        data class BrowseClosed(val selectedDirectory: File?, val data: LibraryData, val originalLibrary: Library?) : Event()

        data class LibraryNameChanged(val data: LibraryData, val originalLibrary: Library?) : Event()
        data class LibraryPathChanged(val data: LibraryData, val originalLibrary: Library?) : Event()
        data class LibraryPlatformChanged(val data: LibraryData, val originalLibrary: Library?) : Event()
    }

    sealed class Action {
        data class SetCanChangePlatform(val canChangePlatform: Boolean) : Action()
        data class Browse(val initialDirectory: File?) : Action()

        data class SetLibraryData(val data: LibraryData) : Action()
        data class LibraryNameValidationResult(val error: String?) : Action()
        data class LibraryPathValidationResult(val error: String?) : Action()

        data class Close(val data: LibraryData?) : Action()
    }
}

interface EditLibraryPresenter {
    fun present(): EditLibraryViewModel
}