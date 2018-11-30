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
import com.gitlab.ykrasik.gamedex.core.storage.JsonStorageFactory
import com.gitlab.ykrasik.gamedex.core.storage.Storage
import com.gitlab.ykrasik.gamedex.util.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.drop
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * User: ykrasik
 * Date: 09/06/2018
 * Time: 21:39
 */
abstract class SettingsRepository<T : Any> {
    protected abstract val storage: SettingsStorage<T>

    val dataChannel: BroadcastReceiveChannel<T> get() = storage.dataChannel

    fun modify(f: T.() -> T) = storage.modify(f)
    fun perform(f: (T) -> Unit) = storage.perform(f)
    fun saveSnapshot() = storage.saveSnapshot()
    fun restoreSnapshot() = storage.restoreSnapshot()
    fun clearSnapshot() = storage.clearSnapshot()

    fun disableWrite() = storage.disableWrite()
    fun enableWrite() = storage.enableWrite()

    fun flush() = storage.flush()

    fun resetDefaults() = storage.resetDefaults()
}

class SettingsStorage<T : Any>(
    basePath: String,
    private val name: String,
    private val default: T,
    private val storage: Storage<String, T>
) : CoroutineScope {
    override val coroutineContext = settingsHandler

    private val log = logger()
    private val displayName = File("$basePath/$name").normalize().toString()

    private var enableWrite = true

    private val rawDataChannel = BroadcastEventChannel.conflated {
        val existingData = try {
            storage[name]
        } catch (e: Exception) {
            log.error("[$name] Error!", e)
            null
        }
        if (existingData == null) {
            log.info("[$displayName] Creating default settings...")
            storage[name] = default
            default
        } else {
            existingData
        }
    }

    private val _dataChannel = rawDataChannel.distinctUntilChanged()
    val dataChannel = _dataChannel
    var data: T by rawDataChannel

    private var snapshot: T? = null

    init {
        launch {
            _dataChannel.subscribe().drop(1).consumeEach {
                if (enableWrite) {
                    storage[name] = it
                }
            }
        }
    }

    fun modify(f: T.() -> T) {
        data = f(data)
    }

    fun perform(f: (T) -> Unit) {
        f(data)
        launch {
            _dataChannel.subscribe().drop(1).consumeEach(f)
        }
    }

    fun <R> channel(extractor: KProperty1<T, R>): BroadcastReceiveChannel<R> {
        val channel = BroadcastEventChannel.conflated(extractor(data))
        launch {
            _dataChannel.subscribe().drop(1).consumeEach {
                channel.send(extractor(it))
            }
        }
        return channel.distinctUntilChanged().apply {
            subscribe(settingsHandler) {
                log.debug("[$displayName] ${extractor.name} = $it")
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
        if (snapshot != data) {
            launch {
                storage[name] = data
            }
        }
    }

    fun resetDefaults() {
        data = default
    }

    companion object {
        private val settingsHandler = Executors.newSingleThreadScheduledExecutor {
            Thread(it, "SettingsHandler").apply { isDaemon = true }
        }.asCoroutineDispatcher()
    }
}

class SettingsStorageFactory(private val basePath: String, private val factory: JsonStorageFactory<String>) {
    operator fun <T : Any> invoke(name: String, klass: KClass<T>, default: () -> T): SettingsStorage<T> {
        val storage = factory(basePath, klass)
        return SettingsStorage(basePath, name, default(), storage)
    }
}