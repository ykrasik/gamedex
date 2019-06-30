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

package com.gitlab.ykrasik.gamedex.app.api.filter

import com.gitlab.ykrasik.gamedex.app.api.ConfirmationView
import com.gitlab.ykrasik.gamedex.app.api.util.State
import com.gitlab.ykrasik.gamedex.app.api.util.UserMutableState
import com.gitlab.ykrasik.gamedex.util.IsValid

/**
 * User: ykrasik
 * Date: 24/06/2018
 * Time: 10:33
 */
interface EditFilterView : ConfirmationView {
    val initialNamedFilter: UserMutableState<NamedFilter>

    val name: UserMutableState<String>
    val nameIsValid: State<IsValid>

    val filter: UserMutableState<Filter>
    val filterIsValid: UserMutableState<IsValid>

    val isTag: UserMutableState<Boolean>

    suspend fun confirmOverwrite(filterToOverwrite: NamedFilter): Boolean

//    val excludedGames: SettableList<Game>
//    val unexcludeGameActions: BroadcastReceiveChannel<Game>
}