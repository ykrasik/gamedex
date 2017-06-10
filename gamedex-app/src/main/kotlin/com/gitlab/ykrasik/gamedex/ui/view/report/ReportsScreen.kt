package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.ui.jfxToggleNode
import com.gitlab.ykrasik.gamedex.ui.skipFirstTime
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import com.gitlab.ykrasik.gamedex.ui.view.GamedexScreen
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.ToolBar
import tornadofx.*

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 16:25
 */
class ReportsScreen : GamedexScreen("Reports", Theme.Icon.chart()) {
    private val duplicateGamesView: DuplicateGamesView by inject()
    private val nameFolderMismatchView: NameFolderMismatchView by inject()

    override val root = tabpane {
        addClass(CommonStyle.tabbedNavigation)

        reportTab(duplicateGamesView)
        reportTab(nameFolderMismatchView)

        selectionModel.selectedItemProperty().addListener { _, oldValue, newValue ->
            cleanupClosedTab(oldValue)
            prepareNewTab(newValue)
        }
    }

    override fun ToolBar.constructToolbar() {
        spacer()
        togglegroup {
            root.tabs.forEach { tab ->
                val graphic = Label(tab.text).apply {
                    addClass(Style.tabGraphic)
                    graphic = tab.graphic
                }
                jfxToggleNode(graphic) {
                    isSelected = root.selectionModel.selectedItem == tab
                    setOnAction { root.selectionModel.select(tab) }
                }
                separator()
            }

            selectedToggleProperty().addListener { _, oldValue, newValue ->
                if (oldValue != null && newValue == null) {
                    selectToggle(oldValue)
                }
            }
        }
    }

    private fun TabPane.reportTab(view: View) = tab(view) {
        userData = view
        graphic = view.icon
    }

    private fun cleanupClosedTab(tab: Tab) = (tab.userData as View).onUndock()
    private fun prepareNewTab(tab: Tab) = (tab.userData as View).onDock()

    override fun onDock() {
        skipFirstTime {
            // This is called on application startup, but we don't want to dock any of the child views
            // (which will cause them to start calculating stuff)  before the user explicitly enters the reports screen.
            // A bit of a hack.
            prepareNewTab(root.selectionModel.selectedItem)
        }
    }

    override fun onUndock() {
        cleanupClosedTab(root.selectionModel.selectedItem)
    }

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