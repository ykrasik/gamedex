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

import com.gitlab.ykrasik.gamedex.app.api.settings.Order
import com.gitlab.ykrasik.gamedex.core.provider.GameProviderRepository
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.provider.ProviderOrderPriorities
import com.gitlab.ykrasik.gamedex.util.Extractor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 17/06/2018
 * Time: 14:26
 */
@Singleton
class ProviderSettingsRepository @Inject constructor(private val gameProviderRepository: GameProviderRepository) :
    SettingsRepository<ProviderSettingsRepository.Data>("provider", ProviderSettingsRepository.Data::class) {
    data class Data(
        val providers: Map<ProviderId, ProviderSettings>,
        val order: ProviderOrderSettings
    ) {
        inline fun modifyProvider(providerId: ProviderId, f: ProviderSettings.() -> ProviderSettings) =
            copy(providers = providers + (providerId to f(providers[providerId]!!)))
    }

    data class ProviderSettings(
        val enabled: Boolean,
        val account: Map<String, String>
    )

    data class ProviderOrderSettings(
        val search: Order,
        val name: Order,
        val description: Order,
        val releaseDate: Order,
        val criticScore: Order,
        val userScore: Order,
        val thumbnail: Order,
        val poster: Order,
        val screenshot: Order
    )

    override fun defaultSettings() = Data(
        providers = gameProviderRepository.allProviders.map { provider ->
            provider.id to ProviderSettings(
                enabled = provider.accountFeature == null,
                account = emptyMap()
            )
        }.toMap(),
        order = ProviderOrderSettings(
            search = defaultOrder { search },
            name = defaultOrder { name },
            description = defaultOrder { description },
            releaseDate = defaultOrder { releaseDate },
            criticScore = defaultOrder { criticScore },
            userScore = defaultOrder { userScore },
            thumbnail = defaultOrder { thumbnail },
            poster = defaultOrder { poster },
            screenshot = defaultOrder { screenshot }
        )
    )

    private inline fun defaultOrder(crossinline extractor: Extractor<ProviderOrderPriorities, Int>) = Order(
        gameProviderRepository.allProviders.sortedBy { extractor(it.defaultOrder) }.mapIndexed { i, provider -> provider.id to i }.toMap()
    )

    val providersChannel = channel(Data::providers)
    val providers by providersChannel

    val orderChannel = channel(Data::order)
    val order by orderChannel
}