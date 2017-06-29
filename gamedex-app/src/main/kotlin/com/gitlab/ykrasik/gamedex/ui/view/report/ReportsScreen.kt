package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.controller.ReportsController
import com.gitlab.ykrasik.gamedex.core.ReportConfig
import com.gitlab.ykrasik.gamedex.settings.ReportSettings
import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.ui.theme.*
import com.gitlab.ykrasik.gamedex.ui.view.GamedexScreen
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import org.controlsfx.control.PopOver
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

    private var content: VBox by singleAssign()
    private var searchTextfield: TextField by singleAssign()

    private val selection = ToggleGroup()
    private val screens = FXCollections.observableArrayList<ReportFragment>()

    private val currentToggle get() = selection.selectedToggleProperty()
    private val currentScreen = currentToggle.map { it?.userData as? ReportFragment }
    private val currentReport = currentScreen.map { it?.report }
    private val currentReportConfig = currentScreen.map { it?.reportConfig }
    private val currentlySelectedGame = currentScreen.flatMap { it?.selectedGameProperty ?: SimpleObjectProperty<Game>() }

    private var isChangingSettings = false

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
            isChangingSettings = true

            // TODO: Check for listener leaks.
            cleanupClosedScreen(currentToggle.value)
            selection.toggles.clear()

            screens.setAll(reports.map { ReportFragment(it.value) })

            isChangingSettings = false
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
        searchTextfield = (TextFields.createClearableTextField() as CustomTextField).apply {
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
        items += searchTextfield
        verticalSeparator()
        addButton { setOnAction { upsertReport { reportsController.addReport() } } }
        verticalSeparator()
        excludeButton {
            val text = currentlySelectedGame.map { if (it != null) "Exclude ${it.name}" else "Exclude" }
            textProperty().bind(text)
            enableWhen { currentlySelectedGame.isNotNull }
            setOnAction { upsertReport { reportsController.excludeGame(currentReportConfig.value!!, currentlySelectedGame.value) }  }
        }
        verticalSeparator()
        spacer()
        verticalSeparator()
        hbox(spacing = 5.0) {
            alignment = Pos.CENTER_RIGHT
            // TODO: Check for listener leaks.
            screens.performing { screens ->
                replaceChildren {
                    screens.forEach { screen ->
                        jfxToggleNode(screen.title, screen.icon, group = selection) {
                            popoverContextMenu(PopOver.ArrowLocation.TOP_RIGHT) {
                                editButton {
                                    addClass(Style.reportContextMenu)
                                    setOnAction { upsertReport { reportsController.editReport(screen.reportConfig) } }
                                }
                                deleteButton("Delete") {
                                    addClass(Style.reportContextMenu)
                                    setOnAction { deleteReport(screen) }
                                }
                            }
                            userData = screen
                            isSelected = this == currentToggle.value
                        }
                    }
                }
            }
        }
    }

    private fun upsertReport(f: () -> ReportConfig?) {
        val newConfig = f() ?: return
        // Select the newly upserted config.
        val newToggle = selection.toggles.find { (it.userData as ReportFragment).reportConfig == newConfig }
        selection.selectToggle(newToggle)
    }

    private fun deleteReport(view: ReportFragment) {
        if (reportsController.deleteReport(view.reportConfig)) {
            selection.selectToggle(selection.toggles.firstOrNull())
        }
    }

    override fun onDock() {
        prepareNewScreen(currentToggle.value)
    }

    override fun onUndock() {
        cleanupClosedScreen(currentToggle.value)
    }

    private fun cleanupClosedScreen(toggle: Toggle?) = toggle?.withScreen { screen ->
        searchTextfield.textProperty().unbindBidirectional(screen.searchProperty)
        screen.onUndock()
        content.clear()
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
            val reportContextMenu by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            reportContextMenu {
                alignment = Pos.CENTER_LEFT
            }
        }
    }
}