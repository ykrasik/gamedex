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

package com.gitlab.ykrasik.gamedex.app.api.game

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.app.api.ConfirmationView
import com.gitlab.ykrasik.gamedex.app.api.util.MultiReceiveChannel
import com.gitlab.ykrasik.gamedex.app.api.util.SettableList
import com.gitlab.ykrasik.gamedex.app.api.util.State
import com.gitlab.ykrasik.gamedex.app.api.util.UserMutableState
import com.gitlab.ykrasik.gamedex.util.IsValid
import java.io.File

/**
 * User: ykrasik
 * Date: 17/05/2018
 * Time: 09:10
 */
interface RenameMoveGameView : ConfirmationView {
    val initialName: String?
    val game: UserMutableState<Game>

    val possibleLibraries: SettableList<Library>

    val library: UserMutableState<Library>
    val path: UserMutableState<String>

    val name: UserMutableState<String>
    val nameIsValid: State<IsValid>

    val selectDirectoryActions: MultiReceiveChannel<Unit>
    fun selectDirectory(initialDirectory: File): File?
}