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

package com.gitlab.ykrasik.gamedex.app.api.filter

import com.gitlab.ykrasik.gamedex.Timestamp

/**
 * User: ykrasik
 * Date: 19/09/2018
 * Time: 10:10
 */
typealias FilterId = String

data class NamedFilter(
    val id: FilterId,
    val filter: Filter,
    val isTag: Boolean,
    val timestamp: Timestamp
) {
    val isAnonymous: Boolean get() = id.isEmpty()
    fun createdNow() = copy(timestamp = Timestamp.now)
    fun updatedNow() = copy(timestamp = timestamp.updatedNow())

    companion object {
        val Null = NamedFilter(
            id = "",
            filter = Filter.Null,
            isTag = false,
            timestamp = Timestamp.Null
        )

        fun anonymous(filter: Filter) = Null.copy(filter = filter)

        operator fun invoke(
            id: FilterId,
            filter: Filter,
            isTag: Boolean,
            timestamp: Timestamp = Timestamp.now
        ) = NamedFilter(id, filter, isTag, timestamp)
    }
}