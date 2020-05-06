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

package com.gitlab.ykrasik.gamedex.core.storage

import com.gitlab.ykrasik.gamedex.app.api.util.ConflatedMultiChannel
import com.gitlab.ykrasik.gamedex.app.api.util.MultiReadChannel
import com.gitlab.ykrasik.gamedex.app.api.util.StatefulChannel
import com.gitlab.ykrasik.gamedex.app.api.util.conflatedChannel
import com.gitlab.ykrasik.gamedex.util.file
import com.gitlab.ykrasik.gamedex.util.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.drop
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.reflect.KProperty1

/**
 * User: ykrasik
 * Date: 02/10/2019
 * Time: 21:47
 */
interface StorageObservable<T> : StatefulChannel<T> {
    fun <R> channel(extractor: KProperty1<T, R>): MultiReadChannel<R>
    fun <R> biChannel(extractor: KProperty1<T, R>, reverseMapper: T.(R) -> T): StatefulChannel<R>

    fun onChange(f: suspend (T) -> Unit)
}

/**
 * A [StatefulChannel] which synchronizes its value with a [Storage].
 */
class StorageObservableImpl<T>(
    private val channel: ConflatedMultiChannel<T>,
    private val storage: Storage<String, T>,
    private val key: String,
    private val default: () -> T
) : StatefulChannel<T> by channel, StorageObservable<T>, CoroutineScope {
    override val coroutineContext = dispatcher

    private val distinctValueChannel = channel.distinctUntilChanged()

    private val log = logger("Storage")
    private val displayName = "$storage/$key".file.toString()

    private var enableWrite = true

    init {
        val existingValue = runCatching { storage[key] }.getOrElse { e ->
            log.error("[$displayName] Error!", e)
            null
        }
        channel *= if (existingValue == null) {
            log.info("[$displayName] Creating default...")
            val defaultValue = default()
            storage[key] = defaultValue
            defaultValue
        } else {
            existingValue
        }

        launch {
            distinctValueChannel.subscribe().drop(1).consumeEach { value ->
                if (enableWrite) {
                    storage[key] = value
                }
            }
        }
    }

    override fun <R> channel(extractor: KProperty1<T, R>): MultiReadChannel<R> {
        return distinctValueChannel.map { extractor(it) }.apply {
            subscribe(dispatcher) {
                log.debug("[$displayName] ${extractor.name} = $it")
            }
        }
    }

    override fun <R> biChannel(extractor: KProperty1<T, R>, reverseMapper: T.(R) -> T): StatefulChannel<R> {
        val channel = conflatedChannel<R>()
        distinctValueChannel.subscribe(dispatcher, start = CoroutineStart.UNDISPATCHED) {
            channel.send(extractor(it))
        }
        channel.distinctUntilChanged().subscribe(dispatcher) {
            log.debug("[$displayName] ${extractor.name} = $it")
            value = reverseMapper(value, it)
        }
        return channel
    }

    override fun onChange(f: suspend (T) -> Unit) {
        distinctValueChannel.drop(1).subscribe(dispatcher, f = f)
    }

    fun enableWrite() {
        enableWrite = true
    }

    fun disableWrite() {
        enableWrite = false
    }

    fun flushValueToStorage() {
        storage[key] = value
    }

    fun resetDefault() {
        value = default()
    }

    override fun close() {
        distinctValueChannel.close()
        channel.close()
    }

    companion object {
        private val dispatcher = Executors.newSingleThreadScheduledExecutor {
            Thread(it, "StorageHandler").apply { isDaemon = true }
        }.asCoroutineDispatcher()
    }
}