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

package com.gitlab.ykrasik.gamedex.core.provider

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.ProviderData
import com.gitlab.ykrasik.gamedex.ProviderHeader
import com.gitlab.ykrasik.gamedex.Timestamp
import com.gitlab.ykrasik.gamedex.core.image.ImageService
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import com.gitlab.ykrasik.gamedex.core.task.Task
import com.gitlab.ykrasik.gamedex.core.task.task
import com.gitlab.ykrasik.gamedex.core.util.ListObservableImpl
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.logger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 13/10/2016
 * Time: 13:29
 */
@Singleton
class GameProviderServiceImpl @Inject constructor(
    repo: GameProviderRepository,
    private val imageService: ImageService,
    private val settingsService: SettingsService
) : GameProviderService {
    private val log = logger()

    override val allProviders = repo.allProviders
    override val enabledProviders = ListObservableImpl<EnabledGameProvider>()

    init {
        log.info("Detected providers: ${allProviders.joinToString()}")

        allProviders.forEach { provider ->
            val providerSettingsRepo = settingsService.providers[provider.id]!!
            providerSettingsRepo.perform { data ->
                val enabledProvider = enabledProviders.find { it.id == provider.id }
                when {
                    !data.enabled && enabledProvider != null -> {
                        enabledProviders -= enabledProvider
                        log.trace("Provider disabled: ${provider.id}")
                    }

                    data.enabled -> {
                        val account = provider.accountFeature?.createAccount(data.account)
                        val newProvider = EnabledGameProvider(provider, account)
                        if (enabledProvider != null) {
                            log.trace("Provider re-enabled: $enabledProvider")
                            enabledProviders.replace(enabledProvider, newProvider)
                        } else {
                            log.trace("Provider enabled: ${provider.id}")
                            enabledProviders += newProvider
                        }
                    }
                }
            }
        }

        enabledProviders.itemsChannel.subscribe { enabledProviders ->
            log.info("Enabled providers: ${enabledProviders.sortedBy { it.id }.joinToString()}")
        }
    }

    override fun isEnabled(id: ProviderId) = enabledProviders.any { it.id == id }

    override val logos = allProviders.map { it.id to imageService.createImage(it.logo) }.toMap()

    override fun checkAtLeastOneProviderEnabled() =
        check(enabledProviders.isNotEmpty()) {
            "No providers are enabled! Please make sure there's at least 1 enabled provider in the settings menu."
        }

    override fun platformsWithEnabledProviders(): Set<Platform> =
        enabledProviders.fold(setOf()) { acc, provider -> acc + provider.supportedPlatforms }

    override fun verifyAccount(providerId: ProviderId, account: Map<String, String>): Task<Unit> {
        val provider = allProviders.find { it.id == providerId }!!
        val accountFeature = checkNotNull(provider.accountFeature) { "Provider $providerId does not require an account!" }
        return task("Verifying $providerId account...", initialImage = logos[providerId]!!) {
            try {
                val providerAccount = accountFeature.createAccount(account)
                provider.search("TestSearchToVerifyAccount", Platform.Windows, providerAccount)
                successMessage = { "$providerId: Valid Account." }
            } catch (e: Exception) {
                errorMessage = { "$providerId: Invalid Account!" }
                throw e
            }
        }
    }

    override fun search(providerId: ProviderId, query: String, platform: Platform) =
        task("Searching $providerId for '$query'...", initialImage = logos[providerId]!!) {
            enabledProviders.find { it.id == providerId }!!.search(query, platform)
        }

    override fun download(name: String, platform: Platform, headers: List<ProviderHeader>) = task("Downloading '$name'...") {
        totalItems = headers.size
        headers.map { header ->
            // TODO: Link to task scope.
            GlobalScope.async {
                val data = enabledProviders.find { it.id == header.id }!!.download(header.apiUrl, platform)
                incProgress()
                ProviderData(
                    header = header,
                    siteUrl = data.siteUrl,
                    gameData = data.gameData,
                    timestamp = Timestamp.now
                )
            }
        }.map { it.await() }
    }
}