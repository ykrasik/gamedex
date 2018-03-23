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

import com.gitlab.ykrasik.gamdex.core.api.util.*
import com.gitlab.ykrasik.gamedex.util.*
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.launch
import java.util.concurrent.Executors
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 11/10/2016
 * Time: 10:34
 */
class SettingsRepo<T : Any>(name: String, klass: KClass<T>, default: () -> T) {
    private val file = "conf/$name.json".toFile()

    private var enableWrite = true

    private val dataSubject = run {
        val data = if (file.exists()) {
            try {
                log.trace { "[$file] Reading settings file..." }
                file.readJson(klass)
            } catch (e: Exception) {
                log.error("[$file] Error reading settings! Resetting to default...", e)
                null
            }
        } else {
            log.trace { "[$file] Settings file doesn't exist. Creating default..." }
            null
        } ?: default().apply { update(this) }
        val observable = data.toBehaviorSubject()
        observable.skip(1).subscribe {
            if (enableWrite) {
                update(it!!)
            }
        }
        observable
    }
    private var data: T by dataSubject

    fun <R> subject(extractor: Extractor<T, R>, modifier: Modifier<T, R>): BehaviorSubject<R> =
        dataSubject.mapBidirectional(extractor, { data.modifier(this) }, uiThreadScheduler)

    private fun update(data: T) = launch(dispatcher) {
        file.create()
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, data)
    }

    private var snapshot: T? = null

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

    companion object {
        private val dispatcher = Executors.newSingleThreadScheduledExecutor {
            Thread(it, "SettingsWriter").apply { isDaemon = true }
        }.asCoroutineDispatcher()

        private val log = logger()

        inline operator fun <reified T : Any> invoke(name: String, noinline default: () -> T): SettingsRepo<T> = SettingsRepo(name, T::class, default)
    }
}