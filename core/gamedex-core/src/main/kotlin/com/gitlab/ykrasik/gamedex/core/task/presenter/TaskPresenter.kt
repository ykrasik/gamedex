/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.core.*
import com.gitlab.ykrasik.gamedex.core.task.ExpectedException
import com.gitlab.ykrasik.gamedex.core.task.Task
import com.gitlab.ykrasik.gamedex.core.task.TaskEvent
import com.gitlab.ykrasik.gamedex.util.Try
import com.gitlab.ykrasik.gamedex.util.humanReadable
import com.gitlab.ykrasik.gamedex.util.logger
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.measureTimedValue

/**
 * User: ykrasik
 * Date: 30/10/2018
 * Time: 15:21
 */
@Singleton
class TaskPresenter @Inject constructor(private val eventBus: EventBus) : Presenter<TaskView> {
    private val log = logger()

    override fun present(view: TaskView) = object : ViewSession() {
        private var job by view.job

        init {
            eventBus.flowOf<TaskEvent.RequestStart<*>>().forEach(debugName = "onTask") {
                execute(it.task)
            }
            view.cancelTaskActions.forEach(debugName = "onCancelTask") { cancelTask() }
        }

        private suspend fun <T> execute(task: Task<T>) {
            if (job != null) {
                log.warn("Trying to execute a task(${task.title}) when one is already executing(${view.taskProgress.title})")
                eventBus.awaitEvent<TaskEvent.Finished<T>>()
                log.warn("Previous task(${view.taskProgress.title}) finished, executing new task(${task.title})")
            }
            check(job == null) { "Already running a job: ${view.taskProgress.title}" }

            view.isCancellable *= task.isCancellable
            view.isRunningSubTask *= false
            bindTaskProgress(task, view.taskProgress)
            // TODO: It's possible that before this coroutine is launched the subtask will send some messages to its message channel that will be lost.
            task.subTask.forEach { subTask ->
                if (subTask != null) {
                    bindTaskProgress(subTask, view.subTaskProgress)
                }
                view.isRunningSubTask *= subTask != null
            }

            val (result, timeTaken) = measureTimedValue {
                val deferred = GlobalScope.async(Dispatchers.IO + CoroutineName(task.title)) {
                    task.execute()
                }
                job = deferred
                Try { deferred.await() }
            }

            view.job.value = null
            view.isCancellable *= false
            view.isRunningSubTask *= false
            view.taskProgress.image.value = null
            view.subTaskProgress.image.value = null

            var resultToReturn = result
            when (result) {
                is Try.Success -> {
                    view.taskProgress.progress *= 1.0
                    view.subTaskProgress.progress *= 1.0

                    val successMessage = task.successMessage?.invoke(result.value)
                    successMessage?.let { view.taskSuccess(title = task.title, message = it) }
                    log.info("${successMessage ?: "${task.title} Done:"} [${timeTaken.humanReadable}]")
                }
                is Try.Failure -> {
                    when (val error = result.error) {
                        is CancellationException -> {
                            val cancelMessage = task.cancelMessage?.invoke()
                            cancelMessage?.let { view.taskCancelled(title = task.title, message = it) }
                            log.info("${cancelMessage ?: "${task.title} Cancelled:"} [${timeTaken.humanReadable}]")

                            // Cancellation exceptions should not be treated as unexpected errors.
                            resultToReturn = Try.error(ExpectedException(error))
                        }
                        else -> {
                            val errorMessage = task.errorMessage?.invoke(error)
                            if (errorMessage != null) {
                                // The presence of an error message means the taskView should display this error, and not treat it as an unexpected error.
                                view.taskError(title = task.title, error = error, message = errorMessage)
                                resultToReturn = Try.error(ExpectedException(error))
                            }
                            log.error("${errorMessage ?: "${task.title} Error:"} [${timeTaken.humanReadable}]", error)
                        }
                    }
                }
            }

            eventBus.send(TaskEvent.Finished(task, resultToReturn))
        }

        private fun bindTaskProgress(task: Task<*>, taskProgress: TaskProgress) {
            taskProgress.title.value = task.title

            taskProgress.message.value = task.title
            taskProgress.message *= task.message.drop(1).onEach { msg -> log.info(msg) } withDebugName "message"

            taskProgress.totalItems *= task.totalItems withDebugName "totalItems"

            taskProgress.processedItems *= task.processedItemsFlow withDebugName "processedItems"

            taskProgress.progress *= task.processedItemsFlow.combine(task.totalItems) { processedItems, totalItems ->
                if (totalItems > 1) {
                    processedItems.toDouble() / totalItems
                } else {
                    -1.0
                }
            } withDebugName "progress"

            taskProgress.image *= task.image withDebugNameWithoutTrace "image"
        }

        private fun cancelTask() {
            val job = checkNotNull(job) { "Cannot cancel, not running any job!" }
            check(view.isCancellable.value) { "Cannot cancel, current job is non-cancellable: $job" }
            job.cancel()
        }
    }
}