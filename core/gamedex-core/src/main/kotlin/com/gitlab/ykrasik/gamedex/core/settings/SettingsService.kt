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

import com.gitlab.ykrasik.gamedex.core.persistence.JsonStorageFactory
import com.gitlab.ykrasik.gamedex.core.provider.GameProviderRepository
import com.gitlab.ykrasik.gamedex.core.report.ReportSettingsRepository
import com.gitlab.ykrasik.gamedex.util.logger
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
    val providerOrder: ProviderOrderSettingsRepository
    val report: ReportSettingsRepository

    fun saveSnapshot()
    fun revertSnapshot()
    fun commitSnapshot()
    fun resetDefaults()
}

@Singleton
class SettingsServiceImpl @Inject constructor(
    factory: JsonStorageFactory<String>,
    gameProviderRepository: GameProviderRepository
) : SettingsService {
    private val log = logger()
    private val all = mutableListOf<SettingsRepository<*>>()
    private val settingsStorageFactory = SettingsStorageFactory("conf", factory)

    override val general = repo { GeneralSettingsRepository(settingsStorageFactory) }
    override val game = repo { GameSettingsRepository(settingsStorageFactory) }
    override val cellDisplay = repo { GameCellDisplaySettingsRepository(settingsStorageFactory) }
    override val nameDisplay = repo { GameNameDisplaySettingsRepository(settingsStorageFactory) }
    override val metaTagDisplay = repo { GameMetaTagDisplaySettingsRepository(settingsStorageFactory) }
    override val versionDisplay = repo { GameVersionDisplaySettingsRepository(settingsStorageFactory) }
    override val provider = repo { ProviderSettingsRepository(settingsStorageFactory, gameProviderRepository) }
    override val providerOrder = repo { ProviderOrderSettingsRepository(settingsStorageFactory, gameProviderRepository) }
    override val report = repo { ReportSettingsRepository(settingsStorageFactory) }

    private inline fun <R : SettingsRepository<*>> repo(f: () -> R): R = f().apply { all += this }

    override fun saveSnapshot() = safeTry {
        withRepos {
            disableWrite()
            saveSnapshot()
        }
    }

    override fun revertSnapshot() = safeTry {
        withRepos {
            restoreSnapshot()
            enableWrite()
            clearSnapshot()
        }
    }

    override fun commitSnapshot() = safeTry(revertFallback = true) {
        withRepos {
            enableWrite()
            flush()
            clearSnapshot()
        }
    }

    // FIXME: Do not reset provider accounts.
    override fun resetDefaults() = safeTry {
        withRepos {
            resetDefaults()
        }
    }

    private inline fun withRepos(f: SettingsRepository<*>.() -> Unit) = all.forEach(f)

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
}