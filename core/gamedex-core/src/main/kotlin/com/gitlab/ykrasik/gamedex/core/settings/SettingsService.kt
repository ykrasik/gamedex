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

import com.gitlab.ykrasik.gamedex.core.provider.GameProviderRepository
import com.google.inject.ImplementedBy
import javax.inject.Inject
import javax.inject.Singleton

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
    val provider: ProviderSettingsRepository

    fun saveSnapshot()
    fun revertSnapshot()
    fun commitSnapshot()
    fun restoreDefaults()
}

@Singleton
class SettingsServiceImpl @Inject constructor(
    factory: SettingsStorageFactory,
    gameProviderRepository: GameProviderRepository
): SettingsService {
    private val all = mutableListOf<SettingsRepository<*>>()

    override val general = repo { GeneralSettingsRepository(factory) }
    override val game = repo { GameSettingsRepository(factory) }
    override val cellDisplay = repo { GameCellDisplaySettingsRepository(factory) }
    override val nameDisplay = repo { GameNameDisplaySettingsRepository(factory) }
    override val metaTagDisplay = repo { GameMetaTagDisplaySettingsRepository(factory) }
    override val versionDisplay = repo { GameVersionDisplaySettingsRepository(factory) }
    override val provider = repo { ProviderSettingsRepository(factory, gameProviderRepository) }

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