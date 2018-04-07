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

package com.gitlab.ykrasik.gamedex.core.api.task

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.core.api.util.conflatedChannel
import com.gitlab.ykrasik.gamedex.core.api.util.getValue
import com.gitlab.ykrasik.gamedex.core.api.util.setValue
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import java.util.concurrent.atomic.AtomicInteger

/**
 * User: ykrasik
 * Date: 31/03/2018
 * Time: 19:35
 */
// TODO: Split into QuickTask, LongTask & LongCancellableTask
interface ReadOnlyTask {
    val processed: Int
    val totalWork: Int?

    val titleChannel: ReceiveChannel<String>
    val progressChannel: ReceiveChannel<Double>
    val messageChannel: ReceiveChannel<String>
    val platformChannel: ReceiveChannel<Platform>
    val providerChannel: ReceiveChannel<ProviderId>

    fun doneMessage(success: Boolean): String?

    val subTask: ReadOnlyTask
}

class Task : ReadOnlyTask {
    private val _processed = AtomicInteger(0)
    override val processed: Int get() = _processed.get()

    override var totalWork: Int? = null
        set(value) {
            field = value
            _processed.set(0)
        }

    private val _titleChannel = conflatedChannel<String>()
    var title by _titleChannel
    override val titleChannel: ReceiveChannel<String> = _titleChannel

    private val _progressChannel = conflatedChannel<Double>()
    var progress by _progressChannel
    override val progressChannel: ReceiveChannel<Double> = _progressChannel

    private val _messageChannel = conflatedChannel<String>()
    var message by _messageChannel
    override val messageChannel: ReceiveChannel<String> = _messageChannel

    private val _platformChannel = conflatedChannel<Platform>()
    var platform by _platformChannel
    override val platformChannel: ReceiveChannel<Platform> = _platformChannel

    private val _providerChannel = conflatedChannel<ProviderId>()
    var provider by _providerChannel
    override val providerChannel: ReceiveChannel<ProviderId> = _providerChannel

    // TODO: Differentiate between error termination & cancel?
    var doneMessage: ((success: Boolean) -> String)? = null
    fun doneMessageOrCancelled(message: String) {
        doneMessage = { success -> if (success) message else "Cancelled" }
    }

    override fun doneMessage(success: Boolean) = doneMessage?.invoke(success)

    private val lazySubTask = lazy { Task() }
    override val subTask by lazySubTask

    fun progress(done: Int, total: Int) {
        progress = done.toDouble() / total.toDouble()
    }

    fun incProgress() = progress(_processed.incrementAndGet(), totalWork!!)
    inline fun <T> incProgress(f: () -> T): T = f().apply { incProgress() }

    inline fun <T, R> List<T>.mapWithProgress(f: (T) -> R): List<R> {
        totalWork = size
        return map { incProgress { f(it) } }
    }

    inline fun <T, R> List<T>.flatMapWithProgress(f: (T) -> List<R>): List<R> {
        totalWork = size
        return flatMap { incProgress { f(it) } }
    }

    inline fun <T> List<T>.filterWithProgress(f: (T) -> Boolean): List<T> {
        totalWork = size
        return filter { incProgress { f(it) } }
    }

    inline fun <T> List<T>.forEachWithProgress(f: (T) -> Unit) {
        totalWork = size
        return forEach { incProgress { f(it) } }
    }

    fun close() {
        _messageChannel.close()
        _progressChannel.close()
        _platformChannel.close()
        _providerChannel.close()
        if (lazySubTask.isInitialized()) {
            subTask.close()
        }
    }
}

// FIXME: Finish this.
class RunnableTask<out T>(val task: Task, private val run: suspend Task.() -> T) {
//    suspend fun
}