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

package com.gitlab.ykrasik.gamedex.ui.view.main

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.library.LibraryScreen
import com.gitlab.ykrasik.gamedex.javafx.screen.GamedexScreen
import com.gitlab.ykrasik.gamedex.javafx.settings.SettingsController
import com.gitlab.ykrasik.gamedex.javafx.task.JavaFxTaskRunner
import com.gitlab.ykrasik.gamedex.ui.view.game.GameScreen
import com.gitlab.ykrasik.gamedex.ui.view.game.details.GameDetailsScreen
import com.gitlab.ykrasik.gamedex.ui.view.log.LogScreen
import com.gitlab.ykrasik.gamedex.ui.view.report.ReportsScreen
import javafx.collections.ObservableList
import javafx.event.EventTarget
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
class MainView : View("GameDex") {
    private val gameScreen: GameScreen by inject()
    private val reportsScreen: ReportsScreen by inject()
    private val libraryScreen: LibraryScreen by inject()
    private val logScreen: LogScreen by inject()
    private val settingsController: SettingsController by di() // TODO: Probably not the correct way to do this.

    private val gameDetailsScreen: GameDetailsScreen by inject()

    private val taskRunner: JavaFxTaskRunner by di()

    private var tabPane: TabPane by singleAssign()
    private var toolbar: ToolBar by singleAssign()

    private lateinit var previousScreen: Tab

    private val screenToolbars = mutableMapOf<GamedexScreen, ObservableList<Node>>()

    override val root = taskRunner.init {
        borderpane {
            top {
                toolbar = toolbar {
                    items.onChange {
                        fade(0.6.seconds, 1.0, play = true) {
                            fromValue = 0.0
                        }
                    }
                }
            }
            center {
                tabPane = tabpane {
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
    }

    private fun TabPane.screenTab(screen: GamedexScreen) = tab(screen) {
        userData = screen
        graphic = screen.icon
    }

    private val mainNavigationButton = buttonWithPopover(graphic = Theme.Icon.bars()) {
        tabPane.tabs.forEach { tab ->
            val screen = tab.userData as GamedexScreen
            if (screen.useDefaultNavigationButton) {
                navigationButton(tab.text, tab.graphic) { tabPane.selectionModel.select(tab) }
            }
            screen.closeRequestedProperty.onChange {
                if (it) {
                    screen.closeRequestedProperty.value = false
                    selectPreviousScreen()
                }
            }
        }

        separator()

        navigationButton("Settings", Theme.Icon.settings()) { settingsController.showSettingsMenu() }.apply {
            shortcut("ctrl+o")
            tooltip("Settings (ctrl+o)")
        }

        separator()

        navigationButton("Quit", Theme.Icon.quit()) { System.exit(0) }
    }.apply {
        textProperty().bind(tabPane.selectionModel.selectedItemProperty().map { it!!.text })
    }

    init {
        prepareNewTab(tabPane.selectionModel.selectedItem)
    }

    private fun cleanupClosedTab(tab: Tab) {
        previousScreen = tab
        (tab.userData as GamedexScreen).onUndock()
    }

    private fun prepareNewTab(tab: Tab) {
        (tab.userData as GamedexScreen).onDock()
        tab.populateToolbar()
    }

    private fun Tab.populateToolbar() = (userData as GamedexScreen).populateToolbar()

    private fun GamedexScreen.populateToolbar() {
        toolbar.replaceChildren {
            items += screenToolbars.getOrPut(this@populateToolbar) {
                // TODO: Find a neater solution, like not using ToolBar.constructToolbar()
                ToolBar().apply {
                    if (useDefaultNavigationButton) {
                        items += mainNavigationButton
                    } else {
                        backButton { setOnAction { selectPreviousScreen() } }
                    }
                    verticalSeparator()
                    this.constructToolbar()
                }.items
            }
        }
    }

    private fun EventTarget.navigationButton(text: String, icon: Node, action: () -> Unit) = jfxButton(text, icon) {
        addClass(CommonStyle.fillAvailableWidth, Style.navigationButton)
        setOnAction { action() }
    }

    private fun selectPreviousScreen() = tabPane.selectionModel.select(previousScreen)

    override fun onDock() {
        primaryStage.isMaximized = true
        root.fade(0.5.seconds, 0.0, reversed = true)
    }

    fun showGameDetails(game: Game) {
        gameDetailsScreen.game = game
        tabPane.selectionModel.selectLast()
    }

    class Style : Stylesheet() {
        companion object {
            val navigationButton by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            navigationButton {
                prefWidth = 100.px
            }
        }
    }
}
