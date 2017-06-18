package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.core.ReportRule
import com.gitlab.ykrasik.gamedex.ui.map
import com.gitlab.ykrasik.gamedex.ui.mapBidirectional
import com.gitlab.ykrasik.gamedex.ui.showWhen
import com.gitlab.ykrasik.gamedex.ui.theme.platformComboBox
import com.gitlab.ykrasik.gamedex.ui.widgets.adjustableTextField
import javafx.beans.property.ObjectProperty
import javafx.geometry.Pos
import javafx.scene.layout.HBox
import tornadofx.hbox

/**
 * User: ykrasik
 * Date: 18/06/2017
 * Time: 14:53
 */
object ReportRuleRenderer {
    fun HBox.platformFilter(currentRule: ObjectProperty<ReportRule>) {
        val platform = currentRule.mapBidirectional(
            { (it as? ReportRule.Filters.PlatformFilter)?.platform ?: Platform.pc }, { ReportRule.Filters.PlatformFilter(it!!) }
        )
        platformComboBox(platform).apply {
            showWhen { currentRule.map { it is ReportRule.Filters.PlatformFilter } }
        }
    }

    fun HBox.criticScoreRule(currentRule: ObjectProperty<ReportRule>) = hbox {
        alignment = Pos.CENTER_LEFT
        val value = currentRule.mapBidirectional(
            { (it as? ReportRule.Rules.CriticScore)?.min ?: 65.0 }, { ReportRule.Rules.CriticScore(it!!) }
        )
        adjustableTextField(value, "Critic Score", min = 0.0, max = 100.0)
    }.apply {
        showWhen { currentRule.map { it is ReportRule.Rules.CriticScore } }
    }

    fun HBox.userScoreRule(currentRule: ObjectProperty<ReportRule>) = hbox {
        alignment = Pos.CENTER_LEFT
        val value = currentRule.mapBidirectional(
            { (it as? ReportRule.Rules.UserScore)?.min ?: 65.0 }, { ReportRule.Rules.UserScore(it!!) }
        )
        adjustableTextField(value, "User Score", min = 0.0, max = 100.0)
    }.apply {
        showWhen { currentRule.map { it is ReportRule.Rules.UserScore } }
    }
}