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

package com.gitlab.ykrasik.gamedex.app.api.general

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.app.api.Presenter
import com.gitlab.ykrasik.gamedex.app.api.View
import com.gitlab.ykrasik.gamedex.util.FileSize
import java.io.File

/**
 * User: ykrasik
 * Date: 02/04/2018
 * Time: 09:27
 */
interface GeneralSettingsView : View<GeneralSettingsView.Event> {
    sealed class Event {
        object ExportDatabaseClicked : Event()
        object ImportDatabaseClicked : Event()
        object ClearUserDataClicked : Event()
        object CleanupDbClicked : Event()
    }

    var canRunTask: Boolean

    fun selectDatabaseExportDirectory(initialDirectory: File?): File?
    fun selectDatabaseImportFile(initialDirectory: File?): File?
    fun browseDirectory(directory: File)

    fun confirmImportDatabase(): Boolean
    fun confirmClearUserData(): Boolean
    fun confirmDeleteStaleData(staleData: StaleData): Boolean
}

interface GeneralSettingsPresenter : Presenter<GeneralSettingsView>

data class StaleData(
    val libraries: List<Library>,
    val games: List<Game>,
    val images: List<Pair<String, FileSize>>
) {
    val isEmpty = libraries.isEmpty() && games.isEmpty() && images.isEmpty()
    val staleImagesSize = images.fold(FileSize(0)) { acc, next -> acc + next.second }
}