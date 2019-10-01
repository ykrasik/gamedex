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

import com.gitlab.ykrasik.gamedex.provider.GameProvider

/**
 * User: ykrasik
 * Date: 17/06/2018
 * Time: 14:26
 */
class ProviderSettingsRepository(factory: SettingsStorageFactory, provider: GameProvider.Metadata) :
    SettingsRepository<ProviderSettingsRepository.Data>() {

    data class Data(
        val enabled: Boolean,
        val account: Map<String, String>
    )

    override val storage = factory(provider.id.toLowerCase(), Data::class) {
        Data(
            enabled = provider.accountFeature == null,
            account = emptyMap()
        )
    }

    val enabledChannel = storage.channel(Data::enabled)
    val enabled by enabledChannel

    val accountChannel = storage.channel(Data::account)
    val account by accountChannel
}