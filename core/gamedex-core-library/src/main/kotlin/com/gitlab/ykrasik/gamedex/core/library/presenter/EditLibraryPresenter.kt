/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.core.settings.GeneralSettingsRepository
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.util.Try
import com.gitlab.ykrasik.gamedex.util.and
import com.gitlab.ykrasik.gamedex.util.existsOrNull
import com.gitlab.ykrasik.gamedex.util.file
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
    private val settingsRepo: GeneralSettingsRepository,
    private val eventBus: EventBus
) : Presenter<EditLibraryView> {
    override fun present(view: EditLibraryView) = object : ViewSession() {
        private val library get() = view.library.peek()
        private var nextTypeChangeIsExpected = false
        private var nextPlatformChangeIsExpected = false

        init {
            view.library.forEach { library ->
                nextTypeChangeIsExpected = true
                nextPlatformChangeIsExpected = true

                view.platform *= library?.platformOrNull ?: Platform.Windows
                view.name *= library?.name ?: ""
                view.path *= library?.path?.toString() ?: ""
                view.type *= library?.type ?: LibraryType.Digital
                view.canChangeType *= assertEmptyLibrary("type")
            }

            view.name.mapTo(view.nameIsValid) { validateName(it) }
            view.path.mapTo(view.pathIsValid) { validatePath(it) }
            view.type.forEach { onTypeChanged(it) }

            view.nameIsValid.combineLatest(view.pathIsValid) { nameIsValid, pathIsValid ->
                view.canAccept *= pathIsValid and nameIsValid and Try {
                    val library = library
                    if (library != null) {
                        check(
                            library.path.toString() != view.path.value ||
                                library.name != view.name.value ||
                                library.type != view.type.value ||
                                library.platformOrNull != view.platform.value
                        ) { "Nothing changed!" }
                    }
                }
            }

            view.platform.forEach {
                if (!nextPlatformChangeIsExpected) view.canChangePlatform.assert()
                nextPlatformChangeIsExpected = false
            }

            view.browseActions.forEach { onBrowse() }
            view.acceptActions.forEach { onAccept() }
            view.cancelActions.forEach { onCancel() }
        }

        override suspend fun onShown() {
            if (library == null) {
                onBrowse()
            }
        }

        private fun onTypeChanged(type: LibraryType) {
            if (!nextTypeChangeIsExpected) view.canChangeType.assert()
            nextTypeChangeIsExpected = false
            view.shouldShowPlatform *= Try { check(type != LibraryType.Excluded) { "Excluded libraries don't have a platform!" } }
            view.canChangePlatform *= view.shouldShowPlatform.value and assertEmptyLibrary("platform")
        }

        private fun validateName(name: String) = Try {
            if (name.isEmpty()) error("Name is required!")
            if (!isAvailableNewLibrary { libraryService[name] } &&
                !isAvailableUpdatedLibrary { libraryService[name] }) {
                error("Name already in use!")
            }
        }

        private fun validatePath(path: String) = Try {
            if (path == library?.path?.toString()) return@Try

            if (path.isEmpty()) error("Path is required!")
            val file = path.file
            if (!file.isDirectory) error("Path doesn't exist!")
            if (!isAvailableNewLibrary { libraryService[file] } &&
                !isAvailableUpdatedLibrary { libraryService[file] }) {
                error("Path already in use!")
            }
        }

        private inline fun isAvailableNewLibrary(findExisting: () -> Library?): Boolean =
            library == null && findExisting() == null

        private inline fun isAvailableUpdatedLibrary(findExisting: (Library) -> Library?): Boolean =
            library?.let { library -> (findExisting(library) ?: library) == library } ?: false

        private fun onBrowse() {
            val initialDirectory = settingsRepo.prevDirectory.value.existsOrNull()
            val selectedDirectory = view.browse(initialDirectory)
            if (selectedDirectory != null) {
                settingsRepo.prevDirectory *= selectedDirectory
                view.path *= selectedDirectory.toString()
                if (view.name.value.isEmpty()) {
                    view.name *= selectedDirectory.name
                }
            }
        }

        private fun assertEmptyLibrary(name: String) = Try {
            val library = library
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

            val library = library
            val task = if (library == null) {
                libraryService.add(libraryData)
            } else {
                libraryService.replace(library, libraryData)
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