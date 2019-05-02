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

import com.gitlab.ykrasik.gamedex.Timestamp
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter

/**
 * User: ykrasik
 * Date: 19/09/2018
 * Time: 10:10
 */
typealias ReportId = Int

data class Report(
    val id: ReportId,
    val data: ReportData
) {
    val name get() = data.name
    val filter get() = data.filter
    val excludedGames get() = data.excludedGames
    val createDate get() = data.timestamp.createDate
    val updateDate get() = data.timestamp.updateDate

    companion object {
        val Null = Report(
            id = 0,
            data = ReportData(
                name = "",
                filter = Filter.Null,
                excludedGames = emptyList()
            )
        )
    }
}

data class ReportData(
    val name: String,
    val filter: Filter,
    val isTag: Boolean,
    val excludedGames: List<Int>,
    val timestamp: Timestamp
) {
    fun createdNow() = copy(timestamp = Timestamp.now)
    fun updatedNow() = copy(timestamp = timestamp.updatedNow())

    companion object {
        operator fun invoke(
            name: String,
            filter: Filter,
            isTag: Boolean = true,
            excludedGames: List<Int> = emptyList(),
            timestamp: Timestamp = Timestamp.now
        ): ReportData = ReportData(name, filter, isTag, excludedGames, timestamp)
    }
}