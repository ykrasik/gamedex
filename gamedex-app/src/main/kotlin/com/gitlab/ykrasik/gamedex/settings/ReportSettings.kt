package com.gitlab.ykrasik.gamedex.settings

import com.gitlab.ykrasik.gamedex.core.ReportConfig
import com.gitlab.ykrasik.gamedex.core.ReportRule
import tornadofx.getValue
import tornadofx.setValue

/**
 * User: ykrasik
 * Date: 18/06/2017
 * Time: 11:33
 */
class ReportSettings : SettingsScope() {
    @Transient
    val reportsProperty = preferenceProperty(defaultReports)
    var reports by reportsProperty

    companion object {
        val defaultReports = listOf(
            ReportConfig("Name Diff", ReportRule.Rules.NameDiff()),
            ReportConfig("Duplications", ReportRule.Rules.Duplications()),
            ReportConfig("Low Score", ReportRule.Operators.And(
                ReportRule.Operators.And(ReportRule.Rules.CriticScore(60.0, greaterThan = false), ReportRule.Rules.HasCriticScore()),
                ReportRule.Operators.And(ReportRule.Rules.UserScore(60.0, greaterThan = false), ReportRule.Rules.HasUserScore())
            )),
            ReportConfig("No Score", ReportRule.Operators.And(
                ReportRule.Operators.Not(ReportRule.Rules.HasCriticScore()),
                ReportRule.Operators.Not(ReportRule.Rules.HasUserScore())
            )),
            ReportConfig("Missing Providers", ReportRule.Operators.Or(
                ReportRule.Operators.Not(ReportRule.Rules.HasProvider("GiantBomb")),
                ReportRule.Operators.Not(ReportRule.Rules.HasProvider("Igdb"))
            ))
        ).associateBy { it.name }
    }
}