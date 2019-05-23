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
import com.gitlab.ykrasik.gamedex.core.game.GameService
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
    private val gameService: GameService,
    private val settingsService: SettingsService,
    private val eventBus: EventBus
) : Presenter<EditLibraryView> {
    override fun present(view: EditLibraryView) = object : ViewSession() {
        init {
            view.name.forEach { onNameChanged() }
            view.path.forEach { onPathChanged() }
            view.type.forEach {
                view.canChangeType.assert()
                onTypeChanged(it)
            }
            view.platform.forEach { onPlatformChanged() }

            view.browseActions.forEach { onBrowse() }
            view.acceptActions.forEach { onAccept() }
            view.cancelActions.forEach { onCancel() }
        }

        override suspend fun onShown() {
            val library = view.library

            view.platform *= library?.platformOrNull ?: Platform.Windows

            view.name *= library?.name ?: ""
            view.nameIsValid *= IsValid.valid

            view.path *= library?.path?.toString() ?: ""
            view.pathIsValid *= IsValid.valid

            view.type *= library?.type ?: LibraryType.Digital
            view.canChangeType *= Try { assertEmptyLibrary("type") }
            onTypeChanged(view.type.value)

            setCanAccept()
            if (library == null) {
                onBrowse()
            }
        }

        private fun onNameChanged() {
            validateName()
            setCanAccept()
        }

        private fun onPathChanged() {
            validatePath()
            setCanAccept()
        }

        private fun onTypeChanged(type: LibraryType) {
            view.shouldShowPlatform *= Try {
                check(type != LibraryType.Excluded) { "Excluded libraries don't have a platform!" }
            }
            view.canChangePlatform *= view.shouldShowPlatform.value.and(Try {
                assertEmptyLibrary("platform")
            })
            validate()
        }

        private fun onPlatformChanged() {
            view.canChangePlatform.assert()
            validate()
        }

        private fun validate() {
            validatePath()
            validateName()
            setCanAccept()
        }

        private fun validateName() {
            view.nameIsValid *= Try {
                val name = view.name.value
                if (name.isEmpty()) error("Name is required!")
                if (!isAvailableNewLibrary { libraryService[name] } &&
                    !isAvailableUpdatedLibrary { libraryService[name] })
                    error("Name already in use!")
            }
        }

        private fun validatePath() {
            view.pathIsValid *= Try {
                if (view.path.value.isEmpty()) error("Path is required!")

                val file = view.path.value.file
                if (!file.isDirectory) error("Path doesn't exist!")
                if (!isAvailableNewLibrary { libraryService[file] } &&
                    !isAvailableUpdatedLibrary { libraryService[file] })
                    error("Path already in use!")
            }
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
            view.canAccept *= view.pathIsValid.and(view.nameIsValid).and(changed)
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

        private fun assertEmptyLibrary(name: String) {
            val library = view.library
            check(library == null || gameService.games.none { it.library.id == library.id }) { "Cannot change the $name of a library with games!" }
        }

        private suspend fun onAccept() {
            view.canAccept.assert()
            val libraryData = LibraryData(
                name = view.name.value,
                path = view.path.value.file,
                type = view.type.value,
                platform = view.platform.value.takeIf { view.type.value != LibraryType.Excluded }
            )
            val task = if (view.library == null) {
                libraryService.add(libraryData)
            } else {
                libraryService.replace(view.library!!, libraryData)
            }

            taskService.execute(task)

            hideView()
        }

        private fun onCancel() {
            hideView()
        }

        private fun hideView() = eventBus.requestHideView(view)
    }
}