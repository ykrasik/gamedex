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

package com.gitlab.ykrasik.gamedex.core.task

import com.gitlab.ykrasik.gamedex.app.api.util.BroadcastEventChannel
import com.gitlab.ykrasik.gamedex.app.api.util.BroadcastReceiveChannel
import kotlinx.coroutines.experimental.isActive
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import java.util.concurrent.atomic.AtomicInteger
import kotlin.properties.Delegates

/**
 * User: ykrasik
 * Date: 31/03/2018
 * Time: 19:35
 */
class Task<out T>(val title: String, val isCancellable: Boolean, private val run: suspend Task<*>.() -> T) {
    private val _messageChannel = BroadcastEventChannel<String>()
    val messageChannel: BroadcastReceiveChannel<String> = _messageChannel
    var message by Delegates.observable("") { _, _, value -> _messageChannel.offer(value) }

    private val _processedItemsChannel = BroadcastEventChannel<Int>()
    val processedItemsChannel: BroadcastReceiveChannel<Int> = _processedItemsChannel
    private val _processedItems = AtomicInteger(0)
    var processedItems: Int
        get() = _processedItems.get()
        set(value) = withProcessedItems { it.set(value); value }

    fun incProgress(amount: Int = 1) = withProcessedItems { it.addAndGet(amount) }

    private inline fun withProcessedItems(f: (AtomicInteger) -> Int) {
        val value = f(_processedItems)
        _processedItemsChannel.offer(value)
    }

    private val _totalItemsChannel = BroadcastEventChannel<Int>().apply {
        subscribe {
            // When totalItems changes, reset processedItems.
            processedItems = 0
        }
    }
    val totalItemsChannel: BroadcastReceiveChannel<Int> = _totalItemsChannel
    var totalItems by Delegates.observable(0) { _, _, value -> _totalItemsChannel.offer(value) }

    private val _subTaskChannel = BroadcastEventChannel<Task<*>?>()
    val subTaskChannel: BroadcastReceiveChannel<Task<*>?> = _subTaskChannel

    var successMessage: (() -> String)? = null
    var cancelMessage: (() -> String)? = { "Cancelled" }

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

fun <T> task(title: String = "", isCancellable: Boolean = false, run: suspend Task<*>.() -> T): Task<T> =
    Task(title, isCancellable, run)