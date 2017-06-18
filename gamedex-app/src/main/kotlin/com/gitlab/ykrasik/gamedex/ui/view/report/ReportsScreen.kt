package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.controller.ReportsController
import com.gitlab.ykrasik.gamedex.settings.ReportSettings
import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import com.gitlab.ykrasik.gamedex.ui.theme.addButton
import com.gitlab.ykrasik.gamedex.ui.theme.deleteButton
import com.gitlab.ykrasik.gamedex.ui.theme.editButton
import com.gitlab.ykrasik.gamedex.ui.view.GamedexScreen
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.Toggle
import javafx.scene.control.ToggleGroup
import javafx.scene.control.ToolBar
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import org.controlsfx.control.textfield.CustomTextField
import org.controlsfx.control.textfield.TextFields
import tornadofx.*

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 16:25
 */
class ReportsScreen : GamedexScreen("Reports", Theme.Icon.chart()) {
    private val reportSettings: ReportSettings by di()
    private val reportsController: ReportsController by di()

    private val searchTextfield = searchTextfield()
    private var content: VBox by singleAssign()
    private val selection = ToggleGroup()
    private val screens = FXCollections.observableArrayList<ReportFragment>()

    private val currentToggle get() = selection.selectedToggleProperty()
    private val currentScreen = currentToggle.map { it?.userData as? ReportFragment }
    private val currentReport = currentScreen.map { it?.ongoingReport }

    private var isReplacingScreens = false

    override val root: StackPane = stackpane {
        content = vbox()
        maskerPane {
            visibleWhen { currentReport.flatMap { it?.isCalculatingProperty ?: false.toProperty() } }
            currentScreen.onChange {
                progressProperty().cleanBind(currentReport.flatMap { it?.progressProperty ?: ProgressIndicator.INDETERMINATE_PROGRESS.toProperty() })
            }
        }
    }

    init {
        reportSettings.reportsProperty.perform { reports ->
            // TODO: Check for listener leaks.
            screens.forEach { it.onUndock() }
            selection.toggles.clear()

            screens.setAll(reports.map { ReportFragment(it.value) })
        }

        selection.selectedToggleProperty().addListener { _, oldToggle, newToggle ->
            println("${(oldToggle?.userData as? ReportFragment)?.reportConfig} -> ${(newToggle?.userData as? ReportFragment)?.reportConfig}")
            if (oldToggle != null && newToggle == null) {
                if (!isReplacingScreens) {
                    selection.selectToggle(oldToggle)
                }
            } else {
                cleanupClosedScreen(oldToggle)
                prepareNewScreen(newToggle)
            }
        }
    }

    override fun ToolBar.constructToolbar() {
        items += searchTextfield
        verticalSeparator()
        addButton {
            setOnAction {
                val newConfig = reportsController.addReport() ?: return@setOnAction
                // TODO: This is a dirty solution, move this logic out of here.
                isReplacingScreens = true
                reportSettings.reports += newConfig.name to newConfig
                isReplacingScreens = false
                val newToggle = selection.toggles.find { (it.userData as ReportFragment).reportConfig == newConfig }
                selection.selectToggle(newToggle)
            }
        }
        verticalSeparator()
        // TODO: Move these buttons to the context menu
        editButton {
            disableWhen { currentToggle.isNull }
            setOnAction {
                val editedConfig = reportsController.editReport(currentScreen.value!!.reportConfig) ?: return@setOnAction
                // TODO: This is a dirty solution, move this logic out of here.
                isReplacingScreens = true
                reportSettings.reports += editedConfig.name to editedConfig // TODO: Works incorrectly if name changes.
                isReplacingScreens = false
                val editedToggle = selection.toggles.find { (it.userData as ReportFragment).reportConfig == editedConfig }
                selection.selectToggle(editedToggle)
            }
        }
        verticalSeparator()
        deleteButton("Delete") {
            disableWhen { currentToggle.isNull }
            setOnAction { deleteReport(currentScreen.value!!) }
        }

        spacer()

        verticalSeparator()
        hbox(spacing = 5.0) {
            alignment = Pos.CENTER_RIGHT
            screens.performing { screens ->
                replaceChildren {
                    screens.forEach { screen ->
                        jfxToggleNode(screen.title, screen.icon, group = selection) {
                            userData = screen
                            isSelected = this == currentToggle.value
                        }
                    }
                }
            }
        }
    }

    // FIXME: Deleting the last report doesn't clear the screen.
    private fun deleteReport(view: ReportFragment) {
        if (reportsController.delete(view.reportConfig)) {
            // TODO: This is a dirty solution, move this logic out of here.
            isReplacingScreens = true
            reportSettings.reports -= view.reportConfig.name
            isReplacingScreens = false
            selection.selectToggle(selection.toggles.firstOrNull())
        }
    }

    private fun searchTextfield() = (TextFields.createClearableTextField() as CustomTextField).apply {
        promptText = "Search"
        left = Theme.Icon.search(18.0)
        isFocusTraversable = false

        addEventHandler(KeyEvent.KEY_PRESSED) { e ->
            if (e.code == KeyCode.ESCAPE) {
                text = ""
                root.requestFocus()
            }
        }
    }

    override fun onDock() {
        skipFirstTime {
            // This is called on application startup, but we don't want to dock any of the child views
            // (which will cause them to start calculating stuff)  before the user explicitly enters the reports screen.
            // A bit of a hack.
            prepareNewScreen(currentToggle.value)
        }
    }

    override fun onUndock() {
        cleanupClosedScreen(currentToggle.value)
    }

    private fun cleanupClosedScreen(toggle: Toggle?) = toggle?.withScreen { screen ->
        searchTextfield.textProperty().unbindBidirectional(screen.searchProperty)
        screen.onUndock()
    }

    private fun prepareNewScreen(toggle: Toggle?) = toggle?.withScreen { screen ->
        screen.onDock()
        searchTextfield.textProperty().bindBidirectional(screen.searchProperty)
        content.replaceChildren {
            children += screen.root.apply { vgrow = Priority.ALWAYS }
        }
    }

    private fun Toggle.withScreen(f: (ReportFragment) -> Unit) = f(userData as ReportFragment)

    class Style : Stylesheet() {
        companion object {
            val tabGraphic by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            tabGraphic {
                padding = box(vertical = 0.px, horizontal = 5.px)
                alignment = Pos.CENTER_LEFT
            }
        }
    }
}