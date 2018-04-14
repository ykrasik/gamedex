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

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 11/03/2018
 * Time: 15:12
 */
// TODO: Maybe this is the SettingsRepository? Hide all access to single settings through this class.
@Singleton
class AllSettings @Inject constructor(private val settings: MutableSet<UserSettings>) {
    @Suppress("UNCHECKED_CAST")
    operator fun <T : UserSettings> get(klass: KClass<T>): T = settings.find { it::class == klass }!! as T

    fun saveSnapshot() = settings.forEach {
        it.disableWrite()
        it.saveSnapshot()
    }

    fun restoreSnapshot() = settings.forEach {
        it.restoreSnapshot()
        it.enableWrite()
        it.clearSnapshot()
    }

    fun commitSnapshot() = settings.forEach {
        it.enableWrite()
        it.flush()
        it.clearSnapshot()
    }
}