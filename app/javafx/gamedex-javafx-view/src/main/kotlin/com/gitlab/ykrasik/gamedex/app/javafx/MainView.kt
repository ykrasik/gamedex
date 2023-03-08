/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.app.api.log.ViewCanShowLogView
import com.gitlab.ykrasik.gamedex.app.api.provider.ViewCanSyncLibraries
import com.gitlab.ykrasik.gamedex.app.api.settings.ViewCanShowSettings
import com.gitlab.ykrasik.gamedex.app.api.util.broadcastFlow
import com.gitlab.ykrasik.gamedex.app.javafx.game.JavaFxGameScreen
import com.gitlab.ykrasik.gamedex.app.javafx.library.LibraryMenu
import com.gitlab.ykrasik.gamedex.app.javafx.maintenance.JavaFxDuplicatesScreen
import com.gitlab.ykrasik.gamedex.app.javafx.maintenance.JavaFxFolderNameDiffScreen
import com.gitlab.ykrasik.gamedex.app.javafx.maintenance.MaintenanceMenu
import com.gitlab.ykrasik.gamedex.app.javafx.provider.JavaFxSyncGamesScreen
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.theme.GameDexStyle
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableScreen
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.gitlab.ykrasik.gamedex.util.humanReadable
import com.jfoenix.controls.JFXButton
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.text.FontWeight
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import tornadofx.*
import java.util.*
import kotlin.time.TimeMark

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 22:44
 */
class MainView : PresentableView("GameDex"),
    ViewCanSyncLibraries,
    ViewCanShowSettings,
    ViewCanShowLogView,
    ViewCanShowAboutView {

    private val gameScreen: JavaFxGameScreen by inject()
    private val syncGamesScreen: JavaFxSyncGamesScreen by inject()
    private val duplicatesScreen: JavaFxDuplicatesScreen by inject()
    private val folderNameDiffScreen: JavaFxFolderNameDiffScreen by inject()
    private val maintenanceMenu: MaintenanceMenu by inject()

    private val libraryMenu: LibraryMenu by inject()

    private val overlayPane = OverlayPane()

    private val toolbars = mutableMapOf<PresentableScreen, HBox>()

    override val canSyncLibraries = mutableStateFlow(IsValid.valid, debugName = "canSyncLibraries")
    override val syncLibrariesActions = broadcastFlow<Unit>()

    override val showLogViewActions = broadcastFlow<Unit>()
    override val showSettingsActions = broadcastFlow<Unit>()
    override val showAboutActions = broadcastFlow<Unit>()

    private val maxNavigationHistory = 5
    private val navigationHistory = ArrayDeque<Tab>(maxNavigationHistory)

    private val toolbar = prettyToolbar {
        children.onChange {
            fade(0.6.seconds, 1.0) {
                fromValue = 0.0
            }
        }
    }

    private val tabPane = jfxTabPane {
        addClass(GameDexStyle.hiddenTabPaneHeader)
        isDisableAnimation = true   // Too slow by default

        tab(gameScreen).select()
        tab(syncGamesScreen)
        tab(duplicatesScreen)
        tab(folderNameDiffScreen)

        selectionModel.selectedItemProperty().addListener { _, oldValue, newValue ->
            cleanupClosedTab(oldValue)
            prepareNewTab(newValue)
        }
    }

    override val root = stackpane {
        borderpane {
            top = toolbar
            center = tabPane
        }
        children += overlayPane
    }

    private fun TabPane.tab(screen: PresentableScreen) = tab(screen) { userData = screen }

    private val mainNavigationButton = stackpane {
        addClass(Style.navigationButton)
        isFocusTraversable = false

        popOverMenu(graphic = Icons.menu) {
//            navigationButton(gameScreen) {
//                shortcut("ctrl+g")
//                tooltip("Show Games (ctrl+g)")
//            }
//
//            verticalGap(size = 15)

            navigationButton("Sync Libraries", Icons.folderSync) {
                tooltip("Scan all libraries for new games and sync them with providers")
                enableWhen(canSyncLibraries)
                action(syncLibrariesActions)
            }
            subMenu(libraryMenu)

            verticalGap(size = 15)

            subMenu(maintenanceMenu) { maintenanceMenu.init(this) }

            navigationButton("Settings", Icons.settings) {
                action(showSettingsActions)
                shortcut("ctrl+o")
                tooltip("Show Settings (ctrl+o)")
            }
            navigationButton("Log", Icons.book) {
                action(showLogViewActions)
                shortcut("ctrl+l")
                tooltip("Show Log (ctrl+l)")
            }
            navigationButton("About", Icons.information) {
                action(showAboutActions)
                shortcut("ctrl+i")
                tooltip("Show About (ctrl+i)")
            }

            verticalGap(size = 15)

            navigationButton("Quit", Icons.quit) {
                action {
                    System.exit(0)
                }
            }
        }
    }

    init {
        register()
        prepareNewTab(tabPane.selectionModel.selectedItem)

        whenDockedOnce {
            (params[StartTimeParam] as? TimeMark)?.let { clockMark ->
                val applicationStartTimeTaken = clockMark.elapsedNow()
                log.info("Application load time: ${applicationStartTimeTaken.humanReadable}")
            }
        }

        shortcut("ctrl+q") { hideAllOverlays() }

        JavaFxScope.launch(CoroutineName("MainView.hideActiveOverlaysRequests")) {
            listOf(gameScreen, syncGamesScreen, duplicatesScreen, folderNameDiffScreen, maintenanceMenu, libraryMenu)
                .map { it.hideActiveOverlaysRequests }
                .merge()
                .collect {
                    hideAllOverlays()
                }
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

//    private inline fun PopOverContent.navigationButton(screen: PresentableScreen, crossinline op: JFXButton.() -> Unit = {}) =
//        navigationButton(screen.title, screen.icon) {
//            action { showScreen(screen) }
//            op()
//        }

    private inline fun PopOverContent.navigationButton(text: String, icon: Node, crossinline op: JFXButton.() -> Unit = {}) =
        jfxButton(text, icon, alignment = Pos.CENTER_LEFT) {
            useMaxWidth = true
            op()
        }

    private inline fun PopOverMenu.subMenu(
        view: PresentableView,
        crossinline op: PopOverMenu.() -> Unit = { children += view.root },
    ): HBox = popOverSubMenu(view.title, view.icon, op = op)

    fun showSyncGamesView(): JavaFxSyncGamesScreen = showScreen(syncGamesScreen)

    fun showDuplicatesReport(): JavaFxDuplicatesScreen = showScreen(duplicatesScreen)
    fun showFolderNameDiffReport(): JavaFxFolderNameDiffScreen = showScreen(folderNameDiffScreen)

    fun showPreviousScreen() {
        navigationHistory.pop()   // The current screen being shown is at the top of the stack.
        tabPane.selectionModel.select(navigationHistory.pop()) // Pop again, because the selection change will push it right back in.
    }

    private inline fun <V : PresentableScreen> showScreen(screen: V, op: V.() -> Unit = {}): V = screen.apply {
        op()
        tabPane.selectionModel.select(screen.tab)
    }

    fun showOverlay(view: View, customizeOverlay: OverlayPane.OverlayLayerImpl.() -> Unit = {}) =
        overlayPane.show(view, customizeOverlay)

    fun hideOverlay(view: View) = overlayPane.hide(view)

    fun forceHideAllOverlays() = overlayPane.forceHideAll()

    fun saveAndClearCurrentOverlays() = overlayPane.saveAndClear()

    fun restoreSavedOverlays() = overlayPane.restoreSaved()

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
                padding = box(top = 0.px, bottom = 0.px, left = 10.px, right = 10.px)
            }
        }
    }

    companion object {
        const val StartTimeParam = "GameDex.startTime"
    }
}
