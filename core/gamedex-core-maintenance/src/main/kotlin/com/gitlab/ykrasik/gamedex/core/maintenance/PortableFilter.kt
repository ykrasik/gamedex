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

package com.gitlab.ykrasik.gamedex.core.maintenance

import com.gitlab.ykrasik.gamedex.Timestamp
import com.gitlab.ykrasik.gamedex.app.api.filter.NamedFilter
import com.gitlab.ykrasik.gamedex.util.dateTime

/**
 * User: ykrasik
 * Date: 28/06/2019
 * Time: 16:54
 */
object PortableFilter {
    data class Filters(
        val filters: List<Filter>,
    )

    data class Filter(
        val id: String,
        val filter: com.gitlab.ykrasik.gamedex.app.api.filter.Filter,
        val isTag: Boolean,
        val createDate: Long,
        val updateDate: Long,
    ) {
        fun toDomain() = NamedFilter(
            id = id,
            filter = filter,
            isTag = isTag,
            timestamp = Timestamp(createDate = createDate.dateTime, updateDate = updateDate.dateTime)
        )
    }

    fun NamedFilter.toPortable() = Filter(
        id = id,
        filter = filter,
        isTag = isTag,
        createDate = timestamp.createDate.millis,
        updateDate = timestamp.updateDate.millis
    )
}