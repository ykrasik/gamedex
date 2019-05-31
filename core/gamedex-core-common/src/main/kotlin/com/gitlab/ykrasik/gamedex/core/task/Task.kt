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

package com.gitlab.ykrasik.gamedex.core.task

import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.app.api.util.MultiChannel
import com.gitlab.ykrasik.gamedex.app.api.util.MultiReceiveChannel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger
import kotlin.properties.Delegates

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
    // TODO: should probably be a conflated channel
    private val _messageChannel = MultiChannel<String>()
    val messageChannel: MultiReceiveChannel<String> = _messageChannel
    var message by Delegates.observable("") { _, _, value -> _messageChannel.offer(value) }

    // TODO: should probably be a conflated channel
    private val _processedItemsChannel = MultiChannel<Int>()
    val processedItemsChannel: MultiReceiveChannel<Int> = _processedItemsChannel
    private val _processedItems = AtomicInteger(0)
    var processedItems: Int
        get() = _processedItems.get()
        set(value) = withProcessedItems { it.set(value); value }

    fun incProgress(amount: Int = 1) = withProcessedItems { it.addAndGet(amount) }

    private inline fun withProcessedItems(f: (AtomicInteger) -> Int) {
        val value = f(_processedItems)
        _processedItemsChannel.offer(value)
    }

    // TODO: should probably be a conflated channel
    private val _totalItemsChannel = MultiChannel<Int>().apply {
        subscribe {
            // When totalItems changes, reset processedItems.
            processedItems = 0
        }
    }
    val totalItemsChannel: MultiReceiveChannel<Int> = _totalItemsChannel
    var totalItems by Delegates.observable(0) { _, _, value -> _totalItemsChannel.offer(value) }

    // TODO: should probably be a conflated channel
    private val _imageChannel = MultiChannel<Image?>()
    val imageChannel: MultiReceiveChannel<Image?> = _imageChannel
    var image by Delegates.observable(initialImage) { _, _, value -> _imageChannel.offer(value) }

    // TODO: should probably be a conflated channel
    private val _subTaskChannel = MultiChannel<Task<*>?>()
    val subTaskChannel: MultiReceiveChannel<Task<*>?> = _subTaskChannel

    var successMessage: ((T) -> String)? = null
    var cancelMessage: (() -> String)? = { "Cancelled" }

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
        check(_messageChannel.isActive) { "Task was already executed: $this[$title]" }

        return try {
            run()
        } finally {
            listOf(
                _messageChannel,
                _processedItemsChannel,
                _totalItemsChannel,
                _imageChannel,
                _subTaskChannel
            ).forEach { it.close() }
        }
    }

    suspend fun <R> executeSubTask(task: Task<R>): R {
        _subTaskChannel.send(task)
        return try {
            task.execute()
        } finally {
            _subTaskChannel.send(null)
        }
    }

    inline fun <R> withImage(image: Image, f: () -> R): R {
        val prevImage = this.image
        this.image = image
        return try {
            f()
        } finally {
            this.image = prevImage
        }
    }

    inline fun <T, R> List<T>.mapWithProgress(f: (T) -> R): List<R> {
        totalItems = size
        return map { f(it).apply { incProgress() } }
    }

    inline fun <T, R : Any> List<T>.mapNotNullWithProgress(f: (T) -> R?): List<R> {
        totalItems = size
        return mapNotNull { f(it).apply { incProgress() } }
    }

    inline fun <T, R> List<T>.flatMapWithProgress(f: (T) -> List<R>): List<R> {
        totalItems = size
        return flatMap { f(it).apply { incProgress() } }
    }

    inline fun <T> List<T>.filterWithProgress(f: (T) -> Boolean): List<T> {
        totalItems = size
        return filter { f(it).apply { incProgress() } }
    }

    inline fun <T> List<T>.forEachWithProgress(f: (T) -> Unit) {
        totalItems = size
        return forEach { f(it).apply { incProgress() } }
    }
}

fun <T> task(
    title: String = "",
    isCancellable: Boolean = false,
    initialImage: Image? = null,
    run: suspend Task<*>.() -> T
): Task<T> = Task(title, isCancellable, initialImage, run)