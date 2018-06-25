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
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.Toggle
import javafx.scene.control.ToggleGroup
import javafx.scene.control.ToolBar
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import tornadofx.*

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 16:25
 */
class ReportsScreen : PresentableScreen("Reports", Theme.Icon.chart()),
    ViewWithReports, ViewCanAddReport, ViewCanEditReport, ViewCanExcludeGameFromReport, ViewCanDeleteReport {
    override val reports = mutableListOf<ReportConfig>().observable()

    override val addReportActions = channel<Unit>()
    override val editReportActions = channel<ReportConfig>()
    override val excludeGameActions = channel<Pair<String, Game>>()
    override val deleteReportActions = channel<ReportConfig>()

    private var content: VBox by singleAssign()
    private val searchTextProperty = SimpleStringProperty("")

    private val selection = ToggleGroup()
    private val screens = FXCollections.observableArrayList<JavaFxReportView>()

    private val currentToggle get() = selection.selectedToggleProperty()
    private val currentScreen = currentToggle.map { it?.userData as? JavaFxReportView }
    private val currentReport = currentScreen.map { it?.report }
    private val currentReportConfig = currentScreen.map { it?.reportConfig }
    private val currentlySelectedGame = currentScreen.flatMap {
        it?.selectedGameProperty ?: SimpleObjectProperty<Game>()
    }

    private var isChangingSettings = false

    override val root = stackpane {
        this@ReportsScreen.content = vbox()
        maskerPane {
            visibleWhen { currentReport.flatMap { it?.isCalculatingProperty ?: false.toProperty() } }
            currentScreen.onChange {
                progressProperty().cleanBind(currentReport.flatMap {
                    it?.progressProperty ?: ProgressIndicator.INDETERMINATE_PROGRESS.toProperty()
                })
            }
        }
    }

    init {
        viewRegistry.register(this)

        reports.perform { reports ->
            isChangingSettings = true

            // TODO: Check for listener leaks.
            cleanupClosedScreen(currentToggle.value)
            selection.toggles.clear()

            screens.setAll(reports.map { JavaFxReportView(it) })

            isChangingSettings = false

            // FIXME: listen to changes of any report & show it on change
        }

        selection.selectedToggleProperty().addListener { _, oldToggle, newToggle ->
            if (oldToggle != null && newToggle == null) {
                // This piece of code prevents a toggle from being 'unselected', as we always want one active.
                // It will get called also when we are re-constructing the screens after a change to the settings,
                // where it must allow the old toggle to be unselected (it is about to be dropped completely).
                if (!isChangingSettings) {
                    selection.selectToggle(oldToggle)
                }
            } else {
                cleanupClosedScreen(oldToggle)
                prepareNewScreen(newToggle)
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
                                    eventOnAction(editReportActions) { screen.reportConfig }
                                }
                                jfxButton(graphic = Theme.Icon.delete()) {
                                    addClass(CommonStyle.deleteButton)
                                    eventOnAction(deleteReportActions) { screen.reportConfig }
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
            textProperty().bind(currentReportConfig.map { it?.name ?: "Select Report" })
        }
        verticalSeparator()
        searchField(this@ReportsScreen, searchTextProperty) { isFocusTraversable = false }
        verticalSeparator()
        spacer()
        verticalSeparator()
        excludeButton {
            textProperty().bind(currentlySelectedGame.map { if (it != null) "Exclude ${it.name}" else "Exclude" })
            enableWhen { currentlySelectedGame.isNotNull }
            eventOnAction(excludeGameActions) { currentReportConfig.value!!.name to currentlySelectedGame.value }
        }
        verticalSeparator()
    }

    private fun upsertReport(f: () -> ReportConfig?) {
        val newConfig = f() ?: return
        // Select the newly upserted config.
        val newToggle = selection.toggles.find { (it.userData as JavaFxReportView).reportConfig == newConfig }
        selection.selectToggle(newToggle)
    }

    override fun onDock() {
        prepareNewScreen(currentToggle.value)
    }

    override fun onUndock() {
        cleanupClosedScreen(currentToggle.value)
    }

    override fun confirmDelete(reportConfig: ReportConfig) =
    // TODO: Display a read-only view of the rules instead.
        areYouSureDialog("Delete report '${reportConfig.name}'?") { label("Rules: ${reportConfig.filter}") }

    private fun cleanupClosedScreen(toggle: Toggle?) = toggle?.withScreen { screen ->
        searchTextProperty.unbindBidirectional(screen.searchProperty)
        screen.onUndock()
        content.clear()
    }

    private fun prepareNewScreen(toggle: Toggle?) = toggle?.withScreen { screen ->
        screen.onDock()
        searchTextProperty.bindBidirectional(screen.searchProperty)
        content.replaceChildren {
            children += screen.root.apply { vgrow = Priority.ALWAYS }
        }
    }

    private fun Toggle.withScreen(f: (JavaFxReportView) -> Unit) = f(userData as JavaFxReportView)
}