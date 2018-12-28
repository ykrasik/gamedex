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

package com.gitlab.ykrasik.gamedex.app.javafx.task

import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.app.api.task.TaskProgress
import com.gitlab.ykrasik.gamedex.app.api.task.TaskView
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.image.image
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.ProgressIndicator
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import kotlinx.coroutines.Job
import tornadofx.*

/**
 * User: ykrasik
 * Date: 16/12/2018
 * Time: 10:02
 */
class JavaFxTaskView : PresentableView(), TaskView {
    override val job = state<Job?>(null)

    override val isCancellable = state(false)
    override val cancelTaskActions = channel<Unit>()

    override val taskProgress = JavaFxTaskProgress()
    override val subTaskProgress = JavaFxTaskProgress()

    override val isRunningSubTask = state(false)

    init {
        register()
    }

    override val root = stackpane { }

    fun init(f: EventTarget.() -> Node): StackPane = root.apply {
        f()
        maskerPane {
            visibleWhen { job.property.isNotNull }
            progressNode = vbox(spacing = 5) {
                progressDisplay(taskProgress, isMain = true)

                vbox {
                    showWhen { isRunningSubTask.property }

                    region { minHeight = 20.0 }
                    progressDisplay(subTaskProgress, isMain = false)
                }

                hbox {
                    showWhen { isCancellable.property }
                    spacer()
                    cancelButton("Cancel") {
                        addClass(Style.progressText)
                        eventOnAction(cancelTaskActions)
                    }
                }
            }
        }
    }

    override fun taskSuccess(message: String) {
//        javaFx {
        // This is OMFG. Showing the notification as part of the regular flow (not in a new coroutine)
        // causes an issue with modal windows not reporting that they are being hidden.
//            delay(1)
        notification(message).info.show()
//        }
    }

    override fun taskCancelled(message: String) = notification(message).warn.show()

    override fun taskError(error: Exception, message: String) = notification(message).error.show()

    private fun EventTarget.progressDisplay(taskProgress: JavaFxTaskProgress, isMain: Boolean) = vbox(spacing = 5) {
        alignment = Pos.CENTER
        defaultHbox {
            val textStyle = if (isMain) Style.mainTaskText else Style.subTaskText
            label(taskProgress.message.property) {
                addClass(Style.progressText, textStyle)
            }
            spacer()
            label(taskProgress.progress.property.asPercent()) {
                visibleWhen { taskProgress.totalItems.property.isNotEqualTo(0) }
                addClass(Style.progressText, textStyle)
            }
        }
        jfxProgressBar(taskProgress.progress.property) {
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
        override val title = state("")

        override val image = state<Image?>(null)
        val javaFxImageProperty = image.property.map { it?.image }

        override val message = state("")
        override val processedItems = state(0)
        override val totalItems = state(0)
        override val progress = state(ProgressIndicator.INDETERMINATE_PROGRESS)

//        val processedItemsCount = processedItemsProperty.combineLatest(totalItemsProperty).stringBinding {
//            val (processedItems, totalItems) = it!!
//            if (totalItems.toInt() > 1) {
//                "$processedItems / $totalItems"
//            } else {
//                ""
//            }
//        }
    }

    class Style : Stylesheet() {
        companion object {
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