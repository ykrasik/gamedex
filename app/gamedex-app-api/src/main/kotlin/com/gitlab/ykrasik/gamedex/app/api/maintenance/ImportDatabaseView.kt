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

package com.gitlab.ykrasik.gamedex.app.api.maintenance

import com.gitlab.ykrasik.gamedex.app.api.ConfirmationView
import com.gitlab.ykrasik.gamedex.app.api.State
import com.gitlab.ykrasik.gamedex.app.api.UserMutableState
import com.gitlab.ykrasik.gamedex.util.IsValid
import kotlinx.coroutines.channels.ReceiveChannel
import java.io.File

/**
 * User: ykrasik
 * Date: 06/05/2018
 * Time: 12:05
 */
interface ImportDatabaseView : ConfirmationView {
    val importDatabaseFile: UserMutableState<String>
    val importDatabaseFileIsValid: State<IsValid>

    val browseActions: ReceiveChannel<Unit>

    fun selectImportDatabaseFile(initialDirectory: File?): File?
}