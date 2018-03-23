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

package com.gitlab.ykrasik.gamedex.settings

import com.gitlab.ykrasik.gamedex.core.game.Filter
import com.gitlab.ykrasik.gamedex.core.game.Filter.Companion.not
import com.gitlab.ykrasik.gamedex.core.settings.SettingsRepo
import com.gitlab.ykrasik.gamedex.core.settings.UserSettings
import com.gitlab.ykrasik.gamedex.ui.view.report.ReportConfig
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 18/06/2017
 * Time: 11:33
 */
// TODO: Put reports in persistence & make a ReportRepository
@Singleton
class ReportSettings : UserSettings() {
    override val repo = SettingsRepo("report") {
        Data(
            reports = listOf(
                ReportConfig("Name Diff", Filter.NameDiff(), excludedGames = emptyList()),
                ReportConfig("Duplications", Filter.Duplications(), excludedGames = emptyList()),
                ReportConfig("Very Low Score",
                    Filter.CriticScore(60.0).not and Filter.noCriticScore.not and {
                        Filter.UserScore(60.0).not and Filter.noUserScore.not
                    }, excludedGames = emptyList()
                ),
                ReportConfig("Low Score",
                    Filter.CriticScore(60.0).not and Filter.noCriticScore.not or {
                        Filter.UserScore(60.0).not and Filter.noUserScore.not
                    }, excludedGames = emptyList()
                ),
                ReportConfig("Missing Score",
                    Filter.noCriticScore or Filter.noUserScore and not {
                        Filter.noCriticScore and Filter.noUserScore
                    }, excludedGames = emptyList()
                ),
                ReportConfig("No Score",
                    Filter.noCriticScore and Filter.noUserScore,
                    excludedGames = emptyList()
                ),
                ReportConfig("Missing Providers", not {
                    Filter.Provider("GiantBomb") and
                        Filter.Provider("Igdb")
                }, excludedGames = emptyList())
            ).associateBy { it.name }
        )
    }

    val reportsSubject = repo.subject(Data::reports) { copy(reports = it) }
    var reports by reportsSubject

    data class Data(
        val reports: Map<String, ReportConfig>
    )
}