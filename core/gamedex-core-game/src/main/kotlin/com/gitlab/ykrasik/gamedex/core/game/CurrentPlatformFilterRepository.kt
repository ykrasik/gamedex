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

package com.gitlab.ykrasik.gamedex.core.game

import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.app.api.game.AvailablePlatform
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.filter.FilterService
import com.gitlab.ykrasik.gamedex.core.flowOf
import com.gitlab.ykrasik.gamedex.core.maintenance.DatabaseInvalidatedEvent
import com.gitlab.ykrasik.gamedex.core.settings.GameSettingsRepository
import com.gitlab.ykrasik.gamedex.core.util.flowScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 01/06/2019
 * Time: 22:56
 */
@Singleton
class CurrentPlatformFilterRepository @Inject constructor(
    private val settingsRepo: GameSettingsRepository,
    private val filterService: FilterService,
    eventBus: EventBus
) {
    private val _currentPlatformFilter = MutableStateFlow(Filter.Null)
    val currentPlatformFilter: StateFlow<Filter> = _currentPlatformFilter

    init {
        flowScope(Dispatchers.IO) {
            eventBus.flowOf<DatabaseInvalidatedEvent>().forEach(debugName = "onDatabaseInvalidated") {
                // Drop any filters we may currently have - they may be incorrect for the new database (point to non-existing libraries).
                // FIXME: Don't do this, instead allow filters to be invalid instead of throwing exceptions.
                AvailablePlatform.values.forEach { platform ->
                    filterService.putSystemFilter(filterName(platform), Filter.Null)
                }
            }
        }

        AvailablePlatform.values.forEach { platform ->
            // Init default filter for all platforms.
            filterService.getOrPutSystemFilter(filterName(platform)) { Filter.Null }
        }

        flowScope(Dispatchers.Default) {
            _currentPlatformFilter *= settingsRepo.platform.map { platform ->
                filterService.getSystemFilter(filterName(platform))!!
            } withDebugName "onCurrentPlatformChanged"
        }
    }

    fun update(filter: Filter) {
        filterService.putSystemFilter(filterName(settingsRepo.platform.value), filter)
        _currentPlatformFilter.value = filter
    }

    private fun filterName(platform: AvailablePlatform) = "${CurrentPlatformFilterRepository::class.qualifiedName!!}_$platform"
}