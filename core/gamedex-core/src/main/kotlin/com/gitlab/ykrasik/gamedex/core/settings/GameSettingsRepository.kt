/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.core.settings

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.app.api.game.DiscoverGameChooseResults
import com.gitlab.ykrasik.gamedex.app.api.game.Sort
import com.gitlab.ykrasik.gamedex.app.api.game.SortBy
import com.gitlab.ykrasik.gamedex.app.api.game.SortOrder
import com.gitlab.ykrasik.gamedex.app.api.util.combineLatest
import org.joda.time.Period
import org.joda.time.PeriodType

/**
 * User: ykrasik
 * Date: 01/05/2017
 * Time: 19:08
 */
class GameSettingsRepository : SettingsRepository<GameSettingsRepository.Data>("game", Data::class) {
    data class Data(
        val platform: Platform,
        val platformSettings: Map<Platform, GamePlatformSettings>,
        val sort: Sort,
        val discoverGameChooseResults: DiscoverGameChooseResults,
        val stalePeriod: Period
    ) {
        fun withPlatFormSettings(f: Map<Platform, GamePlatformSettings>.() -> Map<Platform, GamePlatformSettings>) =
            copy(platformSettings = f(platformSettings))
    }

    override fun defaultSettings() = Data(
        platform = Platform.pc,
        platformSettings = emptyMap(),
        sort = Sort(
            sortBy = SortBy.criticScore,
            order = SortOrder.desc
        ),
        discoverGameChooseResults = DiscoverGameChooseResults.chooseIfNonExact,
        stalePeriod = Period.months(2).normalizedStandard(PeriodType.yearMonthDayTime())
    )

    val platformChannel = channel(Data::platform)
    val platform by platformChannel

    val platformSettingsChannel = channel(Data::platformSettings)
    val platformSettings by platformSettingsChannel

    val currentPlatformSettingsChannel = platformChannel.combineLatest(platformSettingsChannel).distinctUntilChanged().map { (platform, settings) ->
        settings[platform] ?: GamePlatformSettings(Filter.`true`, "")
    }
    val currentPlatformSettings by currentPlatformSettingsChannel

    fun modifyCurrentPlatformSettings(f: GamePlatformSettings.() -> GamePlatformSettings) = modify {
        withPlatFormSettings {
            val currentPlatformSettings = this[platform] ?: GamePlatformSettings(Filter.`true`, "")
            this + (platform to f(currentPlatformSettings))
        }
    }

    val sortChannel = channel(Data::sort)
    val sort by sortChannel

    val discoverGameChooseResultsChannel = channel(Data::discoverGameChooseResults)
    val discoverGameChooseResults by discoverGameChooseResultsChannel

    val stalePeriodChannel = channel(Data::stalePeriod)
    val stalePeriod by stalePeriodChannel
}

data class GamePlatformSettings(
    val filter: Filter,
    val search: String
)