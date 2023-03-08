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

package com.gitlab.ykrasik.gamedex.app.javafx.task

import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.app.api.task.TaskProgress
import com.gitlab.ykrasik.gamedex.app.api.task.TaskView
import com.gitlab.ykrasik.gamedex.app.api.util.broadcastFlow
import com.gitlab.ykrasik.gamedex.app.javafx.image.image
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.theme.cancelButton
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.control.ProgressIndicator
import javafx.scene.paint.Color
import kotlinx.coroutines.Job
import tornadofx.*

/**
 * User: ykrasik
 * Date: 16/12/2018
 * Time: 10:02
 */
class JavaFxTaskView : PresentableView(), TaskView {
    override val job = mutableStateFlow<Job?>(null, debugName = "job")

    override val isCancellable = mutableStateFlow(false, debugName = "isCancellable")
    override val cancelTaskActions = broadcastFlow<Unit>()

    override val taskProgress = JavaFxTaskProgress("mainTask")
    override val subTaskProgress = JavaFxTaskProgress("subTask")

    override val isRunningSubTask = mutableStateFlow(false, debugName = "isRunningSubTask")

    init {
        register()
    }

    override val root = vbox(spacing = 5) {
        addClass(Style.taskViewRoot)

        progressDisplay(taskProgress, isMain = true)

        vbox {
            showWhen { isRunningSubTask.property }
            verticalGap(size = 20)
            progressDisplay(subTaskProgress, isMain = false)
        }

        cancelButton("Cancel") {
            isCancelButton = false
            isFocusTraversable = false
            useMaxWidth = true
            showWhen { isCancellable.property }
            addClass(Style.progressText)
            action(cancelTaskActions)
        }
    }

    override fun taskSuccess(title: String, message: String) =
        notification(NotificationType.Info, message, title)

    override fun taskCancelled(title: String, message: String) =
        notification(NotificationType.Warn, message, title)

    override fun taskError(title: String, error: Exception, message: String) =
        notification(NotificationType.Error, message, title)

    private fun EventTarget.progressDisplay(taskProgress: JavaFxTaskProgress, isMain: Boolean) = vbox(spacing = 5) {
        alignment = Pos.CENTER
        defaultHbox {
            val textStyle = if (isMain) Style.mainTaskText else Style.subTaskText
            label(taskProgress.message.property) {
                addClass(Style.progressText, textStyle)
            }
            spacer()
            label(taskProgress.processedItemsString) {
                visibleWhen { taskProgress.processedItemsString.isNotEmpty }
                addClass(Style.progressText, textStyle)
            }
            gap(3)
            label(taskProgress.progress.property.asPercent()) {
                visibleWhen { taskProgress.totalItems.property.typesafeBooleanBinding { it > 1 } }
                addClass(Style.progressText, textStyle)
            }
        }
        jfxProgressBar(taskProgress.progress.property) {
            useMaxWidth = true
            addClass(if (isMain) Style.mainTaskProgress else Style.subTaskProgress)
            clipRectangle(arc = 3)
        }
        imageview(taskProgress.javaFxImage) {
            fitHeight = 120.0
            isPreserveRatio = true
            showWhen { taskProgress.javaFxImage.isNotNull }
        }
    }

    class JavaFxTaskProgress(debugName: String) : TaskProgress {
        override val title = mutableStateFlow("", debugName = "$debugName.title")

        override val image = mutableStateFlow<Image?>(null, debugName = "$debugName.image")
        val javaFxImage = image.property.binding { it?.image }

        override val message = mutableStateFlow("", debugName = "$debugName.message")
        override val processedItems = mutableStateFlow(0, debugName = "$debugName.processedItems")
        override val totalItems = mutableStateFlow(0, debugName = "$debugName.totalItems")
        override val progress = mutableStateFlow(ProgressIndicator.INDETERMINATE_PROGRESS, debugName = "$debugName.progress")

        val processedItemsString = processedItems.property.combineLatest(totalItems.property).typesafeStringBinding { (processedItems, totalItems) ->
            if (totalItems > 1) {
                "$processedItems / $totalItems"
            } else {
                ""
            }
        }
    }

    class Style : Stylesheet() {
        companion object {
            val taskViewRoot by cssclass()
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
            taskViewRoot {
                padding = box(20.px)
                minWidth = 700.px
//                minHeight = 175.px
                backgroundColor = multi(c(0, 0, 0, 0.9))
            }

            mainTaskProgress {
                bar {
                    backgroundColor = multi(Color.CORNFLOWERBLUE)
                    backgroundRadius = multi(box(20.px))
                }
            }

            mainTaskText {
                fontSize = 24.px
            }

            subTaskProgress {
                bar {
                    backgroundColor = multi(Color.FORESTGREEN)
                    backgroundRadius = multi(box(20.px))
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
