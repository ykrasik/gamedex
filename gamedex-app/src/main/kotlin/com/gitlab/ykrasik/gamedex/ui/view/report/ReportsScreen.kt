package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import com.gitlab.ykrasik.gamedex.ui.view.GamedexScreen
import javafx.geometry.Pos
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.ToolBar
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import org.controlsfx.control.textfield.CustomTextField
import org.controlsfx.control.textfield.TextFields
import tornadofx.*

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 16:25
 */
// TODO: Add games below a certain score, games without any (or not all) providers
class ReportsScreen : GamedexScreen("Reports", Theme.Icon.chart()) {
    private val violationsReportView: ViolationsReportView by inject()
    private val duplicateGamesReportView: DuplicateGamesReportView by inject()
    private val nameFolderDiffReportView: NameFolderDiffReportView by inject()

    private val searchTextfield = searchTextfield()
    private var tabPane: TabPane by singleAssign()
    private val extraMenuPlaceholder = HBox(5.0).apply {
        isFillHeight = true
        useMaxHeight = true
    }

    // Lazy because the tabPane, defined above, is only assigned later on.
    private val currentTab by lazy { tabPane.selectionModel.selectedItemProperty() }
    private val currentView by lazy { currentTab.map { it!!.userData as ReportView<*> } }
    private val currentReport by lazy { currentView.map { it!!.ongoingReport } }

    override val root: StackPane = stackpane {
        tabPane = tabpane {
            addClass(CommonStyle.tabbedNavigation)

            reportTab(violationsReportView)
            reportTab(duplicateGamesReportView)
            reportTab(nameFolderDiffReportView)

            selectionModel.selectedItemProperty().addListener { _, closedTab, openedTab ->
                cleanupClosedTab(closedTab)
                prepareNewTab(openedTab)
            }
        }
        maskerPane {
            visibleWhen { currentReport.flatMap { it!!.isCalculatingProperty } }
            currentTab.onChange {
                progressProperty().cleanBind(currentReport.flatMap { it!!.progressProperty })
            }
        }
    }

    override fun ToolBar.constructToolbar() {
        items += searchTextfield
        verticalSeparator()
        items += extraMenuPlaceholder
        spacer()

        verticalSeparator()
        togglegroup {
            tabPane.tabs.forEach { tab ->
                jfxToggleNode(tab.text, tab.graphic) {
                    isSelected = tab == currentTab.value
                    setOnAction { tabPane.selectionModel.select(tab) }
                }
                separator()
            }

            selectedToggleProperty().addListener { _, oldValue, newValue ->
                if (oldValue != null && newValue == null) selectToggle(oldValue)
            }
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

    private fun TabPane.reportTab(view: View) = tab(view) {
        userData = view
        graphic = view.icon
    }

    override fun onDock() {
        skipFirstTime {
            // This is called on application startup, but we don't want to dock any of the child views
            // (which will cause them to start calculating stuff)  before the user explicitly enters the reports screen.
            // A bit of a hack.
            prepareNewTab(currentTab.value)
        }
    }

    override fun onUndock() {
        cleanupClosedTab(currentTab.value)
    }

    private fun cleanupClosedTab(tab: Tab) = tab.withView { view ->
        searchTextfield.textProperty().unbindBidirectional(view.searchProperty)
        view.onUndock()
    }

    private fun prepareNewTab(tab: Tab) = tab.withView { view ->
        view.onDock()
        searchTextfield.textProperty().bindBidirectional(view.searchProperty)
        extraMenuPlaceholder.replaceChildren { view.extraReportMenu(this) }
    }

    private fun Tab.withView(f: (ReportView<*>) -> Unit) = f(userData as ReportView<*>)

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