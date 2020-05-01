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

package com.gitlab.ykrasik.gamedex.app.api.maintenance

import com.gitlab.ykrasik.gamedex.app.api.ConfirmationView
import com.gitlab.ykrasik.gamedex.app.api.util.MultiReceiveChannel
import com.gitlab.ykrasik.gamedex.app.api.util.State
import com.gitlab.ykrasik.gamedex.app.api.util.UserMutableState
import com.gitlab.ykrasik.gamedex.util.IsValid
import java.io.File

/**
 * User: ykrasik
 * Date: 06/05/2018
 * Time: 12:26
 */
interface ExportDatabaseView : ConfirmationView {
    val exportDatabaseDirectory: UserMutableState<String>
    val exportDatabaseFolderIsValid: State<IsValid>

    val shouldExportLibrary: UserMutableState<Boolean>
    val shouldExportProviderAccounts: UserMutableState<Boolean>
    val shouldExportFilters: UserMutableState<Boolean>

    val browseActions: MultiReceiveChannel<Unit>

    fun selectExportDatabaseDirectory(initialDirectory: File?): File?

    fun openDirectory(directory: File)
}