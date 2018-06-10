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
    val game: GameSettingsRepository
    val cellDisplay: GameCellDisplaySettingsRepository
    val nameDisplay: GameNameDisplaySettingsRepository
    val metaTagDisplay: GameMetaTagDisplaySettingsRepository
    val versionDisplay: GameVersionDisplaySettingsRepository

    fun saveSnapshot()
    fun revertSnapshot()
    fun commitSnapshot()
    fun restoreDefaults()
}

@Singleton
class SettingsServiceImpl : SettingsService {
    private val all = mutableListOf<SettingsRepository<*>>()

    override val general = repo { GeneralSettingsRepository() }
    override val game = repo { GameSettingsRepository() }
    override val cellDisplay = repo { GameCellDisplaySettingsRepository() }
    override val nameDisplay = repo { GameNameDisplaySettingsRepository() }
    override val metaTagDisplay = repo { GameMetaTagDisplaySettingsRepository() }
    override val versionDisplay = repo { GameVersionDisplaySettingsRepository() }

    private inline fun <R : SettingsRepository<*>> repo(f: () -> R): R = f().apply { all += this }

    override fun saveSnapshot() = withRepos {
        disableWrite()
        saveSnapshot()
    }

    override fun revertSnapshot() = withRepos {
        restoreSnapshot()
        enableWrite()
        clearSnapshot()
    }

    override fun commitSnapshot() = withRepos {
        enableWrite()
        flush()
        clearSnapshot()
    }

    override fun restoreDefaults() = withRepos {
        restoreDefaults()
    }

    private inline fun withRepos(f: SettingsRepository<*>.() -> Unit) = all.forEach(f)
}