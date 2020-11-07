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

import com.gitlab.ykrasik.gamedex.util.logger
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.reflect.KProperty1

/**
 * User: ykrasik
 * Date: 02/10/2019
 * Time: 21:47
 *
 * A [MutableStateFlow] which is backed by a [Storage].
 */
class StorageMutableStateFlow<T> private constructor(
    private val storage: Storage<String, T>,
    private val key: String,
    private val default: () -> T,
    private val flow: MutableStateFlow<T>,
) : MutableStateFlow<T> by flow, CoroutineScope {
    override val coroutineContext = dispatcher

    private val log = logger("Storage")
    private val displayName = "$storage/$key".replace('\\', '/')

    private var enableWrite = true

    init {
        val existingValue = try {
            storage[key]
        } catch (e: Exception) {
            log.error("[$displayName] Error!", e)
            null
        }
        flow.value = if (existingValue == null) {
            log.info("[$displayName] Creating default...")
            default()
        } else {
            existingValue
        }

        launch(CoroutineName("writer-$displayName")) {
            flow.collect { value ->
                if (enableWrite) {
                    storage[key] = value
                }
            }
        }
    }

    fun <R> biMap(extractor: KProperty1<T, R>, reverseMapper: T.(R) -> T): MutableStateFlow<R> {
        val coroutineName = CoroutineName("$displayName/${extractor.name}")
        val mappedFlow = MutableStateFlow(extractor(flow.value))
        launch(coroutineName) {
            flow.collect {
                mappedFlow.value = extractor(it)
            }
        }
        launch(coroutineName) {
            mappedFlow.drop(1).collect {
                log.debug(it.toString())
                flow.value = reverseMapper(value, it)
            }
        }
        return mappedFlow
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

    override fun toString() = displayName

    companion object {
        private val dispatcher = Executors.newSingleThreadScheduledExecutor {
            Thread(it, "StorageHandler").apply { isDaemon = true }
        }.asCoroutineDispatcher()

        operator fun <T> invoke(
            storage: Storage<String, T>,
            key: String,
            default: () -> T,
        ) = StorageMutableStateFlow(storage, key, default, MutableStateFlow(default()))
    }
}