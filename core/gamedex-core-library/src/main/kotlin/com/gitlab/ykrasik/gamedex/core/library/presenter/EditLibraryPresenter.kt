/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.LibraryType
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.app.api.library.EditLibraryView
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.util.*
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
    private val settingsService: SettingsService,
    private val eventBus: EventBus
) : Presenter<EditLibraryView> {
    override fun present(view: EditLibraryView) = object : ViewSession() {
        init {
            view.name.forEach { onNameChanged() }
            view.path.forEach { onPathChanged() }
            view.type.forEach { onTypeChanged(it) }
            view.platform.forEach { onPlatformChanged() }

            view.browseActions.forEach { onBrowse() }
            view.acceptActions.forEach { onAccept() }
            view.cancelActions.forEach { onCancel() }
        }

        override suspend fun onShow() {
            val library = view.library
            view.name *= library?.name ?: ""
            view.path *= library?.path?.toString() ?: ""
            view.type *= library?.type ?: LibraryType.Digital
            view.platform *= when {
                library == null -> Platform.Windows
                library.type != LibraryType.Excluded -> library.platform
                else -> null
            }
            view.nameIsValid *= IsValid.valid
            view.pathIsValid *= IsValid.valid
            view.canChangeType *= Try {
                check(library == null) { "Cannot change the library type of an existing library!" }
            }
            view.canChangePlatform *= Try {
                check(library == null) { "Cannot change the platform of an existing library!" }
            }
            setCanAccept()
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

        private fun onTypeChanged(type: LibraryType) {
            view.canChangeType.assert()
            validate()
            view.canChangePlatform *= Try {
                check(type != LibraryType.Excluded) { "Cannot change the platform of excluded libraries!" }
            }
            view.platform *= if (type == LibraryType.Excluded) null else Platform.Windows
        }

        private fun onPlatformChanged() {
            view.canChangePlatform.assert()
            validate()
        }

        private fun validate() {
            validatePath()
            validateName()
        }

        private fun validateName() {
            view.nameIsValid *= Try {
                val name = view.name.value
                if (name.isEmpty()) error("Name is required!")
                if (!isAvailableNewLibrary { libraryService[name] } &&
                    !isAvailableUpdatedLibrary { libraryService[name] })
                    error("Name already in use!")
            }
            setCanAccept()
        }

        private fun validatePath() {
            view.pathIsValid *= Try {
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
            val changed = Try {
                val existingLibrary = view.library
                if (existingLibrary != null) {
                    check(
                        existingLibrary.path.toString() != view.path.value ||
                            existingLibrary.name != view.name.value ||
                            existingLibrary.type != view.type.value ||
                            existingLibrary.platformOrNull != view.platform.value
                    ) { "Nothing changed!" }
                }
            }
            view.canAccept *= view.pathIsValid.value.and(view.nameIsValid.value).and(changed)
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
            view.canAccept.assert()
            val libraryData = LibraryData(
                name = view.name.value,
                path = view.path.value.toFile(),
                type = view.type.value,
                platform = if (view.type.value != LibraryType.Excluded) view.platform.value else null
            )
            val task = if (view.library == null) {
                libraryService.add(libraryData)
            } else {
                libraryService.replace(view.library!!, libraryData)
            }

            taskService.execute(task)

            finished()
        }

        private fun onCancel() {
            finished()
        }

        private fun finished() = eventBus.viewFinished(view)
    }
}