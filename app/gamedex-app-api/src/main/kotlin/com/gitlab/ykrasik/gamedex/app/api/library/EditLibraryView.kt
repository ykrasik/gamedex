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

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.LibraryData
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.app.api.Presenter
import com.gitlab.ykrasik.gamedex.app.api.View
import java.io.File

/**
 * User: ykrasik
 * Date: 21/04/2018
 * Time: 07:05
 */
interface EditLibraryView : View<EditLibraryView.Event> {
    sealed class Event {
        data class Shown(val library: Library?) : Event()
        object AcceptButtonClicked : Event()
        object CancelButtonClicked : Event()
        object BrowseClicked : Event()

        data class LibraryNameChanged(val name: String) : Event()
        data class LibraryPathChanged(val path: String) : Event()
        data class LibraryPlatformChanged(val platform: Platform) : Event()
    }

    var canChangePlatform: Boolean
    var canAccept: Boolean

    var initialLibrary: Library?
    var name: String
    var path: String
    var platform: Platform

    var nameValidationError: String?
    var pathValidationError: String?

    fun selectDirectory(initialDirectory: File?): File?

    fun close(data: LibraryData?)
}

interface EditLibraryPresenter : Presenter<EditLibraryView>