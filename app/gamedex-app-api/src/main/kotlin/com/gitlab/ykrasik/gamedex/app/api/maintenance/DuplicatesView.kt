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

package com.gitlab.ykrasik.gamedex.app.api.maintenance

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.util.ViewMutableStateFlow
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * User: ykrasik
 * Date: 26/04/2019
 * Time: 08:09
 */
interface DuplicatesView {
    val duplicates: MutableStateFlow<List<GameDuplicates>>

    val searchText: ViewMutableStateFlow<String>
    val matchingGame: MutableStateFlow<Game?>

//    val excludeGameActions: MultiReceiveChannel<Game>

    val hideViewActions: Flow<Unit>
}

data class GameDuplicates(
    val game: Game,
    val duplicates: List<GameDuplicate>,
)

data class GameDuplicate(
    val game: Game,
    val providerId: ProviderId,
)