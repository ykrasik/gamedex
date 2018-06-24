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
import com.gitlab.ykrasik.gamedex.provider.ProviderId

/**
 * User: ykrasik
 * Date: 17/06/2018
 * Time: 14:26
 */
// TODO: Split this into a repo-per-provider
class ProviderSettingsRepository(factory: SettingsStorageFactory, private val gameProviderRepository: GameProviderRepository) :
    SettingsRepository<ProviderSettingsRepository.Data>() {
    data class Data(
        val providers: Map<ProviderId, ProviderSettings>
    ) {
        inline fun modifyProvider(providerId: ProviderId, f: ProviderSettings.() -> ProviderSettings) =
            copy(providers = providers + (providerId to f(providers[providerId]!!)))
    }

    data class ProviderSettings(
        val enabled: Boolean,
        val account: Map<String, String>
    )

    override val storage = factory("provider", Data::class) {
        Data(
            providers = gameProviderRepository.allProviders.map { provider ->
                provider.id to ProviderSettings(
                    enabled = provider.accountFeature == null,
                    account = emptyMap()
                )
            }.toMap()
        )
    }

    val providersChannel = storage.channel(Data::providers)
    val providers by providersChannel
}