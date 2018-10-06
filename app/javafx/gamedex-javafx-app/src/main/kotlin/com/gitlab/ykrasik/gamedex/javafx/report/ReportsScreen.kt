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
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.Toggle
import javafx.scene.control.ToggleGroup
import javafx.scene.control.ToolBar
import javafx.scene.layout.VBox
import tornadofx.*

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 16:25
 */
class ReportsScreen : PresentableScreen("Reports", Theme.Icon.chart()),
    ViewWithReports, ViewCanAddReport, ViewCanEditReport, ViewCanExcludeGameFromReport, ViewCanDeleteReport {
    override val reports = mutableListOf<Report>().observable()

    override val addReportActions = channel<Unit>()
    override val editReportActions = channel<Report>()
    override val excludeGameActions = channel<Pair<Report, Game>>()
    override val deleteReportActions = channel<Report>()

    private var content: VBox by singleAssign()
    private val searchTextProperty = SimpleStringProperty("")

    private val selection = ToggleGroup()
    private val screens = FXCollections.observableArrayList<JavaFxReportView>()

    private val currentToggle get() = selection.selectedToggleProperty()
    private val currentScreen = currentToggle.map { it?.userData as? JavaFxReportView }
    private val currentReport = currentScreen.map { it?.report }
    private val currentlySelectedGame = currentScreen.flatMap {
        it?.selectedGameProperty ?: SimpleObjectProperty<Game>()
    }

    override val root = stackpane {
        this@ReportsScreen.content = vbox()
        maskerPane {
            val calculatingReportProperty = currentScreen.flatMap { it?.calculatingReportProperty ?: false.toProperty() }
            val calculatingReportProgressProperty = currentScreen.flatMap { it?.calculatingReportProgressProperty ?: SimpleDoubleProperty(ProgressIndicator.INDETERMINATE_PROGRESS) }
            visibleWhen { calculatingReportProperty }
            progressProperty().bind(calculatingReportProgressProperty)
        }
    }

    init {
        viewRegistry.register(this)

        var isRenderingReports = false
        reports.perform { reports ->
            isRenderingReports = true

            val prevReport = currentReport.value

            // TODO: Check for listener leaks.
            cleanupClosedReportView(currentToggle.value)
            selection.toggles.clear()

            screens.setAll(reports.map { JavaFxReportView(it) })

            isRenderingReports = false

            if (prevReport != null) {
                reports.find { it.id == prevReport.id }?.let { report ->
                    // Re-select the previous report if it wasn't deleted.
                    selection.selectToggle(selection.toggles.find { it.reportView.report.id == report.id })
                }
            }
        }

        var ignoreNextToggleChange = false
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
                    cleanupClosedReportView(oldToggle)
                    prepareNewReportView(newToggle)
                }
            }
        }
    }

    override fun ToolBar.constructToolbar() {
        buttonWithPopover("None", graphic = Theme.Icon.chart()) {
            screens.perform { screens ->
                replaceChildren {
                    gridpane {
                        hgap = 5.0
                        screens.forEach { screen ->
                            row {
                                jfxToggleNode(screen.title, screen.icon, group = selection) {
                                    useMaxWidth = true
                                    userData = screen
                                    isSelected = this == currentToggle.value
                                }
                                jfxButton(graphic = Theme.Icon.edit()) {
                                    eventOnAction(editReportActions) { screen.report }
                                }
                                jfxButton(graphic = Theme.Icon.delete()) {
                                    addClass(CommonStyle.deleteButton)
                                    eventOnAction(deleteReportActions) { screen.report }
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
            }
        }.apply {
            addClass(CommonStyle.thinBorder)
            alignment = Pos.CENTER_LEFT
            textProperty().bind(currentReport.map { it?.name ?: "Select Report" })
        }
        verticalSeparator()
        searchField(this@ReportsScreen, searchTextProperty) { isFocusTraversable = false }
        verticalSeparator()
        spacer()
        verticalSeparator()
        excludeButton {
            textProperty().bind(currentlySelectedGame.map { if (it != null) "Exclude ${it.name}" else "Exclude" })
            enableWhen { currentlySelectedGame.isNotNull }
            eventOnAction(excludeGameActions) { currentReport.value!! to currentlySelectedGame.value }
        }
        verticalSeparator()
    }

    override fun onDock() {
        prepareNewReportView(currentToggle.value)
    }

    override fun onUndock() {
        cleanupClosedReportView(currentToggle.value)
    }

    override fun confirmDelete(report: Report) =
    // TODO: Display a read-only view of the rules instead.
        areYouSureDialog("Delete report '${report.name}'?") { label("Rules: ${report.filter}") }

    private fun cleanupClosedReportView(toggle: Toggle?) = toggle?.reportView?.let { reportView ->
        searchTextProperty.unbindBidirectional(reportView.searchProperty)
        // TODO: reportView.callOnUndock() throws noSuchMethod. Wtf, kotlin?
        reportView.onUndock()
        reportView.onUndockListeners?.forEach { it.invoke(this) }
        content.clear()
    }

    private fun prepareNewReportView(toggle: Toggle?) = toggle?.reportView?.let { reportView ->
        // TODO: reportView.callOnDock() throws noSuchMethod. Wtf, kotlin?
        reportView.onDock()
        reportView.onDockListeners?.forEach { it.invoke(this) }
        searchTextProperty.bindBidirectional(reportView.searchProperty)
        content.replaceChildren {
            addComponent(reportView)
        }
    }

    private val Toggle.reportView get() = userData as JavaFxReportView
}