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

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.javafx.library.JavaFxLibraryScreen
import com.gitlab.ykrasik.gamedex.app.javafx.log.JavaFxLogScreen
import com.gitlab.ykrasik.gamedex.javafx.game.GameScreen
import com.gitlab.ykrasik.gamedex.javafx.game.details.JavaFxGameDetailsScreen
import com.gitlab.ykrasik.gamedex.javafx.report.ReportsScreen
import com.gitlab.ykrasik.gamedex.javafx.screen.GamedexScreen
import com.gitlab.ykrasik.gamedex.javafx.screen.PresentableScreen
import com.gitlab.ykrasik.gamedex.javafx.settings.SettingsController
import com.gitlab.ykrasik.gamedex.javafx.task.JavaFxTaskRunner
import javafx.collections.ObservableList
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
class MainView : View("GameDex") {
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

    // FIXME: Temp until all screens are presentable
    private val screenToolbars = mutableMapOf<GamedexScreen, ObservableList<Node>>()
    private val presentableScreenToolbars = mutableMapOf<PresentableScreen, ObservableList<Node>>()

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
    }

    private fun TabPane.screenTab(screen: GamedexScreen) = tab(screen) {
        userData = screen
        graphic = screen.icon
    }

    // FIXME: Temp until all screens are presentable
    private fun TabPane.screenTab(screen: PresentableScreen) = tab(screen) {
        userData = screen
        graphic = screen.icon
    }

    private val mainNavigationButton = buttonWithPopover(graphic = Theme.Icon.bars()) {
        tabPane.tabs.forEach { tab ->
            if (tab.userData is GamedexScreen) {
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
            } else {
                // FIXME: Temp until all screens are presentable
                val screen = tab.userData as PresentableScreen
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
        // FIXME: Temp until all screens are presentable
        if (tab.userData is GamedexScreen) {
            (tab.userData as GamedexScreen).onUndock()
        } else {
            val screen = tab.userData as PresentableScreen
            screen.onUndock()
            screen.onUndockListeners?.forEach { it.invoke(screen) }
        }
    }

    private fun prepareNewTab(tab: Tab) {
        // FIXME: Temp until all screens are presentable
        if (tab.userData is GamedexScreen) {
            val screen = tab.userData as GamedexScreen
            screen.onDock()
            screen.populateToolbar()
        } else {
            val screen = tab.userData as PresentableScreen
            screen.onDock()
            screen.onDockListeners?.forEach { it.invoke(screen) }
            screen.populateToolbar()
        }
    }

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

    // FIXME: Temp until all screens are presentable
    private fun PresentableScreen.populateToolbar() {
        toolbar.replaceChildren {
            items += presentableScreenToolbars.getOrPut(this@populateToolbar) {
                // TODO: Find a neater solution, like not using ToolBar.constructToolbar()
                ToolBar().apply {
                    enableWhen { enabledProperty }
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
        useMaxWidth = true
        alignment = Pos.CENTER_LEFT
        setOnAction { action() }
    }

    private fun selectPreviousScreen() = tabPane.selectionModel.select(previousScreen)

    override fun onDock() {
        primaryStage.isMaximized = true
        root.fade(0.5.seconds, 0.0, reversed = true)
    }

    fun showGameDetails(game: Game) {
        gameDetailsScreen.show(game)
        tabPane.selectionModel.selectLast()
    }
}
