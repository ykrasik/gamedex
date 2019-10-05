/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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

import com.gitlab.ykrasik.gamedex.core.storage.StorageObservable
import kotlin.reflect.KClass

interface SettingsRepository {
    fun <T : Any> storage(
        basePath: String,
        name: String,
        klass: KClass<T>,
        resettable: Boolean,
        default: () -> T
    ): StorageObservable<T>

    fun saveSnapshot()
    fun commitSnapshot()
    fun revertSnapshot()
    fun resetDefaults()
}

inline fun <reified T : Any> SettingsRepository.storage(
    basePath: String,
    name: String,
    resettable: Boolean = true,
    noinline default: () -> T
): StorageObservable<T> = storage(basePath, name, T::class, resettable, default)