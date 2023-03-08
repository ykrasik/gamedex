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

package com.gitlab.ykrasik.gamedex.core.provider

import com.gitlab.ykrasik.gamedex.app.api.provider.GameSearchState
import com.gitlab.ykrasik.gamedex.core.CoreEvent

/**
 * User: ykrasik
 * Date: 20/10/2018
 * Time: 09:32
 */
sealed class GameSearchEvent : CoreEvent {
    data class Started(val state: GameSearchState, val isAllowSmartChooseResults: Boolean) : GameSearchEvent()
    data class Updated(val state: GameSearchState) : GameSearchEvent()
}