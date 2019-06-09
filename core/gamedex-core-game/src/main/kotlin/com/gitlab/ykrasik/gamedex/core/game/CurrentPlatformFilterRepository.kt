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

package com.gitlab.ykrasik.gamedex.core.game

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.app.api.util.MultiChannel
import com.gitlab.ykrasik.gamedex.app.api.util.MultiReceiveChannel
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.filter.FilterService
import com.gitlab.ykrasik.gamedex.core.maintenance.DatabaseInvalidatedEvent
import com.gitlab.ykrasik.gamedex.core.on
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 01/06/2019
 * Time: 22:56
 */
@Singleton
class CurrentPlatformFilterRepository @Inject constructor(
    private val settingsService: SettingsService,
    private val filterService: FilterService,
    eventBus: EventBus
) {
    private val _currentPlatformFilter = MultiChannel.conflated(Filter.Null)
    val currentPlatformFilter: MultiReceiveChannel<Filter> = _currentPlatformFilter.distinctUntilChanged(Filter::isEqual)

    init {
        eventBus.on<DatabaseInvalidatedEvent> {
            // Drop any filters we may currently have - they may be incorrect for the new database (point to non-existing libraries).
            // FIXME: Don't do this, instead allow filters to be invalid instead of throwing exceptions.
            Platform.values().forEach { platform ->
                filterService.putSystemFilter(filterName(platform), Filter.Null)
            }
        }

        Platform.values().forEach { platform ->
            // Init default filter for all platforms.
            filterService.getOrPutSystemFilter(filterName(platform)) { Filter.Null }
        }

        settingsService.game.platformChannel.subscribe { platform ->
            val filter = filterService.getSystemFilter(filterName(platform))!!
            _currentPlatformFilter.offer(filter)
        }
    }

    fun update(filter: Filter) {
        filterService.putSystemFilter(filterName(settingsService.game.platform), filter)
        _currentPlatformFilter.offer(filter)
    }

    private fun filterName(platform: Platform) = "${CurrentPlatformFilterRepository::class.qualifiedName!!}_$platform"
}