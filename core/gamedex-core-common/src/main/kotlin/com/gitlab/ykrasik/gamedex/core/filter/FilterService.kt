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

package com.gitlab.ykrasik.gamedex.core.filter

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.TagId
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.app.api.filter.FilterId
import com.gitlab.ykrasik.gamedex.app.api.filter.NamedFilter
import com.gitlab.ykrasik.gamedex.core.CoreEvent
import com.gitlab.ykrasik.gamedex.core.task.Task
import com.gitlab.ykrasik.gamedex.core.util.ListObservable

/**
 * User: ykrasik
 * Date: 03/10/2018
 * Time: 22:53
 */
interface FilterService {
    val userFilters: ListObservable<NamedFilter>

    operator fun get(id: FilterId): NamedFilter

    fun save(filter: NamedFilter): Task<NamedFilter>
    fun saveAll(filters: List<NamedFilter>): Task<List<NamedFilter>>

    fun delete(filter: NamedFilter): Task<Unit>

    fun getSystemFilter(id: FilterId): Filter?
    fun putSystemFilter(id: FilterId, filter: Filter)
    fun getOrPutSystemFilter(id: FilterId, default: () -> Filter): Filter =
        getSystemFilter(id) ?: default().also { putSystemFilter(id, it) }

    fun calcFilterTags(game: Game): List<TagId>

    fun filter(games: List<Game>, filter: Filter): List<Game>
}

sealed class FilterEvent : CoreEvent {
    data class Added(val filters: List<NamedFilter>) : FilterEvent()
    data class Deleted(val filters: List<NamedFilter>) : FilterEvent()
    data class Updated(val filters: List<Pair<NamedFilter, NamedFilter>>) : FilterEvent()
}