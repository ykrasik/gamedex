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
import com.gitlab.ykrasik.gamedex.app.api.util.BroadcastReceiveChannel
import com.gitlab.ykrasik.gamedex.util.*
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.drop
import kotlinx.coroutines.experimental.launch
import java.util.concurrent.Executors
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * User: ykrasik
 * Date: 09/06/2018
 * Time: 21:39
 */
abstract class SettingsRepository<T : Any>(private val name: String, klass: KClass<T>) {
    private val log = logger()
    private val file = "conf/$name.conf".toFile()

    private var enableWrite = true

    private val rawDataChannel = BroadcastEventChannel.conflated {
        if (file.exists()) {
            try {
                log.info("Reading $file...")
                log.time({ "Reading $file took $it" }) {
                    file.readJson(klass)
                }
            } catch (e: Exception) {
                log.error("Error reading $file! Resetting to default...", e)
                null
            }
        } else {
            log.info("[$file] Settings file doesn't exist. Creating default...")
            null
        } ?: defaultSettings().apply { update(this) }
    }
    private val dataChannel = rawDataChannel.distinctUntilChanged()
    private var data: T by rawDataChannel

    private var snapshot: T? = null

    init {
        launch(settingsHandler) {
            dataChannel.subscribe().drop(1).consumeEach {
                if (enableWrite) {
                    update(it)
                }
            }
        }
    }

    protected abstract fun defaultSettings(): T

    private fun update(data: T) = launch(settingsHandler) {
        file.create()
        log.trace("[$file] Writing: $data")
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, data)
    }

    fun modify(f: T.() -> T) {
        data = f(data)
    }

    fun <R> channel(extractor: KProperty1<T, R>): BroadcastReceiveChannel<R> {
        val channel = BroadcastEventChannel.conflated(extractor(data))
        launch(settingsHandler) {
            dataChannel.subscribe().drop(1).consumeEach {
                channel.send(extractor(it))
            }
        }
        return channel.distinctUntilChanged().apply {
            subscribe(settingsHandler) {
                log.debug("[$name] ${extractor.name} = $it")
            }
        }
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

    private companion object {
        val settingsHandler = Executors.newSingleThreadScheduledExecutor {
            Thread(it, "SettingsHandler").apply { isDaemon = true }
        }.asCoroutineDispatcher()
    }
}