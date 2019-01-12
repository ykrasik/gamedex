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

package com.gitlab.ykrasik.gamedex.javafx

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.navigation.ViewCanCloseOtherViews
import com.gitlab.ykrasik.gamedex.app.api.settings.ViewCanShowSettings
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.game.GameScreen
import com.gitlab.ykrasik.gamedex.app.javafx.game.details.JavaFxViewGameScreen
import com.gitlab.ykrasik.gamedex.app.javafx.log.JavaFxLogScreen
import com.gitlab.ykrasik.gamedex.app.javafx.maintenance.JavaFxMaintenanceView
import com.gitlab.ykrasik.gamedex.app.javafx.report.ReportsScreen
import com.gitlab.ykrasik.gamedex.app.javafx.task.JavaFxTaskView
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.provider.JavaFxSyncGamesScreen
import com.gitlab.ykrasik.gamedex.javafx.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.javafx.theme.backButton
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableScreen
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import com.gitlab.ykrasik.gamedex.javafx.view.ScreenNavigation
import com.gitlab.ykrasik.gamedex.util.toHumanReadableDuration
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.ContentDisplay
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.text.FontWeight
import tornadofx.*

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 22:44
 */
class MainView : PresentableView("GameDex"), ViewCanCloseOtherViews, ViewCanShowSettings {
    private val taskView: JavaFxTaskView by inject()

    private val gameScreen: GameScreen by inject()
    private val reportsScreen: ReportsScreen by inject()
    private val logScreen: JavaFxLogScreen by inject()

    private val maintenanceView: JavaFxMaintenanceView by inject()

    private val viewGameScreen: JavaFxViewGameScreen by inject()
    private val syncGamesScreen: JavaFxSyncGamesScreen by inject()

    private lateinit var previousScreen: Tab

    private val toolbars = mutableMapOf<PresentableScreen, HBox>()

    override val closeViewActions = channel<Any>()
    override val showSettingsActions = channel<Unit>()

    init {
        register()
    }

    private val toolbar = customToolbar {
        children.onChange {
            fade(0.6.seconds, 1.0, play = true) {
                fromValue = 0.0
            }
        }
    }

    private val tabPane = jfxTabPane {
        addClass(CommonStyle.hiddenTabPaneHeader)

        previousScreen = screenTab(gameScreen)
        screenTab(syncGamesScreen)
        screenTab(reportsScreen)
        screenTab(logScreen)
        screenTab(viewGameScreen)

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

    private fun TabPane.screenTab(screen: PresentableScreen) = tab(screen) {
        userData = screen
        graphic = screen.icon
    }

    private val mainNavigationButton = buttonWithPopover(graphic = Icons.menu, closeOnClick = true) {
        tabPane.tabs.forEach { tab ->
            val screen = tab.userData as PresentableScreen
            val navigation = screen.navigation
            when (navigation) {
                ScreenNavigation.MainMenu ->
                    navigationButton(screen.title, screen.icon) {
                        closeViewActions.event(tabPane.selectionModel.selectedItem.userData as PresentableScreen)
                        tabPane.selectionModel.select(tab)
                    }
                is ScreenNavigation.SubMenu ->
                    subMenu(screen.title, screen.icon, contentDisplay = ContentDisplay.TOP) {
                        navigation.builder(this)
                    }
                ScreenNavigation.Standalone -> {
                    // Nothing to do.
                }
            }
        }

        verticalGap(size = 30)

        subMenu("Maintain", Icons.wrench, contentDisplay = ContentDisplay.TOP) {
            add(maintenanceView.root)
        }

        verticalGap(size = 30)

        navigationButton("Settings", Icons.settings).apply {
            action(showSettingsActions)
            shortcut("ctrl+o")
            tooltip("Settings (ctrl+o)")
        }

        verticalGap(size = 30)

        navigationButton("Quit", Icons.quit) { System.exit(0) }
    }.apply {
        addClass(CommonStyle.toolbarButton, Style.navigationButton)
        textProperty().bind(tabPane.selectionModel.selectedItemProperty().stringBinding { it!!.text })
    }

    init {
        prepareNewTab(tabPane.selectionModel.selectedItem)
    }

    private fun cleanupClosedTab(tab: Tab) {
        previousScreen = tab
        val screen = tab.userData as PresentableScreen
        screen.callOnUndock()
    }

    private fun prepareNewTab(tab: Tab) {
        val screen = tab.userData as PresentableScreen
        screen.callOnDock()
        screen.populateToolbar()
    }

    private fun PresentableScreen.populateToolbar() {
        val screen = this
        toolbar.replaceChildren {
            if (screen.navigation != ScreenNavigation.Standalone) {
                add(mainNavigationButton)
            } else {
                backButton { action(closeViewActions) { screen } }
            }
            gap()
            children += toolbars.getOrPut(screen) {
                HBox().apply {
                    spacing = 10.0
                    useMaxWidth = true
                    hgrow = Priority.ALWAYS
                    alignment = Pos.CENTER_LEFT
                    buildToolbar()
                }
            }
        }
    }

    private inline fun EventTarget.navigationButton(text: String, icon: Node, crossinline action: () -> Unit = {}) =
        jfxButton(text, icon, alignment = Pos.CENTER) {
            useMaxWidth = true
            contentDisplay = ContentDisplay.TOP
            action {
                action()
            }
        }

    fun showGameDetails(game: Game): JavaFxViewGameScreen {
        viewGameScreen.game.valueFromView = game
        selectScreen(viewGameScreen)
        return viewGameScreen
    }

    fun showSyncGamesView(): JavaFxSyncGamesScreen {
        selectScreen(syncGamesScreen)
        return syncGamesScreen
    }

    private fun selectScreen(screen: PresentableScreen) =
        tabPane.selectionModel.select(tabPane.tabs.find { it.userData == screen })

    fun showPreviousScreen() = tabPane.selectionModel.select(previousScreen)

    override fun onDock() {
        val applicationStartTime = System.currentTimeMillis() - params[StartTimeParam] as Long
        log.info("Total application start time: ${applicationStartTime.toHumanReadableDuration()}")
    }

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
        val StartTimeParam = "GameDex.startTime"
    }
}
