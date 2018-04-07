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

package com.gitlab.ykrasik.gamedex.javafx.library

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.core.api.game.GameRepository
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryRepository
import com.gitlab.ykrasik.gamedex.core.game.GameSettings
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.dialog.areYouSureDialog
import com.gitlab.ykrasik.gamedex.javafx.notification.Notifier
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.withContext
import tornadofx.Controller
import tornadofx.label
import tornadofx.listview
import tornadofx.observable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 23/04/2017
 * Time: 11:05
 */
@Singleton
class LibraryController @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val gameRepository: GameRepository,
    private val notifier: Notifier,
    settings: GameSettings
) : Controller() {

    val allLibraries = libraryRepository.libraries.toObservableList()
    val realLibraries = allLibraries.filtered { it.platform != Platform.excluded }
    val platformLibraries = realLibraries.sortedFiltered().apply {
        predicateProperty.bind(settings.platformSubject.toBindingCached().toPredicateF { platform, library: Library ->
            library.platform == platform
        })
    }

    suspend fun onAddLibraryRequested(): Boolean = withContext(JavaFx) {
        addOrEditLibrary<LibraryFragment.Choice.AddNewLibrary>(library = null) { choice ->
            libraryRepository.add(choice.request)
            notifier.showInfoNotification("Added library: '${choice.request.data.name}'.")
        }
    }

    suspend fun edit(library: Library): Boolean = withContext(JavaFx) {
        addOrEditLibrary<LibraryFragment.Choice.EditLibrary>(library) { choice ->
            libraryRepository.replace(library, choice.library)
            notifier.showInfoNotification("Updated library: '${choice.library.name}'.")
        }
    }

    private suspend inline fun <reified T : LibraryFragment.Choice> addOrEditLibrary(library: Library?,
                                                                                     noinline f: suspend (T) -> Unit): Boolean {
        val choice = LibraryFragment(library).show()
        if (choice === LibraryFragment.Choice.Cancel) return false

        f(choice as T)
        return true
    }

    suspend fun delete(library: Library): Boolean = withContext(JavaFx) {
        if (!confirmDelete(library)) return@withContext false

        libraryRepository.delete(library)
        notifier.showInfoNotification("Deleted library: '${library.name}'.")
        true
    }

    private fun confirmDelete(library: Library): Boolean {
        val gamesToBeDeleted = gameRepository.games.mapNotNull { if (it.library.id == library.id) it.name else null }
        return areYouSureDialog("Delete library '${library.name}'?") {
            if (gamesToBeDeleted.isNotEmpty()) {
                label("The following ${gamesToBeDeleted.size} games will also be deleted:")
                listview(gamesToBeDeleted.observable()) { fitAtMost(10) }
            }
        }
    }

    fun getBy(platform: Platform, name: String) = libraryRepository[platform, name]
}