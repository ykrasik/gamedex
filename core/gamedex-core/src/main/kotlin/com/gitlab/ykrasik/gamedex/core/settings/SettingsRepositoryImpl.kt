/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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

import com.gitlab.ykrasik.gamedex.core.storage.JsonStorageFactory
import com.gitlab.ykrasik.gamedex.core.storage.StorageMutableStateFlow
import com.gitlab.ykrasik.gamedex.util.logResult
import com.gitlab.ykrasik.gamedex.util.logger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 09/06/2018
 * Time: 22:04
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(private val factory: JsonStorageFactory<String>) : SettingsRepository {
    private val log = logger()
    private val storages = mutableListOf<SettingsStorage<*>>()

    override fun <T : Any> storage(
        basePath: String,
        name: String,
        klass: KClass<T>,
        resettable: Boolean,
        default: () -> T,
    ) = addStorage {
        SettingsStorage(
            storage = StorageMutableStateFlow(
                storage = factory(basePath = "data/$basePath", klass = klass),
                key = name,
                default = default
            ),
            resettable = resettable
        )
    }

    private inline fun <T> addStorage(f: () -> SettingsStorage<T>) = f().apply { storages += this }.storage

    override fun saveSnapshot() = safeTry {
        onAll {
            disableWrite()
            saveSnapshot()
        }
    }

    override fun commitSnapshot() = safeTry(revertFallback = true) {
        log.logResult("Writing settings...") {
            onAll {
                enableWrite()
                flush()
                clearSnapshot()
            }
        }
    }

    override fun revertSnapshot() = safeTry {
        log.logResult("Reverting changes to settings...") {
            onAll {
                restoreSnapshot()
                enableWrite()
                clearSnapshot()
            }
        }
    }

    override fun resetDefaults() = safeTry {
        log.logResult("Resetting settings to default...") {
            onAll {
                resetDefault()
            }
        }
    }

    private inline fun onAll(f: SettingsStorage<*>.() -> Unit) = storages.forEach(f)

    private inline fun safeTry(revertFallback: Boolean = false, f: () -> Unit) {
        try {
            f()
        } catch (e: Exception) {
            log.error("Error updating settings!", e)
            if (revertFallback) {
                revertSnapshot()
            }
        }
    }

    private class SettingsStorage<T>(
        val storage: StorageMutableStateFlow<T>,
        val resettable: Boolean,
    ) {
        private var snapshot: T? = null

        fun enableWrite() = storage.enableWrite()
        fun disableWrite() = storage.disableWrite()

        fun saveSnapshot() {
            snapshot = storage.value
        }

        fun restoreSnapshot() {
            storage.value = snapshot!!
        }

        fun clearSnapshot() {
            snapshot = null
        }

        fun flush() {
            if (snapshot != storage.value) {
                storage.flushValueToStorage()
            }
        }

        fun resetDefault() {
            if (resettable) {
                storage.resetDefault()
            }
        }
    }
}