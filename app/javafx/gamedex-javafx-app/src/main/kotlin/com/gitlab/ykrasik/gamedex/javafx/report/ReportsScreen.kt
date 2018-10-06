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

package com.gitlab.ykrasik.gamedex.javafx.report

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.report.*
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.dialog.areYouSureDialog
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableScreen
import javafx.geometry.Pos
import javafx.scene.control.Toggle
import javafx.scene.control.ToggleGroup
import javafx.scene.control.ToolBar
import tornadofx.*

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 16:25
 */
class ReportsScreen : PresentableScreen("Reports", Theme.Icon.chart()),
    ViewWithReports, ViewCanAddReport, ViewCanEditReport, ViewCanExcludeGameFromReport, ViewCanDeleteReport {
    private val reportView: JavaFxReportView by inject()

    override val reports = mutableListOf<Report>().observable()
    override val addReportActions = channel<Unit>()
    override val editReportActions = channel<Report>()
    override val excludeGameActions = channel<Pair<Report, Game>>()
    override val deleteReportActions = channel<Report>()

    private val selection = ToggleGroup()
    private val currentToggle get() = selection.selectedToggleProperty()
    private val currentReport = currentToggle.map { it?.report }

    private var isRenderingReports = false
    private var ignoreNextToggleChange = false

    override val root = stackpane {
        addComponent(reportView) {
            root.hiddenWhen { currentReport.isNull }
        }
        maskerPane {
            visibleWhen { reportView.calculatingReportProperty }
            progressProperty().bind(reportView.calculatingReportProgressProperty)
        }
    }

    init {
        viewRegistry.register(this)

        selection.selectedToggleProperty().addListener { _, oldToggle, newToggle ->
            if (oldToggle != null && newToggle == null) {
                // This piece of code prevents a toggle from being 'unselected', as we always want one active.
                // It will get called also when we are re-constructing the screens after a change to the settings,
                // where it must allow the old toggle to be unselected (it is about to be dropped completely).
                if (!isRenderingReports) {
                    ignoreNextToggleChange = true
                    selection.selectToggle(oldToggle)
                    ignoreNextToggleChange = false
                }
            } else {
                if (!ignoreNextToggleChange) {
                    reportView.report = newToggle.report
                }
            }
        }
    }

    override fun ToolBar.constructToolbar() {
        buttonWithPopover("None", graphic = Theme.Icon.chart()) {
            reports.perform { reports ->
                val prevReport = currentReport.value

                isRenderingReports = true
                selection.toggles.clear()

                replaceChildren {
                    gridpane {
                        hgap = 5.0
                        reports.forEach { report ->
                            row {
                                jfxToggleNode(report.name, Theme.Icon.chart(), group = selection) {
                                    useMaxWidth = true
                                    userData = report
                                    isSelected = this == currentToggle.value
                                }
                                jfxButton(graphic = Theme.Icon.edit()) {
                                    eventOnAction(editReportActions) { report }
                                }
                                jfxButton(graphic = Theme.Icon.delete()) {
                                    addClass(CommonStyle.deleteButton)
                                    eventOnAction(deleteReportActions) { report }
                                }
                            }
                        }
                    }
                    separator()
                    addButton {
                        useMaxWidth = true
                        alignment = Pos.CENTER
                        addClass(CommonStyle.thinBorder)
                        eventOnAction(addReportActions)
                    }
                }

                isRenderingReports = false

                if (prevReport != null) {
                    // Re-select the previous report if it wasn't deleted.
                    selection.selectToggle(selection.toggles.find { it.report.id == prevReport.id })
                }
            }
        }.apply {
            addClass(CommonStyle.thinBorder)
            alignment = Pos.CENTER_LEFT
            textProperty().bind(currentReport.map { it?.name ?: "Select Report" })
        }
        verticalSeparator()
        searchField(this@ReportsScreen, reportView.searchTextProperty) { isFocusTraversable = false }
        verticalSeparator()
        spacer()
        verticalSeparator()
        excludeButton {
            val currentlySelectedGame = reportView.selectedGameProperty
            textProperty().bind(currentlySelectedGame.map { if (it != null) "Exclude ${it.name}" else "Exclude" })
            enableWhen { currentlySelectedGame.isNotNull }
            eventOnAction(excludeGameActions) { currentReport.value!! to currentlySelectedGame.value }
        }
        verticalSeparator()
    }

    override fun confirmDelete(report: Report) =
    // TODO: Display a read-only view of the rules instead.
        areYouSureDialog("Delete report '${report.name}'?") { label("Rules: ${report.filter}") }

    private val Toggle.report get() = userData as Report
}