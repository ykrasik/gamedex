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
interface SettingsStorageFactory {
    operator fun <T : Any> invoke(name: String, klass: KClass<T>, default: () -> T): SettingsStorage<T>
}

object FileSettingsStorageFactory : SettingsStorageFactory {
    override fun <T : Any> invoke(name: String, klass: KClass<T>, default: () -> T) =
        FileSettingsStorage(name, default(), klass)
}

interface SettingsStorage<T : Any> {
    var data: T
    fun modify(f: T.() -> T) {
        data = f(data)
    }

    val dataChannel: BroadcastReceiveChannel<T>
    fun <R> channel(extractor: KProperty1<T, R>): BroadcastReceiveChannel<R>

    fun perform(f: (T) -> Unit)

    fun saveSnapshot()
    fun restoreSnapshot()
    fun clearSnapshot()

    fun disableWrite()
    fun enableWrite()
    fun flush()
    fun resetDefaults()
}

class FileSettingsStorage<T : Any>(private val name: String,
                                   private val default: T,
                                   klass: KClass<T>) : SettingsStorage<T> {
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
        } ?: default.apply { update(this) }
    }
    private val _dataChannel = rawDataChannel.distinctUntilChanged()
    override val dataChannel = _dataChannel
    override var data: T by rawDataChannel

    private var snapshot: T? = null

    init {
        launch(settingsHandler) {
            _dataChannel.subscribe().drop(1).consumeEach {
                if (enableWrite) {
                    update(it)
                }
            }
        }
    }

    private fun update(data: T) = launch(settingsHandler) {
        file.create()
        log.trace("[$file] Writing: $data")
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, data)
    }

    override fun perform(f: (T) -> Unit) {
        f(data)
        launch(settingsHandler) {
            _dataChannel.subscribe().drop(1).consumeEach(f)
        }
    }

    override fun <R> channel(extractor: KProperty1<T, R>): BroadcastReceiveChannel<R> {
        val channel = BroadcastEventChannel.conflated(extractor(data))
        launch(settingsHandler) {
            _dataChannel.subscribe().drop(1).consumeEach {
                channel.send(extractor(it))
            }
        }
        return channel.distinctUntilChanged().apply {
            subscribe(settingsHandler) {
                log.debug("[$name] ${extractor.name} = $it")
            }
        }
    }

    override fun saveSnapshot() {
        snapshot = data
    }

    override fun restoreSnapshot() {
        data = snapshot!!
    }

    override fun clearSnapshot() {
        snapshot = null
    }

    override fun disableWrite() {
        enableWrite = false
    }

    override fun enableWrite() {
        enableWrite = true
    }

    override fun flush() {
        update(data)
    }

    override fun resetDefaults() {
        data = default
    }

    companion object {
        private val settingsHandler = Executors.newSingleThreadScheduledExecutor {
            Thread(it, "SettingsHandler").apply { isDaemon = true }
        }.asCoroutineDispatcher()

        inline operator fun <reified T : Any> invoke(name: String, default: () -> T): FileSettingsStorage<T> = FileSettingsStorage(name, default(), T::class)
    }
}

abstract class SettingsRepository<T : Any> {
    protected abstract val storage: SettingsStorage<T>

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