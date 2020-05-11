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

package com.gitlab.ykrasik.gamedex.core.task

import com.gitlab.ykrasik.gamedex.app.api.image.Image
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger

/**
 * User: ykrasik
 * Date: 31/03/2018
 * Time: 19:35
 */
class Task<T>(
    val title: String,
    val isCancellable: Boolean,
    initialImage: Image?,
    private val run: suspend Task<*>.() -> T
) {
    val message = MutableStateFlow("")

    val processedItemsFlow = MutableStateFlow(0)
    private val _processedItems = AtomicInteger(0)
    var processedItems: Int
        get() = _processedItems.get()
        set(value) = withProcessedItems { it.set(value); value }

    fun incProgress(amount: Int = 1) = withProcessedItems { it.addAndGet(amount) }

    private inline fun withProcessedItems(f: (AtomicInteger) -> Int) {
        val value = f(_processedItems)
        processedItemsFlow.value = value
    }

    val totalItems = MutableStateFlow(0)

    val image = MutableStateFlow(initialImage)

    val subTask = MutableStateFlow<Task<*>?>(null)

    var successMessage: ((T) -> String)? = { "Done." }
    var cancelMessage: (() -> String)? = { "Cancelled." }

    // Setting this value to non-null will mean the task view will display this exception.
    // The exception will still propagate up and stop code from executing, but it should not be handled by the default uncaught exception handler.
    // This is to allow tasks to display errors that are expected in a nicer way.
    var errorMessage: ((Exception) -> String)? = null

    inline fun successOrCancelledMessage(crossinline f: (success: Boolean) -> String) {
        successMessage = { f(true) }
        cancelMessage = { f(false) }
    }

    private val lock = Mutex()
    suspend fun execute(): T = lock.withLock {
        run()
    }

    suspend fun <R> executeSubTask(task: Task<R>): R {
        subTask.value = task
        return try {
            task.execute()
        } finally {
            subTask.value = null
        }
    }

    inline fun <R> withMessage(message: String, f: () -> R): R {
        this.message.value = message
        val result = f()
        this.message.value = "$message Done."
        return result
    }

    inline fun <R> withImage(image: Image, f: () -> R): R {
        val prevImage = this.image
        this.image.value = image
        return try {
            f()
        } finally {
            this.image.value = prevImage.value
        }
    }

    inline fun <T, R> List<T>.mapWithProgress(f: (T) -> R): List<R> {
        totalItems.value = size
        return map { incProgress(); f(it) }
    }

    inline fun <T, R : Any> List<T>.mapNotNullWithProgress(f: (T) -> R?): List<R> {
        totalItems.value = size
        return mapNotNull { incProgress(); f(it) }
    }

    inline fun <T, R> List<T>.flatMapWithProgress(f: (T) -> List<R>): List<R> {
        totalItems.value = size
        return flatMap { incProgress(); f(it) }
    }

    inline fun <T> List<T>.filterWithProgress(f: (T) -> Boolean): List<T> {
        totalItems.value = size
        return filter { incProgress(); f(it) }
    }

    inline fun <T> List<T>.forEachWithProgress(f: (T) -> Unit) {
        totalItems.value = size
        return forEach { incProgress(); f(it) }
    }

    override fun toString() = title
}

fun <T> task(
    title: String = "",
    isCancellable: Boolean = false,
    initialImage: Image? = null,
    run: suspend Task<*>.() -> T
): Task<T> = Task(title, isCancellable, initialImage, run)

class ExpectedException(cause: Exception) : RuntimeException(cause)