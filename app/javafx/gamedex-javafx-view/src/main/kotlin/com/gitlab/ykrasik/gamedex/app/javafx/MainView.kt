/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.app.javafx

import com.gitlab.ykrasik.gamedex.app.api.common.ViewCanShowAboutView
import com.gitlab.ykrasik.gamedex.app.api.game.ViewGameParams
import com.gitlab.ykrasik.gamedex.app.api.report.Report
import com.gitlab.ykrasik.gamedex.app.api.settings.ViewCanShowSettings
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.game.JavaFxGameScreen
import com.gitlab.ykrasik.gamedex.app.javafx.game.details.JavaFxGameDetailsView
import com.gitlab.ykrasik.gamedex.app.javafx.library.LibraryMenu
import com.gitlab.ykrasik.gamedex.app.javafx.log.JavaFxLogScreen
import com.gitlab.ykrasik.gamedex.app.javafx.maintenance.JavaFxDuplicatesScreen
import com.gitlab.ykrasik.gamedex.app.javafx.maintenance.JavaFxFolderNameDiffScreen
import com.gitlab.ykrasik.gamedex.app.javafx.maintenance.MaintenanceMenu
import com.gitlab.ykrasik.gamedex.app.javafx.provider.JavaFxSyncGamesScreen
import com.gitlab.ykrasik.gamedex.app.javafx.report.JavaFxReportScreen
import com.gitlab.ykrasik.gamedex.app.javafx.report.ReportMenu
import com.gitlab.ykrasik.gamedex.app.javafx.task.JavaFxTaskView
import com.gitlab.ykrasik.gamedex.javafx.callOnDock
import com.gitlab.ykrasik.gamedex.javafx.callOnUndock
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.importStylesheetSafe
import com.gitlab.ykrasik.gamedex.javafx.theme.GameDexStyle
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableScreen
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import com.gitlab.ykrasik.gamedex.util.humanReadableDuration
import com.jfoenix.controls.JFXButton
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.text.FontWeight
import tornadofx.*
import java.util.*

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 22:44
 */
class MainView : PresentableView("GameDex"), ViewCanShowSettings, ViewCanShowAboutView {
    val taskView: JavaFxTaskView by inject()

    private val gameScreen: JavaFxGameScreen by inject()
    private val libraryMenu: LibraryMenu by inject()
    private val syncGamesScreen: JavaFxSyncGamesScreen by inject()
    private val reportMenu: ReportMenu by inject()
    private val reportScreen: JavaFxReportScreen by inject()
    private val duplicatesScreen: JavaFxDuplicatesScreen by inject()
    private val folderNameDiffScreen: JavaFxFolderNameDiffScreen by inject()
    private val maintenanceMenu: MaintenanceMenu by inject()
    private val logScreen: JavaFxLogScreen by inject()

    private val gameDetailsView: JavaFxGameDetailsView by inject()

    private val toolbars = mutableMapOf<PresentableScreen, HBox>()

    override val showSettingsActions = channel<Unit>()
    override val showAboutActions = channel<Unit>()

    private val maxNavigationHistory = 5
    private val navigationHistory = ArrayDeque<Tab>(maxNavigationHistory)

    private val toolbar = customToolbar {
        children.onChange {
            fade(0.6.seconds, 1.0, play = true) {
                fromValue = 0.0
            }
        }
    }

    private val tabPane = jfxTabPane {
        addClass(GameDexStyle.hiddenTabPaneHeader)
        isDisableAnimation = true   // Too slow by default

        tab(gameScreen).select()
        tab(syncGamesScreen)
        tab(reportScreen)
        tab(duplicatesScreen)
        tab(folderNameDiffScreen)
        tab(logScreen)
        tab(gameDetailsView)

        selectionModel.selectedItemProperty().addListener { _, oldValue, newValue ->
            cleanupClosedTab(oldValue)
            prepareNewTab(newValue)
        }
    }

    override val root = taskView.init {
        borderpane {
            top = toolbar
            center = tabPane
        }
    }

    private fun TabPane.tab(screen: PresentableScreen) = tab(screen) { userData = screen }

    private val mainNavigationButton = popOverMenu(graphic = Icons.menu) {
        navigationButton(gameScreen)

        verticalGap(size = 15)

        subMenu(libraryMenu)
        subMenu(reportMenu)
        subMenu(maintenanceMenu) { maintenanceMenu.init(this) }

        verticalGap(size = 15)

        navigationButton(logScreen)
        navigationButton("Settings", Icons.settings) {
            action(showSettingsActions)
            shortcut("ctrl+o")
            tooltip("Settings (ctrl+o)")
        }

        verticalGap(size = 15)

        navigationButton("About", Icons.information) {
            action(showAboutActions)
        }
        navigationButton("Quit", Icons.quit) {
            action {
                System.exit(0)
            }
        }
    }.apply {
        addClass(GameDexStyle.toolbarButton, Style.navigationButton)
        textProperty().bind(tabPane.selectionModel.selectedItemProperty().stringBinding { it!!.text })
    }

    init {
        register()
        prepareNewTab(tabPane.selectionModel.selectedItem)

        whenDockedOnce {
            val applicationStartTime = System.currentTimeMillis() - params[StartTimeParam] as Long
            log.info("Total application start time: ${applicationStartTime.humanReadableDuration}")
        }
    }

    private fun cleanupClosedTab(tab: Tab) {
        val screen = tab.screen
        screen.callOnUndock()
    }

    private fun prepareNewTab(tab: Tab) {
        addToHistory(tab)
        val screen = tab.screen
        screen.callOnDock()
        populateToolbar(screen)
    }

    private fun addToHistory(tab: Tab) {
        navigationHistory.push(tab)
        if (navigationHistory.size > maxNavigationHistory) {
            navigationHistory.pollLast()
        }
    }

    private fun populateToolbar(screen: PresentableScreen) {
        toolbar.replaceChildren {
            children += screen.customNavigationButton ?: mainNavigationButton
            gap()
            children += toolbars.getOrPut(screen) {
                HBox().apply {
                    spacing = 10.0
                    useMaxWidth = true
                    hgrow = Priority.ALWAYS
                    alignment = Pos.CENTER_LEFT
                    with(screen) { buildToolbar() }
                }
            }
        }
    }

    private fun PopOverContent.navigationButton(screen: PresentableScreen) =
        navigationButton(screen.title, screen.icon) {
            action {
                selectScreen(screen)
            }
        }

    private inline fun PopOverContent.navigationButton(text: String, icon: Node, crossinline op: JFXButton.() -> Unit = {}) =
        jfxButton(text, icon, alignment = Pos.CENTER_LEFT) {
            useMaxWidth = true
            op()
        }

    private inline fun PopOverMenu.subMenu(
        view: PresentableView,
        crossinline op: PopOverMenu.() -> Unit = { children += view.root }
    ): HBox = popOverSubMenu(view.title, view.icon, op = op)

    fun showGameDetails(params: ViewGameParams): JavaFxGameDetailsView = selectScreen(gameDetailsView) {
        this.gameParams.valueFromView = params
    }

    fun showSyncGamesView(): JavaFxSyncGamesScreen = selectScreen(syncGamesScreen)

    fun showReportView(report: Report): JavaFxReportScreen = selectScreen(reportScreen) {
        this.report.valueFromView = report
    }

    fun showDuplicatesReport(): JavaFxDuplicatesScreen = selectScreen(duplicatesScreen)
    fun showFolderNameDiffReport(): JavaFxFolderNameDiffScreen = selectScreen(folderNameDiffScreen)

    fun showPreviousScreen() {
        navigationHistory.pop()   // The current screen being shown is at the top of the stack.
        tabPane.selectionModel.select(navigationHistory.pop()) // Pop again, because the selection change will push it right back in.
    }

    private inline fun <V : PresentableScreen> selectScreen(screen: V, op: V.() -> Unit = {}): V {
        op(screen)
        tabPane.selectionModel.select(screen.tab)
        return screen
    }

    private val Tab.screen: PresentableScreen get() = userData as PresentableScreen
    private val PresentableScreen.tab: Tab get() = tabPane.tabs.find { it.screen == this }!!

    class Style : Stylesheet() {
        companion object {
            val navigationButton by cssclass()

            init {
                importStylesheetSafe(Style::class)
            }
        }

        init {
            navigationButton {
                fontWeight = FontWeight.BOLD
            }
        }
    }

    companion object {
        const val StartTimeParam = "GameDex.startTime"
    }
}
