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

package com.gitlab.ykrasik.gamedex.repository

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.settings.ProviderSettings
import com.gitlab.ykrasik.gamedex.settings.ProviderUserSettings
import com.gitlab.ykrasik.gamedex.javafx.map
import com.gitlab.ykrasik.gamedex.javafx.mapToList
import com.gitlab.ykrasik.gamedex.javafx.toImage
import com.gitlab.ykrasik.gamedex.util.info
import com.gitlab.ykrasik.gamedex.util.logger
import javafx.collections.ObservableList
import javafx.scene.image.Image
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 07/04/2017
 * Time: 21:30
 */
@Singleton
class GameProviderRepository @Inject constructor(settings: ProviderSettings, providers: MutableSet<GameProvider>) {
    private val log = logger()

    val allProviders: List<GameProvider> = providers.sortedBy { it.id }

    // JavaFx has no read-only list interface. Do not modify!
    val enabledProviders: ObservableList<EnabledGameProvider> = settings.providerSettingsProperty.mapToList { providerSettings ->
        providerSettings.mapNotNull { (providerId, settings) ->
            if (!settings.enable) return@mapNotNull null

            val provider = allProviders.find { it.id == providerId }!!
            val account = provider.createAccount(settings)
            EnabledGameProvider(provider, account)
        }
    }.sorted().apply {
        comparatorProperty().bind(settings.searchOrderProperty.map { it!!.toComparator<EnabledGameProvider>() })
    }

    init {
        log.info { "Detected providers: ${providers.sortedBy { it.id }}" }
    }

    fun enabledProvider(id: ProviderId) = enabledProviders.find { it.id == id }!!
    fun provider(id: ProviderId) = allProviders.find { it.id == id }!!
    fun isEnabled(id: ProviderId) = enabledProviders.any { it.id == id }

    private fun GameProvider.createAccount(settings: ProviderUserSettings): ProviderUserAccount? {
        val accountFeature = accountFeature ?: return null
        return accountFeature.createAccount(settings.account!!)
    }
}

// FIXME: Move this out of this class.
private val providerImages = mutableMapOf<ProviderId, Image>()
val GameProvider.logoImage get() = providerImages.getOrPut(id) { logo.toImage() }

class EnabledGameProvider(private val provider: GameProvider, private val account: ProviderUserAccount?) : GameProvider by provider {
    fun search(name: String, platform: Platform): List<ProviderSearchResult> = provider.search(name, platform, account)
    fun download(apiUrl: String, platform: Platform): ProviderData = provider.download(apiUrl, platform, account)

    override fun toString() = provider.toString()
}