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

package com.gitlab.ykrasik.gamdex.core.api.task

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.info
import com.gitlab.ykrasik.gamedex.util.logger
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import org.slf4j.Logger
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicInteger

/**
 * User: ykrasik
 * Date: 31/03/2018
 * Time: 19:35
 */
// TODO: Need to separate ui tasks from simple coroutines returned by services.
class Task<out T>(val title: String? = null,
                  private val run: suspend CoroutineScope.(Progress) -> T) {
    private val log = logger()

    private val _runningChannel = Channel<Boolean>(Channel.CONFLATED)
    val runningChannel: ReceiveChannel<Boolean> = _runningChannel

    private val _progress = Progress(log)
    val progress: ReadOnlyProgress = _progress

    private var beforeStart: suspend () -> Unit = { }
    private var afterEnd: suspend () -> Unit = { }
    private var onComplete: suspend (T) -> Unit = { }
    private var onCancelled: suspend () -> Unit = { }
    private var onError: suspend (Exception) -> Unit = { }

    private var job: Deferred<T>? = null

    suspend fun run(): T {
        require(job == null) { "$this is already running!" }
        job = async(CommonPool) {
            try {
                _runningChannel.offer(true)
                title?.let { log.info("Started: $it") }
                beforeStart()
                run(_progress).apply {
                    onComplete(this)
                }
            } catch (e: Exception) {
                when (e) {
                    is CancellationException -> onCancelled()
                    else -> onError(e)
                }
                throw e
            } finally {
                _runningChannel.offer(false)
                afterEnd()
                _progress.close()
                title?.let { log.info("Finished: $it") }
            }
        }

        return job!!.await()
    }

    fun cancel() = job!!.cancel()

    fun beforeStart(f: suspend () -> Unit): Task<T> = apply { beforeStart = f }
    fun afterEnd(f: suspend () -> Unit): Task<T> = apply { afterEnd = f }
    fun onComplete(f: suspend (T) -> Unit): Task<T> = apply { onComplete = f }
    fun onCancelled(f: suspend () -> Unit): Task<T> = apply { onCancelled = f }
    fun onError(f: suspend (Exception) -> Unit): Task<T> = apply { onError = f }

    override fun toString() = "Task($title)"
}

interface ReadOnlyProgress {
    val progressChannel: ReceiveChannel<Double>
    val messageChannel: ReceiveChannel<String>

    val doneMessage: String?    // TODO: Observable? Or maybe a function, so it can be lazy computed?

    // TODO: Figure out where these things belong.
    val platformChannel: ReceiveChannel<Platform>
    val providerChannel: ReceiveChannel<ProviderId>

    val processed: Int
    val totalWork: Int?

    val subProgress: ReadOnlyProgress
}

// TODO: Consider moving the message and doneMessage to the task.
class Progress(private val log: Logger?) : ReadOnlyProgress {
    private val _progressChannel = Channel<Double>(Channel.CONFLATED)
    override val progressChannel: ReceiveChannel<Double> = _progressChannel

    private val _messageChannel = Channel<String>(Channel.CONFLATED)
    override val messageChannel: ReceiveChannel<String> = _messageChannel

    private val _platformChannel = Channel<Platform>(Channel.CONFLATED)
    override val platformChannel: ReceiveChannel<Platform> = _platformChannel

    private val _providerChannel = Channel<ProviderId>(Channel.CONFLATED)
    override val providerChannel: ReceiveChannel<ProviderId> = _providerChannel

    override var doneMessage: String? = null

    private val _processed = AtomicInteger(0)
    override val processed: Int get() = _processed.get()

    override var totalWork: Int? = null
        set(value) {
            field = value
            _processed.set(0)
        }

    private val lazySubProgress = lazy { Progress(log) }
    override val subProgress by lazySubProgress

    fun progress(progress: Double) = _progressChannel.offer(progress)
    fun progress(done: Int, total: Int) = progress(done.toDouble() / total.toDouble())
    fun inc() = progress(_processed.incrementAndGet(), totalWork!!)
    inline fun <T> inc(f: () -> T): T = f().apply { inc() }

    fun message(message: String) {
        _messageChannel.offer(message)

        // TODO: This code doesn't really belong here, but whatever.
        if (log != null) {
            val prefix = "${_platformChannel.poll()?.let { "[$it]" }}${_providerChannel.poll()?.let { "[$it]" }}"
            log.info { if (prefix.isNotEmpty()) "$prefix $message" else message }
        }
    }

    fun platform(platform: Platform) = _platformChannel.offer(platform)
    fun provider(providerId: ProviderId) = _providerChannel.offer(providerId)

    fun close() {
        _messageChannel.close()
        _progressChannel.close()
        _platformChannel.close()
        _providerChannel.close()
        if (lazySubProgress.isInitialized()) {
            subProgress.close()
        }
    }
}