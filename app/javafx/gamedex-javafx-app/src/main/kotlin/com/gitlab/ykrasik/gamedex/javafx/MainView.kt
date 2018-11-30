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

import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.app.api.settings.ViewCanShowSettings
import com.gitlab.ykrasik.gamedex.app.api.task.TaskProgress
import com.gitlab.ykrasik.gamedex.app.api.task.TaskView
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.game.GameScreen
import com.gitlab.ykrasik.gamedex.app.javafx.game.details.JavaFxViewGameScreen
import com.gitlab.ykrasik.gamedex.app.javafx.image.image
import com.gitlab.ykrasik.gamedex.app.javafx.library.JavaFxLibraryScreen
import com.gitlab.ykrasik.gamedex.app.javafx.log.JavaFxLogScreen
import com.gitlab.ykrasik.gamedex.app.javafx.report.ReportsScreen
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableScreen
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import com.gitlab.ykrasik.gamedex.util.toHumanReadableDuration
import javafx.beans.property.*
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import kfoenix.jfxprogressbar
import kotlinx.coroutines.Job
import tornadofx.*

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 22:44
 */
class MainView : PresentableView("GameDex"), TaskView, ViewCanShowSettings {
    private val gameScreen: GameScreen by inject()
    private val reportsScreen: ReportsScreen by inject()
    private val libraryScreen: JavaFxLibraryScreen by inject()
    private val logScreen: JavaFxLogScreen by inject()

    private val viewGameScreen: JavaFxViewGameScreen by inject()

    private var tabPane: TabPane by singleAssign()
    private var toolbar: ToolBar by singleAssign()

    private lateinit var previousScreen: Tab

    private val fakeScreens = setOf(viewGameScreen)

    private val toolbars = mutableMapOf<PresentableScreen, HBox>()

    private val jobProperty = SimpleObjectProperty<Job?>(null)
    override var job by jobProperty

    private val isCancellableProperty = SimpleBooleanProperty(false)
    override var isCancellable by isCancellableProperty

    override val cancelTaskActions = channel<Unit>()

    override val taskProgress = JavaFxTaskProgress()
    override val subTaskProgress = JavaFxTaskProgress()

    private val isRunningSubTaskProperty = SimpleBooleanProperty(false)
    override var isRunningSubTask by isRunningSubTaskProperty

    override val showSettingsActions = channel<Unit>()

    init {
        viewRegistry.onCreate(this)
    }

    override val root = stackpane {
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
                    screenTab(viewGameScreen)
                    screenTab(reportsScreen)
                    screenTab(libraryScreen)
                    screenTab(logScreen)

                    selectionModel.selectedItemProperty().addListener { _, oldValue, newValue ->
                        cleanupClosedTab(oldValue)
                        prepareNewTab(newValue)
                    }
                }
            }
        }
        maskerPane {
            visibleWhen { jobProperty.isNotNull }
            progressNode = vbox(spacing = 5) {
                progressDisplay(taskProgress, isMain = true)

                vbox {
                    showWhen { isRunningSubTaskProperty }

                    region { minHeight = 20.0 }
                    progressDisplay(subTaskProgress, isMain = false)
                }

                hbox {
                    showWhen { isCancellableProperty }
                    spacer()
                    cancelButton("Cancel") {
                        addClass(Style.progressText)
                        eventOnAction(cancelTaskActions)
                    }
                }
            }
        }
    }

    private fun TabPane.screenTab(screen: PresentableScreen) = tab(screen) {
        userData = screen
        graphic = screen.icon
    }

    private val mainNavigationButton = buttonWithPopover(graphic = Icons.menu) {
        tabPane.tabs.forEach { tab ->
            val screen = tab.userData as PresentableScreen
            if (screen !in fakeScreens) {
                navigationButton(tab.text, tab.graphic) { tabPane.selectionModel.select(tab) }
            }
        }

        verticalGap(size = 30)

        navigationButton("Settings", Icons.settings) { }.apply {
            eventOnAction(showSettingsActions)
            shortcut("ctrl+o")
            tooltip("Settings (ctrl+o)")
        }

        verticalGap(size = 30)

        navigationButton("Quit", Icons.quit) { System.exit(0) }
    }.apply {
        addClass(Style.navigationButton)
        alignment = Pos.CENTER_LEFT
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
            if (screen !in fakeScreens) {
                add(mainNavigationButton)
            } else {
                backButton { setOnAction { showPreviousScreen() } }
            }
            gap()
            items += toolbars.getOrPut(screen) {
                HBox().apply {
                    spacing = 10.0
                    useMaxWidth = true
                    hgrow = Priority.ALWAYS
                    alignment = Pos.CENTER_LEFT
                    constructToolbar()
                }
            }
        }
    }

    private fun EventTarget.navigationButton(text: String, icon: Node, action: () -> Unit) = jfxButton(text, icon) {
        useMaxWidth = true
        alignment = Pos.CENTER
        contentDisplay = ContentDisplay.TOP
        setOnAction { action() }
    }

    fun showGameDetails() = selectScreen(viewGameScreen)

    private fun selectScreen(screen: PresentableScreen) =
        tabPane.selectionModel.select(tabPane.tabs.find { it.userData == screen })

    fun showPreviousScreen() = tabPane.selectionModel.select(previousScreen)

    override fun onDock() {
        val applicationStartTime = System.currentTimeMillis() - Main.startTime
        log.info("Total application start time: ${applicationStartTime.toHumanReadableDuration()}")
    }

    override fun taskSuccess(message: String) {
//        javaFx {
        // This is OMFG. Showing the notification as part of the regular flow (not in a new coroutine)
        // causes an issue with modal windows not reporting that they are being hidden.
//            delay(1)
        notification(message).info.show()
//        }
    }

    override fun taskCancelled(message: String) = taskSuccess(message)

    private fun EventTarget.progressDisplay(taskProgress: JavaFxTaskProgress, isMain: Boolean) = vbox(spacing = 5) {
        alignment = Pos.CENTER
        hbox {
            val textStyle = if (isMain) Style.mainTaskText else Style.subTaskText
            label(taskProgress.messageProperty) {
                addClass(Style.progressText, textStyle)
            }
            spacer()
            label(taskProgress.progressProperty.asPercent()) {
                visibleWhen { taskProgress.progressProperty.isNotEqualTo(-1) }
                addClass(Style.progressText, textStyle)
            }
        }
        jfxprogressbar {
            progressProperty().bind(taskProgress.progressProperty)
            useMaxWidth = true
            addClass(if (isMain) Style.mainTaskProgress else Style.subTaskProgress)
        }
        imageview(taskProgress.javaFxImageProperty) {
            fitHeight = 120.0
            isPreserveRatio = true
            showWhen { taskProgress.javaFxImageProperty.isNotNull }
        }
    }

    class JavaFxTaskProgress : TaskProgress {
        val titleProperty = SimpleStringProperty("")
        override var title by titleProperty

        val imageProperty = SimpleObjectProperty<Image?>(null)
        val javaFxImageProperty = imageProperty.map { it?.image }
        override var image by imageProperty

        val messageProperty = SimpleStringProperty("")
        override var message by messageProperty

        val processedItemsProperty = SimpleIntegerProperty(0)
        override var processedItems by processedItemsProperty

        val totalItemsProperty = SimpleIntegerProperty(0)
        override var totalItems by totalItemsProperty

//        val processedItemsCount = processedItemsProperty.combineLatest(totalItemsProperty).stringBinding {
//            val (processedItems, totalItems) = it!!
//            if (totalItems.toInt() > 1) {
//                "$processedItems / $totalItems"
//            } else {
//                ""
//            }
//        }

        val progressProperty = SimpleDoubleProperty(ProgressIndicator.INDETERMINATE_PROGRESS)
        override var progress by progressProperty
    }

    class Style : Stylesheet() {
        companion object {
            val navigationButton by cssclass()
            val mainTaskProgress by cssclass()
            val mainTaskText by cssclass()
            val subTaskProgress by cssclass()
            val subTaskText by cssclass()

            val progressText by cssclass()

            init {
                importStylesheetSafe(Style::class)
            }
        }

        init {
            navigationButton {
                fontWeight = FontWeight.BOLD
            }
            mainTaskProgress {
            }

            mainTaskText {
                fontSize = 24.px
            }

            subTaskProgress {
                bar {
                    backgroundColor = multi(Color.FORESTGREEN)
                }
//                percentage {
//                    fill = Color.CADETBLUE
//                }
//                arc {
//                    stroke = Color.CORNFLOWERBLUE
//                }
            }

            subTaskText {
                fontSize = 16.px
            }

            progressText {
                textFill = Color.WHITE
            }
        }
    }
}
