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

package com.gitlab.ykrasik.gamedex.javafx.settings

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.core.provider.ProviderUserConfig
import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfigRepository
import com.gitlab.ykrasik.gamedex.core.userconfig.SettingsService
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.provider.ProviderUserAccount
import com.gitlab.ykrasik.gamedex.util.info
import com.gitlab.ykrasik.gamedex.util.logger
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.withContext
import tornadofx.Controller
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 09/05/2017
 * Time: 17:09
 */
// TODO: Move to tornadoFx di() and have the presenter as a dependency.
@Singleton
class SettingsController @Inject constructor(private val userConfigRepository: UserConfigRepository,
                                             private val settingsService: SettingsService) : Controller() {
    private val logger = logger()

    private val settingsView: SettingsView by inject()

    private val providerUserConfig = userConfigRepository[ProviderUserConfig::class]

    fun providerSettings(providerId: ProviderId) = providerUserConfig[providerId]
    fun setProviderEnabled(providerId: ProviderId, enable: Boolean) {
        providerUserConfig.modify(providerId) { copy(enable = enable) }
    }

    suspend fun showSettingsMenu() {
        userConfigRepository.saveSnapshot()
        settingsService.saveSnapshot()
        try {
            val accept = settingsView.show()
            if (accept) {
                userConfigRepository.commitSnapshot()
                settingsService.commitSnapshot()
            } else {
                userConfigRepository.revertSnapshot()
                settingsService.revertSnapshot()
            }
        } catch (e: Exception) {
            logger.error("Error updating settings!", e)
            userConfigRepository.revertSnapshot()
            settingsService.revertSnapshot()
        }
    }

    suspend fun validateAndUseAccount(provider: GameProvider, account: Map<String, String>): Boolean = withContext(CommonPool) {
        val newAccount = provider.accountFeature!!.createAccount(account)
        val valid = validate(provider, newAccount)
        if (valid) {
            withContext(JavaFx) {
                providerUserConfig.modify(provider.id) { copy(account = account) }
            }
        }
        valid
    }

    private fun validate(provider: GameProvider, account: ProviderUserAccount): Boolean = try {
        logger.info { "[${provider.id}] Validating: $account" }
        provider.search("TestSearchToVerifyAccount", Platform.pc, account)
        logger.info { "[${provider.id}] Valid!" }
        true
    } catch (e: Exception) {
        logger.warn("[${provider.id}] Invalid!", e)
        false
    }
}