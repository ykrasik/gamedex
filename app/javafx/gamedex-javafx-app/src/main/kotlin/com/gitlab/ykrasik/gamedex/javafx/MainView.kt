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

package com.gitlab.ykrasik.gamedex.javafx

import com.gitlab.ykrasik.gamedex.app.javafx.library.JavaFxLibraryScreen
import com.gitlab.ykrasik.gamedex.app.javafx.log.JavaFxLogScreen
import com.gitlab.ykrasik.gamedex.javafx.game.GameScreen
import com.gitlab.ykrasik.gamedex.javafx.game.details.JavaFxGameDetailsScreen
import com.gitlab.ykrasik.gamedex.javafx.report.ReportsScreen
import com.gitlab.ykrasik.gamedex.javafx.settings.SettingsController
import com.gitlab.ykrasik.gamedex.javafx.task.JavaFxTaskRunner
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableScreen
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import com.gitlab.ykrasik.gamedex.util.logger
import com.gitlab.ykrasik.gamedex.util.toHumanReadableDuration
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.ToolBar
import tornadofx.*

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 22:44
 */
class MainView : PresentableView("GameDex") {
    private val logger = logger()

    private val gameScreen: GameScreen by inject()
    private val reportsScreen: ReportsScreen by inject()
    private val libraryScreen: JavaFxLibraryScreen by inject()
    private val logScreen: JavaFxLogScreen by inject()
    private val settingsController: SettingsController by di() // TODO: Probably not the correct way to do this.

    private val gameDetailsScreen: JavaFxGameDetailsScreen by inject()

    private val taskRunner: JavaFxTaskRunner by di()

    private var tabPane: TabPane by singleAssign()
    private var toolbar: ToolBar by singleAssign()

    private lateinit var previousScreen: Tab

    private val nonNavigableScreens = setOf(gameDetailsScreen)

    private val toolbars = mutableMapOf<PresentableScreen, ToolBar>()

    init {
        viewRegistry.register(this)
    }

    override val root = stackpane {
        borderpane {
            top {
                toolbar = toolbar {
                    enableWhen { enabledProperty }
                    items.onChange {
                        fade(0.6.seconds, 1.0, play = true) {
                            fromValue = 0.0
                        }
                    }
                }
            }
            center {
                tabPane = jfxTabPane {
                    addClass(CommonStyle.hiddenTabPaneHeader)

                    previousScreen = screenTab(gameScreen)
                    screenTab(reportsScreen)
                    screenTab(libraryScreen)
                    screenTab(logScreen)
                    screenTab(gameDetailsScreen)

                    selectionModel.selectedItemProperty().addListener { _, oldValue, newValue ->
                        cleanupClosedTab(oldValue)
                        prepareNewTab(newValue)
                    }
                }
            }
        }
        children += taskRunner.maskerPane()
    }

    private fun TabPane.screenTab(screen: PresentableScreen) = tab(screen) {
        userData = screen
        graphic = screen.icon
    }

    private val mainNavigationButton = buttonWithPopover(graphic = Theme.Icon.bars()) {
        tabPane.tabs.forEach { tab ->
            val screen = tab.userData as PresentableScreen
            if (screen !in nonNavigableScreens) {
                navigationButton(tab.text, tab.graphic) { tabPane.selectionModel.select(tab) }
            }
        }

        separator()

        navigationButton("Settings", Theme.Icon.settings()) {
            javaFx {
                settingsController.showSettingsMenu()
            }
        }.apply {
            shortcut("ctrl+o")
            tooltip("Settings (ctrl+o)")
        }

        separator()

        navigationButton("Quit", Theme.Icon.quit()) { System.exit(0) }
    }.apply {
        textProperty().bind(tabPane.selectionModel.selectedItemProperty().stringBinding { it!!.text })
    }

    init {
        prepareNewTab(tabPane.selectionModel.selectedItem)
    }

    private fun cleanupClosedTab(tab: Tab) {
        previousScreen = tab
        val screen = tab.userData as PresentableScreen
        screen.onUndock()
        screen.onUndockListeners?.forEach { it.invoke(screen) }
    }

    private fun prepareNewTab(tab: Tab) {
        val screen = tab.userData as PresentableScreen
        screen.callOnDock()
        screen.populateToolbar()
    }

    private fun PresentableScreen.populateToolbar() {
        val screen = this
        toolbar.replaceChildren {
            items += toolbars.getOrPut(screen) {
                ToolBar().apply {
                    if (screen !in nonNavigableScreens) {
                        items += mainNavigationButton
                    } else {
                        backButton { setOnAction { showPreviousScreen() } }
                    }
                    verticalSeparator()
                    this.constructToolbar()
                }
            }.items
        }
    }

    private fun EventTarget.navigationButton(text: String, icon: Node, action: () -> Unit) = jfxButton(text, icon) {
        useMaxWidth = true
        alignment = Pos.CENTER_LEFT
        setOnAction { action() }
    }

    // TODO: Try to move this responsibility to the viewManager.
    fun showGameDetails() {
        tabPane.selectionModel.selectLast()
    }

    fun showPreviousScreen() = tabPane.selectionModel.select(previousScreen)

    override fun onDock() {
        val applicationStartTime = System.currentTimeMillis() - Main.startTime
        logger.info("Total application start time: ${applicationStartTime.toHumanReadableDuration()}")
    }
}
