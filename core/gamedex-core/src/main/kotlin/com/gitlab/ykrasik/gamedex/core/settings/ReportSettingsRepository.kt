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

import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter.Companion.not
import com.gitlab.ykrasik.gamedex.app.api.report.ReportConfig

/**
 * User: ykrasik
 * Date: 24/06/2018
 * Time: 16:22
 */
// TODO: Probably a more correct place to put this is PersistenceService
// TODO: Try to save a report-per-file.
class ReportSettingsRepository(factory: SettingsStorageFactory) : SettingsRepository<ReportSettingsRepository.Data>() {
    data class Data(
        val reports: Map<String, ReportConfig>
    ) {
        inline fun modifyReports(f: (Map<String, ReportConfig>) -> Map<String, ReportConfig>) =
            copy(reports = f(reports))

        inline fun modifyReport(name: String, f: (ReportConfig) -> ReportConfig) =
            modifyReports { it + (name to f(it[name]!!)) }
    }

    override val storage = factory("report", Data::class) {
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

    val reportsChannel = storage.channel(Data::reports)
    val reports by reportsChannel

    private companion object {
        val noCriticScore = Filter.CriticScore(Filter.ScoreRule.NoScore)
        val noUserScore = Filter.UserScore(Filter.ScoreRule.NoScore)
    }
}