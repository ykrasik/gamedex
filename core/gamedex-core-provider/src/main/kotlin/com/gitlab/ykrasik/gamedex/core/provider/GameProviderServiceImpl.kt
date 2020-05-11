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

package com.gitlab.ykrasik.gamedex.core.provider

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.ProviderData
import com.gitlab.ykrasik.gamedex.ProviderHeader
import com.gitlab.ykrasik.gamedex.Timestamp
import com.gitlab.ykrasik.gamedex.core.image.ImageService
import com.gitlab.ykrasik.gamedex.core.plugin.PluginManager
import com.gitlab.ykrasik.gamedex.core.settings.ProviderSettingsRepository
import com.gitlab.ykrasik.gamedex.core.task.Task
import com.gitlab.ykrasik.gamedex.core.task.task
import com.gitlab.ykrasik.gamedex.core.util.ListObservableImpl
import com.gitlab.ykrasik.gamedex.core.util.flowScope
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.provider.id
import com.gitlab.ykrasik.gamedex.util.logger
import kotlinx.coroutines.Dispatchers
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
    pluginManager: PluginManager,
    settingsRepo: ProviderSettingsRepository,
    private val imageService: ImageService
) : GameProviderService {
    private val log = logger()

    private val providersById: MutableMap<ProviderId, InternalGameProvider> = pluginManager.getImplementations(GameProvider::class)
        .associateByTo(mutableMapOf(), GameProvider::id) { InternalGameProvider(it, GameProvider.Account.Null) }

    override val allProviders = providersById.values.map { it.metadata }.sortedBy { it.id }
    override val enabledProviders = ListObservableImpl<GameProvider.Metadata>()

    init {
        log.info("Detected providers: [${allProviders.joinToString()}]")

        flowScope(Dispatchers.Default) {
            allProviders.forEach { provider ->
                val repo = settingsRepo.register(provider)
                repo.data.forEach(debugName = "${provider.id}.onDataChanged") { data ->
                    val enabledProvider = enabledProviders.find { it.id == provider.id }
                    when {
                        !data.enabled && enabledProvider != null -> {
                            enabledProviders -= enabledProvider
                            log.debug("Provider disabled: ${provider.id}")
                        }

                        data.enabled -> {
                            val account = provider.accountFeature?.createAccount(data.account) ?: GameProvider.Account.Null
                            providersById.compute(provider.id) { _, provider -> provider!!.copy(account = account) }

                            if (enabledProvider == null) {
                                log.debug("Provider enabled: ${provider.id}")
                                enabledProviders += provider
                            }
                        }
                    }
                }
            }

            enabledProviders.items.forEach(debugName = "onEnabledProvidersChanged") {
                log.info("Enabled providers: [${it.sortedBy { it.id }.joinToString()}]")
            }
        }
    }

    override val logos = allProviders.map { it.id to imageService.createImage(it.logo) }.toMap()

    override fun verifyAccount(providerId: ProviderId, account: Map<String, String>): Task<Unit> {
        val provider = providersById.getValue(providerId)
        val accountFeature = checkNotNull(provider.metadata.accountFeature) { "Provider $providerId does not require an account!" }
        return task("Verifying $providerId account...", initialImage = logos.getValue(providerId)) {
            try {
                val providerAccount = accountFeature.createAccount(account)
                provider.search("TestSearchToVerifyAccount", Platform.Windows, providerAccount, offset = 0, limit = 1)
                successMessage = { "Account is Valid." }
            } catch (e: Exception) {
                errorMessage = { "Account is Invalid!" }
                throw e
            }
        }
    }

    override fun search(providerId: ProviderId, query: String, platform: Platform, offset: Int, limit: Int) =
        task("Searching $providerId for '$query'...", initialImage = logos.getValue(providerId)) {
            successMessage = null
            val provider = providerId.enabledProvider
            provider.search(query, platform, provider.account, offset = offset, limit = limit)
        }

    override fun fetch(name: String, platform: Platform, headers: List<ProviderHeader>) = task("Fetching '$name'...") {
        if (headers.isEmpty()) return@task emptyList<ProviderData>()
        successMessage = null
        totalItems.value = headers.size
        headers.map { header ->
            // TODO: Link to task scope.
            GlobalScope.async {
                val provider = header.providerId.enabledProvider
                val response = provider.fetch(header.providerGameId, platform, provider.account)
                incProgress()
                ProviderData(
                    header = header,
                    siteUrl = response.siteUrl,
                    gameData = response.gameData,
                    timestamp = Timestamp.now
                )
            }
        }.map { it.await() }
    }

    private val ProviderId.enabledProvider: InternalGameProvider
        get() = providersById.getValue(this).also { provider ->
            check(enabledProviders.any { it.id == provider.id }) { "Provider is not enabled: '${provider.id}'" }
        }

    data class InternalGameProvider(
        private val provider: GameProvider,
        val account: GameProvider.Account
    ) : GameProvider by provider {

        override fun toString() = provider.toString()
    }
}