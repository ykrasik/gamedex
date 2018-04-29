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
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.withContext
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
    private val notificationPane = NotificationPane().apply { isCloseButtonVisible = false }

    private val currentJobProperty = SimpleObjectProperty<Job?>(null)
    private var currentJob by currentJobProperty

    private var longRunningNotification: Notification? = null

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
                hbox(spacing = 5.0) {
                    paddingAll = 5.0
                    useMaxWidth = true
                    alignment = Pos.CENTER_LEFT
                    label(mainTaskProperties.title)
                    if (mainTask?.type == TaskType.Long) {
                        jfxButton("Cancel") {
                            addClass(CommonStyle.thinBorder)
                            useMaxWidth = true
                            hgrow = Priority.ALWAYS
                            isCancelButton = true
                            setOnAction {
                                currentJob!!.cancel()
                            }
                        }
                    }
                }
                separator()
                gridpane {
                    paddingAll = 5.0
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

    // FIXME: fix this, the notificationPane is redundant.
    fun <T : Parent> init(f: () -> T): NotificationPane {
        notificationPane.content = StackPane().apply {
            children += f()
            maskerPane {
                visibleWhen { currentJobProperty.isNotNull }
                progressProperty().bind(mainTaskProperties.progress)
                textProperty().bind(mainTaskProperties.title)
            }
        }
        return notificationPane
    }

    val canRunTaskProperty = notificationPane.showingProperty().not()

    override suspend fun <T> runTask(task: ReadOnlyTask<T>): T = withContext(JavaFx) {
        require(currentJob == null) { "Already running a job: $currentJob" }

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
            showInfoNotification(task.doneMessage.await())
        }
    }

    private fun showPersistentNotification() {
        longRunningNotification = Notification()
            .graphic(taskView)
            .hideCloseButton()
            .position(Pos.TOP_LEFT)

        longRunningNotification!!.show()

//        notificationPane.graphic = taskView
//        notificationPane.show()
    }

    private fun hidePersistentNotification() {
        longRunningNotification!!.hide()
        longRunningNotification = null

//        notificationPane.hide()
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
}