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

package com.gitlab.ykrasik.gamedex.core.filter

import com.gitlab.ykrasik.gamedex.app.api.filter.FilterId
import com.gitlab.ykrasik.gamedex.app.api.filter.NamedFilter
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
    private val storage: Storage<FilterId, NamedFilter>
) {
    private val log = logger()

    val filters = ListObservableImpl(fetchFilters())

    private fun fetchFilters(): List<NamedFilter> =
        log.time("Fetching $name filters...", { time, filters -> "${filters.size} $name filters in $time" }) {
            storage.getAll().values.toList()
        }

    fun set(filter: NamedFilter): NamedFilter {
        val existingFilterIndex = filters.indexOfFirst { it.id == filter.id }
        val updatedFilter = if (existingFilterIndex != -1) {
            filter.updatedNow()
        } else {
            filter.createdNow()
        }
        storage[filter.id] = updatedFilter

        if (existingFilterIndex != -1) {
            filters[existingFilterIndex] = updatedFilter
        } else {
            filters += updatedFilter
        }
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