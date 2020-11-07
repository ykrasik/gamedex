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

package com.gitlab.ykrasik.gamedex.core.provider

import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.core.filter.FilterService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 31/12/2018
 * Time: 15:12
 */
@Singleton
class BulkSyncGamesFilterRepository @Inject constructor(
    private val filterService: FilterService,
) {
    private val _bulkSyncGamesFilter = MutableStateFlow(filterService.getOrPutSystemFilter(filterName) { Filter.Null })
    val bulkSyncGamesFilter: StateFlow<Filter> = _bulkSyncGamesFilter

    fun update(filter: Filter) {
        filterService.putSystemFilter(filterName, filter)
        _bulkSyncGamesFilter.value = filter
    }

    private companion object {
        val filterName = BulkSyncGamesFilterRepository::class.qualifiedName!!
    }
}