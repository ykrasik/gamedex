/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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

import com.gitlab.ykrasik.gamedex.core.log.LogService
import com.gitlab.ykrasik.gamedex.core.storage.StorageMutableStateFlow
import com.gitlab.ykrasik.gamedex.core.util.flowScope
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 17/06/2018
 * Time: 14:26
 */
@Singleton
class ProviderSettingsRepository @Inject constructor(
    private val repo: SettingsRepository,
    private val logService: LogService,
) {
    data class Data(
        val enabled: Boolean,
        val account: Map<String, String>,
    )

    class Repo(storage: StorageMutableStateFlow<Data>) {
        val enabled = storage.biMap(Data::enabled) { copy(enabled = it) }
        val account = storage.biMap(Data::account) { copy(account = it) }

        val data: StateFlow<Data> = storage
    }

    private val _providers = mutableMapOf<ProviderId, Repo>()
    val providers: Map<ProviderId, Repo> = _providers

    fun register(provider: GameProvider.Metadata): Repo {
        val repo = Repo(repo.storage(basePath = "provider/${provider.id.lowercase()}", name = "account", resettable = false) {
            Data(
                enabled = provider.accountFeature == null,
                account = emptyMap()
            )
        })

        flowScope(Dispatchers.Default) {
            repo.account.forEach(debugName = "${provider.id}.onAccountChanged") { account ->
                account.values.forEach {
                    logService.addBlacklistValue(it)
                }
            }
        }
        _providers[provider.id] = repo
        return repo
    }
}
