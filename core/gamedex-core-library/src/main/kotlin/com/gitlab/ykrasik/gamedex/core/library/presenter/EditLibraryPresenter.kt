/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.settings.GeneralSettingsRepository
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.core.view.Presenter
import com.gitlab.ykrasik.gamedex.core.view.ViewSession
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.gitlab.ykrasik.gamedex.util.and
import com.gitlab.ykrasik.gamedex.util.existsOrNull
import com.gitlab.ykrasik.gamedex.util.file
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
    private val eventBus: EventBus,
) : Presenter<EditLibraryView> {
    override fun present(view: EditLibraryView) = object : ViewSession() {
        private val library by view.library

        init {
            // TODO: At the time of writing this, there was a bug that caused combine(view.library.onlyValuesFromView(), isShowing) to behave incorrectly (miss the first value)
            view.library.allValues().combine(isShowing) { library, isShowing -> library to isShowing }
                .forEach(debugName = "onLibraryChanged") { (library, isShowing) ->
                    if (!isShowing) return@forEach

                    view.platform /= library?.platformOrNull ?: Platform.Windows
                    view.name /= library?.name ?: ""
                    view.path /= library?.path?.toString() ?: ""
                    view.type /= library?.type ?: LibraryType.Digital
                    view.canChangeType /= checkEmptyLibrary(library, "type")
                    view.canChangePlatform /= checkEmptyLibrary(library, "platform")

                    if (library == null) {
                        onBrowse()
                    }
                }
            view::shouldShowPlatform *= view.type.allValues().map { type ->
                IsValid {
                    check(type != LibraryType.Excluded) { "Excluded libraries don't have a platform!" }
                }
            }
            view::canChangePlatform *= view.shouldShowPlatform.map { it and checkEmptyLibrary(library, "platform") }
            view::nameIsValid *= view.name.allValues().map { name ->
                IsValid {
                    check(name.isNotBlank()) { "Name is required!" }
                    check(isAvailableLibrary(libraryService[name])) { "Name already in use!" }
                }
            }
            view::pathIsValid *= view.path.allValues().map { path ->
                IsValid {
                    if (path == library?.path?.toString()) return@IsValid

                    check(path.isNotBlank()) { "Path is required!" }
                    val file = path.file
                    check(file.isDirectory) { "Path doesn't exist!" }
                    check(isAvailableLibrary(libraryService[file])) { "Path already in use!" }
                }
            }
            view::canAccept *= view.nameIsValid.combine(view.pathIsValid) { nameIsValid, pathIsValid -> pathIsValid and nameIsValid }

            view.platform.onlyChangesFromView().forEach(debugName = "onPlatformChanged") {
                view.canChangePlatform.assert()
            }

            view::browseActions.forEach { onBrowse() }
            view::acceptActions.forEach { onAccept() }
            view::cancelActions.forEach { onCancel() }
        }

        private fun isAvailableLibrary(target: Library?) = target == null || target == library

        private fun onBrowse() {
            val initialDirectory = settingsRepo.prevDirectory.value.existsOrNull()
            val selectedDirectory = view.browse(initialDirectory)
            if (selectedDirectory != null) {
                settingsRepo.prevDirectory /= selectedDirectory
                view.path /= selectedDirectory.toString()
                if (view.name.v.isEmpty()) {
                    view.name /= selectedDirectory.name
                }
            }
        }

        private fun checkEmptyLibrary(library: Library?, name: String) = IsValid {
            check(library == null ||
                gameService.games.none { it.library.id == library.id }) { "Cannot change the $name of a library with games!" }
        }

        private suspend fun onAccept() {
            view.canAccept.assert()
            val libraryData = LibraryData(
                name = view.name.v,
                path = view.path.v.file,
                type = view.type.v,
                platform = view.platform.v.takeIf { view.type.v != LibraryType.Excluded }
            )
            if (libraryData != library?.data) {
                val library = library
                val task = if (library == null) {
                    libraryService.add(libraryData)
                } else {
                    libraryService.replace(library, libraryData)
                }
                taskService.execute(task)
            }


            hideView()
        }

        private fun onCancel() {
            hideView()
        }

        private fun hideView() = eventBus.requestHideView(view)
    }
}