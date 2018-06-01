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

package com.gitlab.ykrasik.gamedex.core.report

import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter.Companion.not
import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfig
import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfigScope
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 18/06/2017
 * Time: 11:33
 */
// TODO: Put reports in persistence & make a ReportRepository
@Singleton
class ReportUserConfig : UserConfig() {
    private val noCriticScore = Filter.CriticScore(Filter.ScoreRule.NoScore)
    private val noUserScore = Filter.UserScore(Filter.ScoreRule.NoScore)

    override val scope = UserConfigScope("report") {
        Data(
            reports = listOf(
                ReportConfig("Name Diff", Filter.NameDiff(), excludedGames = emptyList()),
                ReportConfig("Duplications", Filter.Duplications(), excludedGames = emptyList()),
                ReportConfig("Very Low Score",
                    Filter.CriticScore(60.0).not and noCriticScore.not and {
                        Filter.UserScore(60.0).not and noUserScore.not
                    }, excludedGames = emptyList()
                ),
                ReportConfig("Low Score",
                    Filter.CriticScore(60.0).not and noCriticScore.not or {
                        Filter.UserScore(60.0).not and noUserScore.not
                    }, excludedGames = emptyList()
                ),
                ReportConfig("Missing Score",
                    noCriticScore or noUserScore and not {
                        noCriticScore and noUserScore
                    }, excludedGames = emptyList()
                ),
                ReportConfig("No Score",
                    noCriticScore and noUserScore,
                    excludedGames = emptyList()
                ),
                ReportConfig("Missing Providers", not {
                    Filter.Provider("GiantBomb") and
                        Filter.Provider("Igdb")
                }, excludedGames = emptyList())
            ).associateBy { it.name }
        )
    }

    val reportsSubject = scope.subject(Data::reports) { copy(reports = it) }
    var reports by reportsSubject

    data class Data(
        val reports: Map<String, ReportConfig>
    )
}