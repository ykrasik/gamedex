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

package com.gitlab.ykrasik.gamedex.javafx.task

import com.gitlab.ykrasik.gamedex.app.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.app.api.util.BroadcastEventChannel
import com.gitlab.ykrasik.gamedex.app.api.util.ReadOnlyTask
import com.gitlab.ykrasik.gamedex.app.api.util.TaskType
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.notification.Notification
import com.gitlab.ykrasik.gamedex.util.InitOnce
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.layout.VBox
import javafx.scene.text.FontWeight
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.javafx.JavaFx
import org.controlsfx.control.NotificationPane
import tornadofx.*
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 19/03/2018
 * Time: 22:28
 */
// TODO: Separate into TaskView & Presenter
@Singleton
class JavaFxTaskRunner : TaskRunner {
    private val currentJobProperty = SimpleObjectProperty<Job?>(null)
    private var currentJob by currentJobProperty

    private var notificationPane: NotificationPane by InitOnce()

    private var mainTask: ReadOnlyTask<*>? = null
    private val mainTaskProperties = TaskProperties()
    private val tasks = mutableListOf<ReadOnlyTask<*>>().observable()
    private val taskProperties = mutableListOf<TaskProperties>()

    override val currentlyRunningTaskChannel = BroadcastEventChannel.conflated<ReadOnlyTask<*>?>(null)

    private val taskView = VBox().apply {
        spacing = 5.0
        alignment = Pos.CENTER_LEFT
        tasks.performing {
            replaceChildren {
                hbox(spacing = 5) {
                    paddingAll = 5
                    useMaxWidth = true
                    alignment = Pos.CENTER_LEFT
                    label(mainTaskProperties.title) { addClass(Style.taskTitle) }
                    if (mainTask?.type == TaskType.Long) {
                        spacer()
                        toolbarButton("Cancel") {
                            addClass(CommonStyle.thinBorder)
                            isCancelButton = true
                            setOnAction {
                                currentJob!!.cancel()
                            }
                        }
                    }
                }
                separator()
                gridpane {
                    paddingAll = 5
                    paddingLeft = 20
                    paddingBottom = 20
                    hgap = 5.0
                    vgap = 5.0
                    alignment = Pos.CENTER_LEFT

                    taskProperties.forEach { properties ->
                        row {
                            label(properties.message1)
                            label(properties.message2)
                            progressindicator(properties.progress)
                            // TODO: Add a progress counter after the progressBar.
                        }
                    }
                }
            }
        }
    }

    fun init(f: EventTarget.() -> Node) = NotificationPane().apply {
        notificationPane = this
        isCloseButtonVisible = false
        isShowFromTop = true
        graphic = taskView
        content = stackpane {
            f()
            maskerPane {
                visibleWhen { currentJobProperty.isNotNull }
                progressProperty().bind(mainTaskProperties.progress)
                textProperty().bind(mainTaskProperties.title)
            }
        }
    }

    override suspend fun <T> runTask(task: ReadOnlyTask<T>): T = withContext(JavaFx) {
        check(currentJob == null) { "Already running a job: $currentJob" }

        try {
            mainTask = task
            mainTaskProperties.bind(task)
            taskProperties += mainTaskProperties
            tasks += task
            javaFx {
                task.subTasks.consumeEach { task ->
                    taskProperties += TaskProperties(task)
                    tasks += task
                }
            }
            if (task.type != TaskType.Quick) {
                showPersistentNotification()
            }

            currentlyRunningTaskChannel.send(task)
            // FIXME: Remove this! Should be handled entirely by the task. Should task return a Deferred?
            async(CommonPool) {
                task.run()
            }.apply {
                currentJob = this
            }.await()
        } finally {
            currentJob = null
            currentlyRunningTaskChannel.send(null)
            if (task.type != TaskType.Quick) {
                hidePersistentNotification()
            }
            taskProperties.forEach { it.close() }
            taskProperties.clear()
            tasks.clear()

            // This is OMFG. Showing the notification as part of the regular flow (not in a new coroutine)
            // causes an issue with modal windows not reporting that it is being hidden.
            javaFx {
                delay(1)
                showInfoNotification(task.doneMessage.await())
            }
        }
    }

    private fun showPersistentNotification() {
        notificationPane.show()
    }

    private fun hidePersistentNotification() {
        notificationPane.hide()
    }

    // FIXME: Showing notifications causes the FontAwesome glyphs to bug up for the notification Pane. Maybe use BootstrapFx instead?
    private fun showInfoNotification(text: String) = Notification()
        .text(text)
        .information()
        .automaticallyHideAfter(3.seconds)
        .hideCloseButton()
        .position(Pos.BOTTOM_RIGHT)
        .show()

    private inner class TaskProperties() {
        constructor(task: ReadOnlyTask<*>) : this() {
            bind(task)
        }

        val title = SimpleStringProperty("")
        val message1 = SimpleStringProperty("")
        val message2 = SimpleStringProperty("")
        val progress = SimpleDoubleProperty(0.0)

        fun bind(task: ReadOnlyTask<*>) {
            title.value = task.title
            message1.bind(task.message1Channel.toObservableValue(""))
            message2.bind(task.message2Channel.toObservableValue(""))
            progress.bind(task.progressChannel.toObservableValue(0.0))
        }

        fun close() {
            progress.unbind()
            message2.unbind()
            message1.unbind()
            title.value = ""
        }
    }

    class Style : Stylesheet() {
        companion object {
            val taskTitle by cssclass()

            init {
                importStylesheetSafe(Style::class)
            }
        }

        init {
            taskTitle {
                fontWeight = FontWeight.BOLD
            }
        }
    }
}