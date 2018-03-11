package com.gitlab.ykrasik.gamedex.settings

import com.gitlab.ykrasik.gamedex.core.Filter
import com.gitlab.ykrasik.gamedex.core.Filter.Companion.not
import com.gitlab.ykrasik.gamedex.ui.view.report.ReportConfig
import tornadofx.getValue
import tornadofx.setValue
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 18/06/2017
 * Time: 11:33
 */
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

    val reportsProperty = repo.property(Data::reports) { copy(reports = it) }
    var reports by reportsProperty

    data class Data(
        val reports: Map<String, ReportConfig>
    )
}