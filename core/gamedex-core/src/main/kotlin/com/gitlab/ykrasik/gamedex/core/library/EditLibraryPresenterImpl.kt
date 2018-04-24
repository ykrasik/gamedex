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
import com.gitlab.ykrasik.gamedex.core.api.library.EditLibraryView
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryRepository
import com.gitlab.ykrasik.gamedex.core.api.util.launchConsumeEach
import com.gitlab.ykrasik.gamedex.core.api.util.uiThreadDispatcher
import com.gitlab.ykrasik.gamedex.core.general.GeneralUserConfig
import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfigRepository
import com.gitlab.ykrasik.gamedex.util.InitOnce
import com.gitlab.ykrasik.gamedex.util.existsOrNull
import com.gitlab.ykrasik.gamedex.util.toFile
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

    private var view: EditLibraryView by InitOnce()

    override fun present(view: EditLibraryView) {
        this.view = view
        view.events.launchConsumeEach(uiThreadDispatcher) { event ->
            try {
                when (event) {
                    is EditLibraryView.Event.Shown -> handleShown(event.library)
                    EditLibraryView.Event.AcceptButtonClicked -> handleAcceptClicked()
                    EditLibraryView.Event.CancelButtonClicked -> handleCancelClicked()
                    EditLibraryView.Event.BrowseClicked -> selectDirectory()

                    is EditLibraryView.Event.LibraryNameChanged -> handleNameChanged()
                    is EditLibraryView.Event.LibraryPathChanged -> handlePathChanged()
                    is EditLibraryView.Event.LibraryPlatformChanged -> handlePlatformChanged()
                }
            } catch (e: Exception) {
                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e)
            }
        }
    }

    private fun handleShown(library: Library?) {
        view.initialLibrary = library
        view.canChangePlatform = library == null
        view.name = library?.name ?: ""
        view.path = library?.path?.toString() ?: ""
        view.platform = library?.platform ?: Platform.pc
        view.nameValidationError = null
        view.pathValidationError = null
        if (library == null) {
            selectDirectory()
        }
    }

    private fun handleAcceptClicked() {
        check(view.canAccept) { "Cannot accept invalid state!" }
        view.close(LibraryData(name = view.name, path = view.path.toFile(), platform = view.platform))
    }

    private fun handleCancelClicked() {
        view.close(data = null)
    }

    private fun selectDirectory() {
        val initialDirectory = generalUserConfig.prevDirectory.existsOrNull()
        val selectedDirectory = view.selectDirectory(initialDirectory) ?: return
        generalUserConfig.prevDirectory = selectedDirectory
        view.path = selectedDirectory.toString()
        if (view.name.isEmpty()) {
            view.name = selectedDirectory.name
        }
        validateData()
    }

    private fun handleNameChanged() {
        validateName()
        checkValid()
    }

    private fun handlePathChanged() {
        validatePath()
        checkValid()
    }

    private fun handlePlatformChanged() {
        check(view.platform == view.initialLibrary?.platform || view.canChangePlatform) { "Changing library platform for an existing library is not allowed: ${view.initialLibrary}" }
        validateData()
    }

    private fun validateData() {
        validateName()
        validatePath()
        checkValid()
    }

    private fun validateName() {
        view.nameValidationError = when {
            view.name.isEmpty() -> "Name is required!"
            libraryRepository.libraries.any { it != view.initialLibrary && it.name == view.name && it.platform == view.platform } ->
                "Name already in use for this platform!"
            else -> null
        }
    }

    private fun validatePath() {
        view.pathValidationError = when {
            view.path.isEmpty() -> "Path is required!"
            !view.path.toFile().isDirectory -> "Path doesn't exist!"
            libraryRepository.libraries.any { it != view.initialLibrary && it.path == view.path.toFile() } -> "Path already in use!"
            else -> null
        }
    }

    private fun checkValid() {
        view.canAccept = view.nameValidationError == null && view.pathValidationError == null
    }
}