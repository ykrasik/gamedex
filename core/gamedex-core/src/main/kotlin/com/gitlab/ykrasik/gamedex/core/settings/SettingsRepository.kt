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

package com.gitlab.ykrasik.gamedex.core.settings

import com.gitlab.ykrasik.gamedex.app.api.util.BroadcastEventChannel
import com.gitlab.ykrasik.gamedex.app.api.util.distinctUntilChanged
import com.gitlab.ykrasik.gamedex.util.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.drop
import kotlinx.coroutines.experimental.channels.map
import kotlinx.coroutines.experimental.launch
import java.util.concurrent.Executors
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 09/06/2018
 * Time: 21:39
 */
abstract class SettingsRepository<T : Any>(name: String, klass: KClass<T>) {
    private val file = "conf/$name.json".toFile()

    private var enableWrite = true

    @Suppress("LeakingThis")
    private var _data: T = if (file.exists()) {
        try {
            log.info("Reading $name settings...")
            log.time({ "Read $name settings in $it" }) {
                file.readJson(klass)
            }
        } catch (e: Exception) {
            log.error("Error reading $name settings! Resetting to default...", e)
            null
        }
    } else {
        log.info("[$file] Settings file doesn't exist. Creating default...")
        null
    } ?: defaultSettings().apply { update(this) }

    private val dataChannel = BroadcastEventChannel.conflated(_data)

    private var data: T
        get() = _data
        set(value) {
            this.dataChannel.offer(value)
        }

    private var snapshot: T? = null

    init {
        launch(CommonPool) {
            dataChannel.subscribe().drop(1).distinctUntilChanged().consumeEach {
                _data = it
                if (enableWrite) {
                    update(it)
                }
            }
        }
    }

    protected abstract fun defaultSettings(): T

    private fun update(data: T) = launch(dispatcher) {
        file.create()
        log.trace("Writing $file...")
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, data)
    }

    fun modify(f: (T) -> T) {
        data = f(data)
    }

    fun <R> map(extractor: Extractor<T, R>, modifier: Modifier<T, R>): BroadcastEventChannel<R> {
        val channel = BroadcastEventChannel.conflated(extractor(data))
        launch(CommonPool) {
            dataChannel.subscribe().map { extractor(it) }.distinctUntilChanged().consumeEach {
                channel.send(it)
            }
        }
        launch(CommonPool) {
            channel.subscribe().map { data.modifier(it) }.distinctUntilChanged().consumeEach {
                dataChannel.send(it)
            }
        }
        return channel
    }

    fun saveSnapshot() {
        snapshot = data
    }

    fun restoreSnapshot() {
        data = snapshot!!
    }

    fun clearSnapshot() {
        snapshot = null
    }

    fun disableWrite() {
        enableWrite = false
    }

    fun enableWrite() {
        enableWrite = true
    }

    fun flush() {
        update(data)
    }

    fun restoreDefaults() {
        data = defaultSettings()
    }

    companion object {
        private val dispatcher = Executors.newSingleThreadScheduledExecutor {
            Thread(it, "SettingsWriter").apply { isDaemon = true }
        }.asCoroutineDispatcher()

        private val log = logger()
    }
}