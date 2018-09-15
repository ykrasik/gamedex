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

import com.gitlab.ykrasik.gamedex.LibraryData
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.app.api.library.EditLibraryView
import com.gitlab.ykrasik.gamedex.app.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.core.Presentation
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
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
class EditLibraryPresenter @Inject constructor(
    private val taskRunner: TaskRunner,
    private val libraryService: LibraryService,
    private val viewManager: ViewManager,
    private val settingsService: SettingsService
) : Presenter<EditLibraryView> {
    override fun present(view: EditLibraryView) = object : Presentation() {
        init {
            view.nameChanges.forEach { onNameChanged() }
            view.pathChanges.forEach { onPathChanged() }
            view.platformChanges.forEach { onPlatformChanged() }

            view.browseActions.forEach { onBrowse() }
            view.acceptActions.forEach { onAccept() }
            view.cancelActions.forEach { onCancel() }
        }

        override fun onShow() {
            val library = view.library
            view.name = library?.name ?: ""
            view.path = library?.path?.toString() ?: ""
            view.platform = library?.platform ?: Platform.pc
            view.nameValidationError = null
            view.pathValidationError = null
            if (library == null) {
                onBrowse()
            }
        }

        private fun onNameChanged() {
            validateName()
        }

        private fun onPathChanged() {
            validatePath()
        }

        private fun onPlatformChanged() {
            check(view.library == null || view.platform == view.library!!.platform) { "Changing library platform for an existing library is not allowed: ${view.library}" }
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

        private val isAvailableNewName get() = view.library == null && libraryService.isAvailableNewName(view.platform, view.name)
        private val isAvailableUpdatedName get() = view.library != null && libraryService.isAvailableUpdatedName(view.library!!, view.name)

        private fun validatePath() {
            view.pathValidationError = when {
                view.path.isEmpty() -> "Path is required!"
                !view.path.toFile().isDirectory -> "Path doesn't exist!"
                !isAvailableNewPath && !isAvailableUpdatedPath -> "Path already in use!"
                else -> null
            }
        }

        private val isAvailableNewPath get() = view.library == null && libraryService.isAvailableNewPath(view.path.toFile())
        private val isAvailableUpdatedPath get() = view.library != null && libraryService.isAvailableUpdatedPath(view.library!!, view.path.toFile())

        private fun onBrowse() {
            val initialDirectory = settingsService.general.prevDirectory.existsOrNull()
            val selectedDirectory = view.selectDirectory(initialDirectory) ?: return
            settingsService.general.modify { copy(prevDirectory = selectedDirectory) }
            view.path = selectedDirectory.toString()
            if (view.name.isEmpty()) {
                view.name = selectedDirectory.name
            }
            validate()
        }

        private suspend fun onAccept() {
            check(view.nameValidationError == null && view.pathValidationError == null) { "Cannot accept invalid state!" }
            val libraryData = LibraryData(name = view.name, path = view.path.toFile(), platform = view.platform)
            val task = if (view.library == null) {
                libraryService.add(libraryData)
            } else {
                libraryService.replace(view.library!!, libraryData)
            }

            taskRunner.runTask(task)

            close()
        }

        private fun onCancel() {
            close()
        }

        private fun close() = viewManager.closeEditLibraryView(view)
    }
}