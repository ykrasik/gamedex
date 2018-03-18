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

package com.gitlab.ykrasik.gamedex.settings

import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.util.*
import javafx.beans.property.*
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.launch
import tornadofx.getValue
import tornadofx.onChange
import tornadofx.setValue
import tornadofx.toProperty
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

    private val dataProperty = run {
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
        }
        val property = (data ?: default().apply { update(this) }).toProperty()
        property.onChange {
            // TODO: If all settings update access is done through SettingsController, there's no need for this listener - controller flushes itself.
            if (enableWrite) {
                update(it!!)
            }
        }
        property
    }
    private var data: T by dataProperty

    // TODO: Consider making all these properties ThreadAware by default.
    fun <R> property(extractor: Extractor<T, R>, modifier: Modifier<T, R>): ObjectProperty<R> =
        dataProperty.mapBidirectional(extractor) { data.modifier(this) }

    fun booleanProperty(extractor: Extractor<T, Boolean>, modifier: Modifier<T, Boolean>): BooleanProperty =
        dataProperty.mapBidirectionalBoolean(extractor) { data.modifier(this) }

    fun stringProperty(extractor: Extractor<T, String>, modifier: Modifier<T, String>): StringProperty =
        dataProperty.mapBidirectionalString(extractor) { data.modifier(this) }

    fun intProperty(extractor: Extractor<T, Int>, modifier: Modifier<T, Int>): IntegerProperty =
        dataProperty.mapBidirectionalInt(extractor) { data.modifier(this) }

    fun doubleProperty(extractor: Extractor<T, Double>, modifier: Modifier<T, Double>): DoubleProperty =
        dataProperty.mapBidirectionalDouble(extractor) { data.modifier(this) }

    private fun update(data: T) = launch(dispatcher) {
        file.create()
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, data)
    }

    fun onChange(f: () -> Unit) = dataProperty.onChange { f() }
    fun perform(f: () -> Unit) = dataProperty.perform { f() }

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
            Thread(it, "settingsWriter").apply { isDaemon = true }
        }.asCoroutineDispatcher()

        private val log = logger()

        inline operator fun <reified T : Any> invoke(name: String, noinline default: () -> T): SettingsRepo<T> = SettingsRepo(name, T::class, default)
    }
}