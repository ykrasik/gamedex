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
import com.gitlab.ykrasik.gamedex.app.api.game.DiscoverGameChooseResults
import com.gitlab.ykrasik.gamedex.app.api.game.SortBy
import com.gitlab.ykrasik.gamedex.app.api.game.SortOrder
import org.joda.time.Period
import org.joda.time.PeriodType

/**
 * User: ykrasik
 * Date: 01/05/2017
 * Time: 19:08
 */
class GameSettingsRepository(factory: SettingsStorageFactory) : SettingsRepository<GameSettingsRepository.Data>() {
    data class Data(
        val platform: Platform,
        val sortBy: SortBy,
        val sortOrder: SortOrder,
        val discoverGameChooseResults: DiscoverGameChooseResults,
        val redownloadCreatedBeforePeriod: Period,
        val redownloadUpdatedAfterPeriod: Period
    )

    override val storage = factory("game", Data::class) {
        Data(
            platform = Platform.pc,
            sortBy = SortBy.criticScore,
            sortOrder = SortOrder.desc,
            discoverGameChooseResults = DiscoverGameChooseResults.chooseIfNonExact,
            redownloadCreatedBeforePeriod = Period.months(2).normalizedStandard(PeriodType.yearMonthDayTime()),
            redownloadUpdatedAfterPeriod = Period.months(2).normalizedStandard(PeriodType.yearMonthDayTime())
        )
    }

    val platformChannel = storage.channel(Data::platform)
    val platform by platformChannel

    val sortByChannel = storage.channel(Data::sortBy)
    val sortBy by sortByChannel

    val sortOrderChannel = storage.channel(Data::sortOrder)
    val sortOrder by sortOrderChannel

    val discoverGameChooseResultsChannel = storage.channel(Data::discoverGameChooseResults)
    val discoverGameChooseResults by discoverGameChooseResultsChannel

    val redownloadCreatedBeforePeriodChannel = storage.channel(Data::redownloadCreatedBeforePeriod)
    val redownloadCreatedBeforePeriod by redownloadCreatedBeforePeriodChannel

    val redownloadUpdatedAfterPeriodChannel = storage.channel(Data::redownloadUpdatedAfterPeriod)
    val redownloadUpdatedAfterPeriod by redownloadUpdatedAfterPeriodChannel
}