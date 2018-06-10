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

import com.google.inject.ImplementedBy
import com.google.inject.Singleton

/**
 * User: ykrasik
 * Date: 09/06/2018
 * Time: 22:04
 */
@ImplementedBy(SettingsServiceImpl::class)
interface SettingsService {
    val general: GeneralSettingsRepository
    val gameDisplay: GameDisplaySettingsRepository

    fun saveSnapshot()
    fun revertSnapshot()
    fun commitSnapshot()
    fun restoreDefaults()
}

@Singleton
class SettingsServiceImpl : SettingsService {
    override val general = GeneralSettingsRepository()
    override val gameDisplay = GameDisplaySettingsRepository()

    private val all = listOf(gameDisplay, general)

    override fun saveSnapshot() = withSettings {
        disableWrite()
        saveSnapshot()
    }

    override fun revertSnapshot() = withSettings {
        restoreSnapshot()
        enableWrite()
        clearSnapshot()
    }

    override fun commitSnapshot() = withSettings {
        enableWrite()
        flush()
        clearSnapshot()
    }

    override fun restoreDefaults() = withSettings {
        restoreDefaults()
    }

    private inline fun withSettings(f: SettingsRepository<*>.() -> Unit) = all.forEach(f)
}