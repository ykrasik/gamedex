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
import com.gitlab.ykrasik.gamedex.LibraryData
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.core.api.library.EditLibraryPresenter
import com.gitlab.ykrasik.gamedex.core.api.library.EditLibraryViewModel
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryRepository
import com.gitlab.ykrasik.gamedex.core.consumeEvents
import com.gitlab.ykrasik.gamedex.core.general.GeneralUserConfig
import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfigRepository
import com.gitlab.ykrasik.gamedex.util.existsOrNull
import kotlinx.coroutines.experimental.channels.SendChannel
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 21/04/2018
 * Time: 07:07
 */
@Singleton
class EditLibraryPresenterImpl @Inject constructor(
    private val libraryRepository: LibraryRepository,
    userConfigRepository: UserConfigRepository
) : EditLibraryPresenter {
    private val generalUserConfig = userConfigRepository[GeneralUserConfig::class]

    override fun present() = EditLibraryViewModel().consumeEvents { event, actions ->
        when (event) {
            is EditLibraryViewModel.Event.Shown -> {
                actions.send(EditLibraryViewModel.Action.SetCanChangePlatform(event.library == null))
                val data = if (event.library == null) LibraryData(name = "", path = File(""), platform = Platform.pc) else event.library!!.data
                actions.send(EditLibraryViewModel.Action.SetLibraryData(data))
                if (event.library == null) browse(actions)
            }

            EditLibraryViewModel.Event.BrowseClicked -> browse(actions)
            is EditLibraryViewModel.Event.BrowseClosed -> event.selectedDirectory?.let {
                generalUserConfig.prevDirectory = it
                val name = event.originalLibrary?.name ?: it.name
                val data = event.data.copy(name = name, path = it)
                actions.send(EditLibraryViewModel.Action.SetLibraryData(data))
                validateData(event.data, event.originalLibrary, actions)
            }

            is EditLibraryViewModel.Event.LibraryNameChanged -> validateName(event.data, event.originalLibrary, actions)
            is EditLibraryViewModel.Event.LibraryPathChanged -> validatePath(event.data, event.originalLibrary, actions)
            is EditLibraryViewModel.Event.LibraryPlatformChanged -> {
                check(event.originalLibrary == null) { "Changing library platform for an existing library is not allowed: ${event.originalLibrary}"}
                validateData(event.data, event.originalLibrary, actions)
            }
            
            is EditLibraryViewModel.Event.AcceptButtonClicked -> actions.send(EditLibraryViewModel.Action.Close(event.data))
            is EditLibraryViewModel.Event.CancelButtonClicked -> actions.send(EditLibraryViewModel.Action.Close(null))
        }
    }

    private suspend fun validateData(data: LibraryData, originalLibrary: Library?, actions: SendChannel<EditLibraryViewModel.Action>) {
        validateName(data, originalLibrary, actions)
        validatePath(data, originalLibrary, actions)
    }

    private suspend fun validateName(data: LibraryData, originalLibrary: Library?, actions: SendChannel<EditLibraryViewModel.Action>) {
        val error = when {
            data.name.isEmpty() -> "Name is required!"
            libraryRepository.libraries.any { it != originalLibrary && it.name == data.name && it.platform == data.platform } ->
                "Name already in use for this platform!"
            else -> null
        }
        actions.send(EditLibraryViewModel.Action.LibraryNameValidationResult(error))
    }

    private suspend fun validatePath(data: LibraryData, originalLibrary: Library?, actions: SendChannel<EditLibraryViewModel.Action>) {
        val error = when {
            data.path.toString().isEmpty() -> "Path is required!"
            !data.path.isDirectory -> "Path doesn't exist!"
            libraryRepository.libraries.any { it != originalLibrary && it.path == data.path } -> "Path already in use!"
            else -> null
        }
        actions.send(EditLibraryViewModel.Action.LibraryPathValidationResult(error))
    }

    private suspend fun browse(actions: SendChannel<EditLibraryViewModel.Action>) {
        val initialDirectory = generalUserConfig.prevDirectory.existsOrNull()
        actions.send(EditLibraryViewModel.Action.Browse(initialDirectory))
    }
}