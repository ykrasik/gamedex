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
import com.gitlab.ykrasik.gamedex.app.api.library.EditLibraryView
import com.gitlab.ykrasik.gamedex.app.api.library.EditLibraryViewPresenter
import com.gitlab.ykrasik.gamedex.app.api.library.EditLibraryViewPresenterFactory
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.general.GeneralUserConfig
import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfigRepository
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
class EditLibraryViewPresenterFactoryImpl @Inject constructor(
    private val libraryService: LibraryService,
    userConfigRepository: UserConfigRepository
) : EditLibraryViewPresenterFactory {
    private val generalUserConfig = userConfigRepository[GeneralUserConfig::class]

    override fun present(view: EditLibraryView): EditLibraryViewPresenter = object : EditLibraryViewPresenter {
        override fun onShown(library: Library?) {
            view.initialLibrary = library
            view.canChangePlatform = library == null
            view.name = library?.name ?: ""
            view.path = library?.path?.toString() ?: ""
            view.platform = library?.platform ?: Platform.pc
            view.nameValidationError = null
            view.pathValidationError = null
            if (library == null) {
                onBrowse()
            }
        }

        override fun onAccept() {
            check(view.nameValidationError == null && view.pathValidationError == null) { "Cannot accept invalid state!" }
            view.close(LibraryData(name = view.name, path = view.path.toFile(), platform = view.platform))
        }

        override fun onCancel() {
            view.close(data = null)
        }

        override fun onBrowse() {
            val initialDirectory = generalUserConfig.prevDirectory.existsOrNull()
            val selectedDirectory = view.selectDirectory(initialDirectory) ?: return
            generalUserConfig.prevDirectory = selectedDirectory
            view.path = selectedDirectory.toString()
            if (view.name.isEmpty()) {
                view.name = selectedDirectory.name
            }
            validate()
        }

        override fun onNameChanged(name: String) {
            validateName()
        }

        override fun onPathChanged(path: String) {
            validatePath()
        }

        override fun onPlatformChanged(platform: Platform) {
            check(view.platform == view.initialLibrary?.platform || view.canChangePlatform) { "Changing library platform for an existing library is not allowed: ${view.initialLibrary}" }
            validate()
        }

        private fun validate() {
            validateName()
            validatePath()
        }

        private fun validateName() {
            view.nameValidationError = when {
                view.name.isEmpty() -> "Name is required!"
                !isAvailableNewName && !isAvailableUpdatedName -> "Name already in use for this platform!"
                else -> null
            }
        }

        private val isAvailableNewName get() = view.initialLibrary == null && libraryService.isAvailableNewName(view.platform, view.name)
        private val isAvailableUpdatedName get() = view.initialLibrary != null && libraryService.isAvailableUpdatedName(view.initialLibrary!!, view.name)

        private fun validatePath() {
            view.pathValidationError = when {
                view.path.isEmpty() -> "Path is required!"
                !view.path.toFile().isDirectory -> "Path doesn't exist!"
                !isAvailableNewPath && !isAvailableUpdatedPath -> "Path already in use!"
                else -> null
            }
        }

        private val isAvailableNewPath get() = view.initialLibrary == null && libraryService.isAvailableNewPath(view.path.toFile())
        private val isAvailableUpdatedPath get() = view.initialLibrary != null && libraryService.isAvailableUpdatedPath(view.initialLibrary!!, view.path.toFile())
    }
}