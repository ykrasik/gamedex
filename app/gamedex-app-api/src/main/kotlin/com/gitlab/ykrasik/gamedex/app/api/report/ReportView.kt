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

package com.gitlab.ykrasik.gamedex.app.api.report

import com.gitlab.ykrasik.gamedex.FileStructure
import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameId
import com.gitlab.ykrasik.gamedex.app.api.State
import com.gitlab.ykrasik.gamedex.app.api.UserMutableState
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter

/**
 * User: ykrasik
 * Date: 29/06/2018
 * Time: 10:29
 */
interface ReportView {
    val report: UserMutableState<Report?>

    val result: State<ReportResult>
}

data class ReportResult(
    val games: List<Game>,
    val additionalData: Map<GameId, Set<Filter.Context.AdditionalData>>,
    val fileStructure: Map<GameId, FileStructure>
) {
    companion object {
        val Null = ReportResult(
            games = emptyList(),
            additionalData = emptyMap(),
            fileStructure = emptyMap()
        )
    }
}
