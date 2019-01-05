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

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.app.api.util.BroadcastReceiveChannel
import com.gitlab.ykrasik.gamedex.core.provider.GameProviderRepository
import com.gitlab.ykrasik.gamedex.core.storage.JsonStorageFactory
import com.gitlab.ykrasik.gamedex.provider.ProviderId
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
    val nameDisplay: GameOverlayDisplaySettingsRepository
    val metaTagDisplay: GameOverlayDisplaySettingsRepository
    val versionDisplay: GameOverlayDisplaySettingsRepository

    val platforms: Map<Platform, GamePlatformSettingsRepository>
    val currentPlatformSettingsChannel: BroadcastReceiveChannel<GamePlatformSettingsRepository>
    val currentPlatformSettings: GamePlatformSettingsRepository

    val providerGeneral: ProviderGeneralSettingsRepository
    val providerOrder: ProviderOrderSettingsRepository
    val providers: Map<ProviderId, ProviderSettingsRepository>

    fun saveSnapshot()
    fun revertSnapshot()
    fun commitSnapshot()
    fun resetDefaults()
}

@Singleton
class SettingsServiceImpl @Inject constructor(
    private val factory: JsonStorageFactory<String>,
    gameProviderRepository: GameProviderRepository
) : SettingsService {
    private val log = logger()
    private val all = mutableListOf<SettingsRepository<*>>()

    private inline fun <R : SettingsRepository<*>> repo(f: () -> R): R = f().apply { all += this }

    override val general = repo { GeneralSettingsRepository(settingsStorage()) }
    override val game = repo { GameSettingsRepository(settingsStorage()) }
    override val cellDisplay = repo { GameCellDisplaySettingsRepository(settingsStorage("display")) }
    override val nameDisplay = repo { GameOverlayDisplaySettingsRepository.name(settingsStorage("display")) }
    override val metaTagDisplay = repo { GameOverlayDisplaySettingsRepository.metaTag(settingsStorage("display")) }
    override val versionDisplay = repo { GameOverlayDisplaySettingsRepository.version(settingsStorage("display")) }

    override val platforms = Platform.values().filter { it != Platform.excluded }.map { platform ->
        platform to repo { GamePlatformSettingsRepository(settingsStorage("platform"), platform) }
    }.toMap()

    override val currentPlatformSettingsChannel = game.platformChannel.map { platform -> platforms[platform]!! }
    override val currentPlatformSettings by currentPlatformSettingsChannel

    override val providerGeneral = repo { ProviderGeneralSettingsRepository(settingsStorage("provider"), gameProviderRepository) }
    override val providerOrder = repo { ProviderOrderSettingsRepository(settingsStorage("provider"), gameProviderRepository) }
    override val providers = gameProviderRepository.allProviders.map { provider ->
        provider.id to repo { ProviderSettingsRepository(settingsStorage("provider"), provider) }
    }.toMap()

    private fun settingsStorage(basePath: String = "") = SettingsStorageFactory("conf/$basePath", factory)

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

    override fun resetDefaults() = safeTry {
        withRepos {
            // Do not reset provider accounts.
            if (!providers.values.contains(this)) {
                resetDefaults()
            }
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