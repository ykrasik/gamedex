//package com.gitlab.ykrasik.gamedex.ui.view.report
//
//import com.gitlab.ykrasik.gamedex.report.RuleResult
//import com.gitlab.ykrasik.gamedex.ui.jfxButton
//import com.gitlab.ykrasik.gamedex.ui.simpleColumn
//import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
//import com.gitlab.ykrasik.gamedex.ui.theme.Theme
//import com.sun.org.apache.bcel.internal.Repository.addClass
//import javafx.beans.property.SimpleBooleanProperty
//import javafx.scene.layout.HBox
//import tornadofx.*
//
///**
// * User: ykrasik
// * Date: 17/06/2017
// * Time: 15:53
// */
//class ViolationsReportView : ReportFragment<RuleResult.Fail>("Violations Report", Theme.Icon.book()) {
//    override val reportHeader get() = "Violations"
//    override val ongoingReport get() = reportsController.violations
//
//    override fun reportsView() = tableview<RuleResult.Fail> {
//        makeIndexColumn().apply { addClass(CommonStyle.centered) }
//        simpleColumn("Rule") { violation -> violation.rule }
//        simpleColumn("Value") { violation -> violation.value.toDisplayString() }
//
//        selectedGameProperty.onChange { selectedGame ->
//            items = selectedGame?.let { ongoingReport.resultsProperty.value[it]!!.observable() }
//            resizeColumnsToFitContent()
//        }
//    }
//}