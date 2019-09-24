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

package com.gitlab.ykrasik.gamedex.app.javafx.provider

import com.gitlab.ykrasik.gamedex.app.api.file.ViewCanOpenFile
import com.gitlab.ykrasik.gamedex.app.api.provider.GameSearchState
import com.gitlab.ykrasik.gamedex.app.api.provider.SyncGamesView
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.jfxButton
import com.gitlab.ykrasik.gamedex.javafx.control.jfxProgressBar
import com.gitlab.ykrasik.gamedex.javafx.control.prettyListCell
import com.gitlab.ykrasik.gamedex.javafx.control.prettyListView
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.dangerButton
import com.gitlab.ykrasik.gamedex.javafx.theme.size
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableScreen
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import tornadofx.*
import java.io.File

/**
 * User: ykrasik
 * Date: 17/10/2018
 * Time: 09:18
 */
class JavaFxSyncGamesScreen : PresentableScreen("Sync", Icons.sync), SyncGamesView, ViewCanOpenFile {
    override val cancelActions = channel<Unit>()

    override val isAllowSmartChooseResults = state(false)

    override val isGameSyncRunning = state(false)

    override val state = settableList<GameSearchState>()
    private val pathsToProcessSize = state.sizeProperty
    override val numProcessed = state(0)

    override val currentState = userMutableState<GameSearchState?>(null)
    override val restartStateActions = channel<GameSearchState>()

    override val openFileActions = channel<File>()

    private val providerSearchView: JavaFxProviderSearchView by inject()

    override val customNavigationButton = dangerButton("Stop", graphic = Icons.stop) {
        isCancelButton = false
        action(cancelActions)
    }

    init {
        register()
    }

    private val paths = prettyListView(state) {
        useMaxWidth = true
        hgrow = Priority.ALWAYS
        addClass(Style.pathsList)

        prettyListCell { state ->
            text = state.libraryPath.path.toString()
            graphic = StackPane().apply {
                jfxButton {
                    mouseTransparentWhen { isGameSyncRunning.property.not().or(!state.isFinished) }
                    graphicProperty().bind(hoverProperty().binding { hover ->
                        when {
                            hover && state.isFinished -> Icons.resetToDefault
                            state.status == GameSearchState.Status.Success -> Icons.accept
                            state.status == GameSearchState.Status.Cancelled -> Icons.cancel
                            else -> null
                        }?.size(24)
                    })
                    action(restartStateActions) { state }
                }
//                val cellPrefWidth = prefWidth(-1.0) + (verticalScrollbar?.width ?: 0.0) + insets.left + insets.right
//                if (cellPrefWidth > this@customListView.prefWidth) {
//                    this@customListView.prefWidth = cellPrefWidth
//                }
            }
        }

        var ignoreNextSelectionChange = false
        selectionModel.selectedItemProperty().addListener { _, oldValue, selectedItem ->
            if (selectedItem?.index != oldValue?.index && !ignoreNextSelectionChange) {
                currentState.valueFromView = selectedItem
            }
        }

        currentState.onChange { state ->
            ignoreNextSelectionChange = true
            selectionModel.select(state)
            ignoreNextSelectionChange = false
        }
    }

    override val root = hbox {
        useMaxWidth = true
        visibleWhen { pathsToProcessSize.booleanBinding { it!!.toInt() > 0 } }

        add(paths)
        add(providerSearchView)
    }

    override fun HBox.buildToolbar() {
        val resultsAndNumProcessed = pathsToProcessSize.combineLatest(numProcessed.property)
        val numProcessedLabelProperty = resultsAndNumProcessed.stringBinding {
            val (numResults, numProcessed) = it!!
            "$numProcessed/$numResults"
        }
        val progressProperty = resultsAndNumProcessed.doubleBinding {
            val (numResults, numProcessed) = it!!
            numProcessed.toDouble() / numResults.toDouble()
        }
        label(numProcessedLabelProperty)
        jfxProgressBar(progressProperty) {
            hgrow = Priority.ALWAYS
            useMaxWidth = true
            paddingRight = 20
        }
    }

    override fun successMessage(message: String) = notification(NotificationType.Info, "Done: $message", title = null)
    override fun cancelledMessage(message: String) = notification(NotificationType.Warn, "Cancelled: $message", title = null)

    class Style : Stylesheet() {
        companion object {
            val pathsList by cssclass()

            init {
                importStylesheetSafe(Style::class)
            }
        }

        init {
            pathsList {
                text {
                    fontSize = 18.px
                }
            }
        }
    }
}