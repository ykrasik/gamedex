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

package com.gitlab.ykrasik.gamedex.core.library.presenter

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.LibraryData
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.app.api.library.EditLibraryView
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.util.IsValid
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
    private val taskService: TaskService,
    private val libraryService: LibraryService,
    private val viewManager: ViewManager,
    private val settingsService: SettingsService
) : Presenter<EditLibraryView> {
    override fun present(view: EditLibraryView) = object : ViewSession() {
        init {
            view.name.forEach { onNameChanged() }
            view.path.forEach { onPathChanged() }
            view.platform.forEach { onPlatformChanged() }

            view.browseActions.forEach { onBrowse() }
            view.acceptActions.forEach { onAccept() }
            view.cancelActions.forEach { onCancel() }
        }

        override fun onShow() {
            val library = view.library
            view.name *= library?.name ?: ""
            view.path *= library?.path?.toString() ?: ""
            view.platform *= library?.platform ?: Platform.pc
            view.nameIsValid *= IsValid.valid
            view.pathIsValid *= IsValid.valid
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
            validatePath()
            validateName()
        }

        private fun validateName() {
            view.nameIsValid *= IsValid {
                val name = view.name.value
                if (name.isEmpty()) error("Name is required!")
                if (!isAvailableNewLibrary { libraryService[view.platform.value, name] } &&
                    !isAvailableUpdatedLibrary { libraryService[it.platform, name] })
                    error("Name already in use for this platform!")
            }
            setCanAccept()
        }

        private fun validatePath() {
            view.pathIsValid *= IsValid {
                if (view.path.value.isEmpty()) error("Path is required!")

                val file = view.path.value.toFile()
                if (!file.isDirectory) error("Path doesn't exist!")
                if (!isAvailableNewLibrary { libraryService[file] } &&
                    !isAvailableUpdatedLibrary { libraryService[file] })
                    error("Path already in use!")
            }
            setCanAccept()
        }

        private inline fun isAvailableNewLibrary(findExisting: () -> Library?): Boolean =
            view.library == null && findExisting() == null

        private inline fun isAvailableUpdatedLibrary(findExisting: (Library) -> Library?): Boolean =
            view.library?.let { library -> (findExisting(library) ?: library) == library } ?: false

        private fun setCanAccept() {
            view.canAccept *= view.pathIsValid.and(view.nameIsValid)
        }

        private fun onBrowse() {
            val initialDirectory = settingsService.general.prevDirectory.existsOrNull()
            val selectedDirectory = view.selectDirectory(initialDirectory)
            if (selectedDirectory != null) {
                settingsService.general.modify { copy(prevDirectory = selectedDirectory) }
                view.path *= selectedDirectory.toString()
                if (view.name.value.isEmpty()) {
                    view.name *= selectedDirectory.name
                }
            }
            validate()
        }

        private suspend fun onAccept() {
            check(view.canAccept.value.isSuccess) { "Cannot accept invalid state!" }
            val libraryData = LibraryData(name = view.name.value, path = view.path.value.toFile(), platform = view.platform.value)
            val task = if (view.library == null) {
                libraryService.add(libraryData)
            } else {
                libraryService.replace(view.library!!, libraryData)
            }

            taskService.execute(task)

            close()
        }

        private fun onCancel() {
            close()
        }

        private fun close() = viewManager.closeEditLibraryView(view)
    }
}