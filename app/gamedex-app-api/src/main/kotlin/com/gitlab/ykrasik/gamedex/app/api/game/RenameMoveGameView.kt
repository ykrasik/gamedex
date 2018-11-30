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

package com.gitlab.ykrasik.gamedex.app.api.game

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Library
import kotlinx.coroutines.channels.ReceiveChannel
import java.io.File

/**
 * User: ykrasik
 * Date: 17/05/2018
 * Time: 09:10
 */
interface RenameMoveGameView {
    val possibleLibraries: MutableList<Library>

    var initialName: String?
    var game: Game

    var library: Library
    val libraryChanges: ReceiveChannel<Library>

    var path: String
    val pathChanges: ReceiveChannel<String>

    var name: String
    val nameChanges: ReceiveChannel<String>

    var nameValidationError: String?

    val selectDirectoryActions: ReceiveChannel<Unit>
    fun selectDirectory(initialDirectory: File): File?

    val browseToGameActions: ReceiveChannel<Unit>
    fun browseTo(dir: File)

    val acceptActions: ReceiveChannel<Unit>
    val cancelActions: ReceiveChannel<Unit>
}