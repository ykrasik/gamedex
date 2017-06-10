package com.gitlab.ykrasik.gamedex.ui.view.main

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import com.gitlab.ykrasik.gamedex.ui.theme.backButton
import com.gitlab.ykrasik.gamedex.ui.view.GamedexScreen
import com.gitlab.ykrasik.gamedex.ui.view.game.GameScreen
import com.gitlab.ykrasik.gamedex.ui.view.game.details.GameDetailsScreen
import com.gitlab.ykrasik.gamedex.ui.view.library.LibraryScreen
import com.gitlab.ykrasik.gamedex.ui.view.log.LogScreen
import com.gitlab.ykrasik.gamedex.ui.view.settings.SettingsFragment
import com.gitlab.ykrasik.gamedex.ui.widgets.Notification
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.ToolBar
import org.controlsfx.control.NotificationPane
import tornadofx.*

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 22:44
 */
class MainView : View("Gamedex") {
    private val gameScreen: GameScreen by inject()
    private val libraryScreen: LibraryScreen by inject()
    private val logScreen: LogScreen by inject()
    private val gameDetailsScreen: GameDetailsScreen by inject()

    private var tabPane: TabPane by singleAssign()
    private var toolbar: ToolBar by singleAssign()

    private lateinit var previousScreen: Tab

    override val root = persistentNotification.apply {
        content = borderpane {
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
                    addClass(CommonStyle.tabbedNavigation)

                    tab(gameScreen) {
                        userData = gameScreen
                        graphic = Theme.Icon.games()
                        previousScreen = this
                    }
                    tab(libraryScreen) {
                        userData = libraryScreen
                        graphic = Theme.Icon.hdd()
                    }
                    tab(logScreen) {
                        userData = logScreen
                        graphic = Theme.Icon.report()
                    }
                    tab(gameDetailsScreen) {
                        userData = gameDetailsScreen
                    }

                    selectionModel.select(0)
                }
            }
        }
    }

    private val mainNavigationButton = buttonWithPopover(graphic = Theme.Icon.bars()) {
        tabPane.tabs.forEach { tab ->
            val gamedexScreen = tab.userData as GamedexScreen
            if (gamedexScreen.useDefaultNavigationButton) {
                navigationButton(tab.text, tab.graphic) { tabPane.selectionModel.select(tab) }
            }
            gamedexScreen.closeRequestedProperty.onChange {
                if (it) {
                    gamedexScreen.closeRequestedProperty.value = false
                    selectPreviousScreen()
                }
            }
        }

        separator()

        navigationButton("Settings", Theme.Icon.settings()) { SettingsFragment().show() }

        separator()

        navigationButton("Quit", Theme.Icon.quit()) { System.exit(0) }
    }.apply {
        textProperty().bind(tabPane.selectionModel.selectedItemProperty().map { it!!.text })
    }

    init {
        tabPane.selectionModel.selectedItemProperty().perform { it!!.populateToolbar() }
        tabPane.selectionModel.selectedItemProperty().addListener { _, oldValue, _ ->
            previousScreen = oldValue
        }
    }

    private fun Tab.populateToolbar() = (userData as GamedexScreen).populateToolbar()

    // TODO: Should really consider only constructing this once.
    private fun GamedexScreen.populateToolbar() {
        toolbar.replaceChildren {
            if (useDefaultNavigationButton) {
                items += mainNavigationButton
            } else {
                backButton { setOnAction { selectPreviousScreen() } }
            }
            verticalSeparator()
            this.constructToolbar()
        }
    }

    private fun EventTarget.navigationButton(text: String, icon: Node, action: () -> Unit) = jfxButton(text, icon) {
        addClass(CommonStyle.fillAvailableWidth, Style.navigationButton)
        setOnAction { action() }
    }

    private fun selectPreviousScreen() {
        (tabPane.selectionModel.selectedItem.userData as GamedexScreen).onUndock()
        tabPane.selectionModel.select(previousScreen)
    }

    override fun onDock() {
        primaryStage.isMaximized = true
        root.fade(0.5.seconds, 0.0, reversed = true)
    }

    fun showGameDetails(game: Game) {
        gameDetailsScreen.game = game
        tabPane.selectionModel.select(3)
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

    companion object {
        private val persistentNotification = NotificationPane().apply {
            isCloseButtonVisible = false
            isShowFromTop = false
        }

        val canShowPersistentNotificationProperty = persistentNotification.showingProperty().not()

        fun showPersistentNotification(graphic: Node) {
            persistentNotification.graphic = graphic
            persistentNotification.show()
        }

        fun hidePersistentNotification() {
            persistentNotification.hide()
        }

        fun showFlashInfoNotification(text: String) = Notification()
            .text(text)
            .information()
            .automaticallyHideAfter(3.seconds)
            .hideCloseButton()
            .position(Pos.BOTTOM_RIGHT)
            .show()
    }
}
