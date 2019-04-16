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

import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.provider.id
import com.gitlab.ykrasik.gamedex.util.months

/**
 * User: ykrasik
 * Date: 31/12/2018
 * Time: 15:12
 */
class ProviderGeneralSettingsRepository(factory: SettingsStorageFactory, providers: List<GameProvider>) : SettingsRepository<ProviderGeneralSettingsRepository.Data>() {
    data class Data(
        val refetchGamesFilter: Filter,
        val resyncGamesFilter: Filter
    )

    override val storage = factory("general", Data::class) {
        Data(
            refetchGamesFilter = Filter.PeriodUpdateDate(2.months).not,
            resyncGamesFilter = if (providers.isNotEmpty()) {
                val providerIds = providers.map { it.id }
                providerIds.drop(1).fold(providerIds.first().missingProviderFilter) { filter, providerId ->
                    filter or providerId.missingProviderFilter
                }
            } else {
                Filter.PeriodCreateDate(2.months).not
            }
        )
    }

    private val ProviderId.missingProviderFilter: Filter get() = Filter.Provider(this).not

    val refetchGamesFilterChannel = storage.channel(Data::refetchGamesFilter)
    val refetchGamesFilter by refetchGamesFilterChannel

    val resyncGamesFilterChannel = storage.channel(Data::resyncGamesFilter)
    val resyncGamesFilter by resyncGamesFilterChannel
}