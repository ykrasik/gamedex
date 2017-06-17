package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.core.RuleResult
import com.gitlab.ykrasik.gamedex.ui.jfxButton
import com.gitlab.ykrasik.gamedex.ui.simpleColumn
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import com.gitlab.ykrasik.gamedex.ui.theme.toDisplayString
import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.layout.HBox
import tornadofx.*

/**
 * User: ykrasik
 * Date: 17/06/2017
 * Time: 15:53
 */
class ViolationsReportView : ReportView<RuleResult.Fail>("Violations Report", Theme.Icon.book()) {
    override val reportHeader get() = "Violations"
    override val ongoingReport get() = reportsController.violations

    override fun reportsView() = tableview<RuleResult.Fail> {
        makeIndexColumn().apply { addClass(CommonStyle.centered) }
        simpleColumn("Rule") { violation -> violation.rule }
        simpleColumn("Value") { violation -> violation.value.toDisplayString() }

        selectedGameProperty.onChange { selectedGame ->
            items = selectedGame?.let { ongoingReport.resultsProperty.value[it]!!.observable() }
            resizeColumnsToFitContent()
        }
    }

    override fun extraReportMenu(hbox: HBox) {
        hbox.jfxButton("Rules", Theme.Icon.edit()) {
            val open = SimpleBooleanProperty(false)
            setOnAction {
                open.value = true
                reportsController.editViolationRules()
                open.value = false
            }
            disableWhen { open }
        }
    }
}