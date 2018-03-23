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

package com.gitlab.ykrasik.gamedex.core.provider

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.ProviderData
import com.gitlab.ykrasik.gamdex.core.api.util.ListObservable
import com.gitlab.ykrasik.gamdex.core.api.util.SubjectListObservable
import com.gitlab.ykrasik.gamdex.core.api.util.combineLatest
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.provider.ProviderUserAccount
import com.gitlab.ykrasik.gamedex.util.info
import com.gitlab.ykrasik.gamedex.util.logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 07/04/2017
 * Time: 21:30
 */
@Singleton
class GameProviderRepository @Inject constructor(providers: MutableSet<GameProvider>, settings: ProviderSettings) {
    private val log = logger()

    val allProviders: List<GameProvider> = providers.sortedBy { it.id }

    private val _enabledProviders = SubjectListObservable<EnabledGameProvider>()
    val enabledProviders: ListObservable<EnabledGameProvider> = _enabledProviders

    init {
        settings.providerSettingsSubject.combineLatest(settings.searchOrderSubject) { providerSettings, searchOrder ->
            providerSettings.mapNotNull { (providerId, settings) ->
                if (!settings.enable) return@mapNotNull null

                val provider = allProviders.find { it.id == providerId }!!
                val account = provider.accountFeature?.createAccount(settings.account!!)
                EnabledGameProvider(provider, account)
            }.sortedWith(searchOrder.toComparator())
        }.subscribe { enabledProviders ->
            _enabledProviders.set(enabledProviders)
        }

        log.info { "Detected providers: $allProviders" }
        log.info { "Enabled providers: ${_enabledProviders.sortedBy { it.id }}" }
    }

    fun enabledProvider(id: ProviderId) = enabledProviders.find { it.id == id }!!
    fun provider(id: ProviderId) = allProviders.find { it.id == id }!!
    fun isEnabled(id: ProviderId) = enabledProviders.any { it.id == id }
}

class EnabledGameProvider(private val provider: GameProvider, private val account: ProviderUserAccount?) : GameProvider by provider {
    fun search(name: String, platform: Platform): List<ProviderSearchResult> = provider.search(name, platform, account)
    fun download(apiUrl: String, platform: Platform): ProviderData = provider.download(apiUrl, platform, account)

    override fun toString() = provider.toString()
}