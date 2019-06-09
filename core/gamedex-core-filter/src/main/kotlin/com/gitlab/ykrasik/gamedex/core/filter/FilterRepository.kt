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

import com.gitlab.ykrasik.gamedex.app.api.filter.FilterId
import com.gitlab.ykrasik.gamedex.app.api.filter.NamedFilter
import com.gitlab.ykrasik.gamedex.app.api.filter.NamedFilterData
import com.gitlab.ykrasik.gamedex.core.storage.Storage
import com.gitlab.ykrasik.gamedex.core.util.ListObservableImpl
import com.gitlab.ykrasik.gamedex.util.logger
import com.gitlab.ykrasik.gamedex.util.time

/**
 * User: ykrasik
 * Date: 24/06/2018
 * Time: 16:22
 */
class FilterRepository(
    private val name: String,
    private val storage: Storage<FilterId, NamedFilterData>
) {
    private val log = logger()

    val filters = ListObservableImpl(fetchFilters())

    private fun fetchFilters(): List<NamedFilter> =
        log.time("Fetching $name filters...", { time, filters -> "${filters.size} $name filters in $time" }) {
            storage.getAll().map { (id, data) -> NamedFilter(id, data) }
        }

    fun add(data: NamedFilterData): NamedFilter {
        val updatedData = data.createdNow()
        val id = storage.add(updatedData)
        val filter = NamedFilter(id, updatedData)
        filters += filter
        return filter
    }

    fun update(filter: NamedFilter, data: NamedFilterData): NamedFilter {
        val updatedData = data.updatedNow()
        storage.update(filter.id, updatedData)
        val updatedFilter = filter.copy(data = updatedData)
        filters.replace(filter, updatedFilter)
        return updatedFilter
    }

    fun delete(filter: NamedFilter) {
        storage.delete(filter.id)
        filters -= filter
    }

    fun invalidate() {
        // Re-fetch all filters from storage
        filters.setAll(fetchFilters())
    }
}