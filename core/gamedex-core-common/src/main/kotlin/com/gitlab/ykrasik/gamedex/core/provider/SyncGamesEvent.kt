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

package com.gitlab.ykrasik.gamedex.core.provider

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.LibraryPath
import com.gitlab.ykrasik.gamedex.core.CoreEvent

/**
 * User: ykrasik
 * Date: 02/12/2018
 * Time: 12:49
 */
sealed class SyncGamesEvent : CoreEvent {
    data class Requested(
        val paths: List<Pair<LibraryPath, Game?>>,
        val isAllowSmartChooseResults: Boolean
    ) : SyncGamesEvent()

    object Started : SyncGamesEvent()
    object Finished : SyncGamesEvent()
}