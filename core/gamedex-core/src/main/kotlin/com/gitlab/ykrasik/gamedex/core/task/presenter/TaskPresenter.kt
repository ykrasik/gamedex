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

package com.gitlab.ykrasik.gamedex.core.task.presenter

import com.gitlab.ykrasik.gamedex.app.api.task.TaskProgress
import com.gitlab.ykrasik.gamedex.app.api.task.TaskView
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.task.Task
import com.gitlab.ykrasik.gamedex.core.task.TaskFinishedEvent
import com.gitlab.ykrasik.gamedex.core.task.TaskStartedEvent
import com.gitlab.ykrasik.gamedex.util.logger
import com.gitlab.ykrasik.gamedex.util.toHumanReadableDuration
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.async
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 30/10/2018
 * Time: 15:21
 */
@Singleton
class TaskPresenter @Inject constructor(private val eventBus: EventBus) : Presenter<TaskView> {
    private val log = logger()

    override fun present(view: TaskView) = object : ViewSession() {
        init {
            eventBus.forEach<TaskStartedEvent<*>> { execute(it.task) }
            view.cancelTaskActions.forEach { cancelTask() }
        }

        private suspend fun <T> execute(task: Task<T>) {
            if (view.job != null) {
                log.warn("Trying to execute a task(${task.title}) when one is already executing(${view.taskProgress.title})")
                eventBus.awaitEvent(TaskFinishedEvent::class)
                log.warn("Previous task(${view.taskProgress.title}) finished, executing new task(${task.title})")
            }
            check(view.job == null) { "Already running a job: ${view.taskProgress.title}" }

            view.isCancellable = task.isCancellable
            view.isRunningSubTask = false
            bindTaskProgress(task, view.taskProgress)
            // TODO: It's possible that before this coroutine is launched the subtask will send some messages to its message channel that will be lost.
            task.subTaskChannel.forEach { subTask ->
                if (subTask != null) {
                    bindTaskProgress(subTask, view.subTaskProgress)
                }
                view.isRunningSubTask = subTask != null
            }

            val start = System.currentTimeMillis()
            val result = async(Dispatchers.IO) {
                task.execute()
            }
            var success = true
            var cancelled = false
            try {
                view.job = result
                result.await()
            } catch (e: Exception) {
                success = false
                if (e is CancellationException) {
                    cancelled = true
                }
            } finally {
                val millisTaken = System.currentTimeMillis() - start
                if (success) {
                    view.taskProgress.progress = 1.0
                    view.subTaskProgress.progress = 1.0
                }
                view.job = null
                view.isCancellable = false
                view.isRunningSubTask = false
                view.taskProgress.image = null
                view.subTaskProgress.image = null
                when {
                    success -> {
                        val successMessage = task.successMessage?.invoke()
                        successMessage?.let(view::taskSuccess)
                        log.info("${successMessage ?: "${task.title} Done:"} [${millisTaken.toHumanReadableDuration()}]")
                    }
                    cancelled -> {
                        val cancelMessage = task.cancelMessage?.invoke()
                        cancelMessage?.let(view::taskCancelled)
                        log.info("${cancelMessage ?: "${task.title} Cancelled:"} [${millisTaken.toHumanReadableDuration()}]")
                    }
                }
            }
            eventBus.send(TaskFinishedEvent(task, result))
        }

        private fun bindTaskProgress(task: Task<*>, taskProgress: TaskProgress) {
            taskProgress.title = task.title
            message(task.title, taskProgress)
            task.messageChannel.forEach { message(it, taskProgress) }

            taskProgress.totalItems = task.totalItems
            task.totalItemsChannel.forEach { taskProgress.totalItems = it }

            taskProgress.processedItems = task.processedItems
            task.processedItemsChannel.forEach { processedItems ->
                taskProgress.processedItems = processedItems

                if (task.totalItems > 1) {
                    taskProgress.progress = processedItems.toDouble() / task.totalItems
//                    log.debug("Progress: $processedItems/${task.totalItems} ${String.format("%.3f", taskProgress.progress * 100)}%")
                }
            }

            taskProgress.progress = -1.0

            taskProgress.image = task.image
            task.imageChannel.forEach { taskProgress.image = it }
        }

        private fun message(msg: String, taskProgress: TaskProgress) {
            log.info(msg)
            taskProgress.message = msg
        }

        private fun cancelTask() {
            val job = checkNotNull(view.job) { "Cannot cancel, not running any job!" }
            check(view.isCancellable) { "Cannot cancel, current job is non-cancellable: ${view.job}" }
            job.cancel()
        }
    }
}