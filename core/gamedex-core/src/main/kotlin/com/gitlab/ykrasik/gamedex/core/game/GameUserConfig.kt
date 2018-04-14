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

package com.gitlab.ykrasik.gamedex.core.game

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.core.api.util.combineLatest
import com.gitlab.ykrasik.gamedex.core.api.util.mapBidirectional
import com.gitlab.ykrasik.gamedex.core.api.util.toBehaviorSubjectOnChange
import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfig
import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfigScope
import org.joda.time.Period
import org.joda.time.PeriodType
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 01/05/2017
 * Time: 19:08
 */
@Singleton
class GameUserConfig : UserConfig() {
    override val scope = UserConfigScope("game") {
        Data(
            displayType = DisplayType.wall,
            platform = Platform.pc,
            platformSettings = emptyMap(),
            sort = Sort(),
            chooseResults = ChooseResults.chooseIfNonExact,
            stalePeriod = Period.months(2).normalizedStandard(PeriodType.yearMonthDayTime())
        )
    }

    val displayTypeSubject = scope.subject(Data::displayType) { copy(displayType = it) }
    var displayType by displayTypeSubject

    val platformSubject = scope.subject(Data::platform) { copy(platform = it) }
    var platform by platformSubject

    val platformSettingsSubject = scope.subject(Data::platformSettings) { copy(platformSettings = it) }
    var platformSettings by platformSettingsSubject

    // TODO: Write unit tests for this.
    val currentPlatformSettingsSubject = platformSettingsSubject.combineLatest(platformSubject) { settings, platform ->
        settings.getOrElse(platform) { GamePlatformSettings(Filter.`true`) }
    }.toBehaviorSubjectOnChange {
        platformSettings += platform to it
    }
    var currentPlatformSettings by currentPlatformSettingsSubject

    val currentPlatformFilterSubject = currentPlatformSettingsSubject.mapBidirectional({ filter }, { GamePlatformSettings(this) })
    var currentPlatformFilter by currentPlatformFilterSubject

    val sortSubject = scope.subject(Data::sort) { copy(sort = it) }
    var sort by sortSubject

    val chooseResultsSubject = scope.subject(Data::chooseResults) { copy(chooseResults = it) }
    var chooseResults by chooseResultsSubject

    val stalePeriodSubject = scope.subject(Data::stalePeriod) { copy(stalePeriod = it) }
    var stalePeriod by stalePeriodSubject

    enum class DisplayType { wall, list }

    data class Sort(
        val sortBy: SortBy = SortBy.criticScore,
        val order: SortType = SortType.desc
    )

    enum class SortBy(val key: String) {
        name_("Name"),
        criticScore("Critic Score"),
        userScore("User Score"),
        minScore("Min Score"),
        avgScore("Average Score"),
        size("Size"),
        releaseDate("Release Date"),
        updateDate("Update Date");

        override fun toString() = key
    }

    enum class SortType {
        asc, desc;

        fun toggle(): SortType = when (this) {
            asc -> desc
            desc -> asc
        }
    }

    enum class ChooseResults(val description: String) {
        chooseIfNonExact("If no exact match: Choose"),
        alwaysChoose("Always choose"),
        skipIfNonExact("If no exact match: Skip"),
        proceedWithoutIfNonExact("If no exact match: Proceed Without")
    }

    data class Data(
        val displayType: DisplayType,
        val platform: Platform,
        val platformSettings: Map<Platform, GamePlatformSettings>,
        val sort: Sort,
        val chooseResults: ChooseResults,
        val stalePeriod: Period
    )
}

data class GamePlatformSettings(
    val filter: Filter
)