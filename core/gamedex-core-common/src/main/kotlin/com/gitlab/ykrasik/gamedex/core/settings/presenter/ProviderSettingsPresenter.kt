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

package com.gitlab.ykrasik.gamedex.core.settings.presenter

import com.gitlab.ykrasik.gamedex.app.api.settings.ProviderAccountStatus
import com.gitlab.ykrasik.gamedex.app.api.settings.ProviderSettingsView
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.core.settings.ProviderSettingsRepository
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.util.Try
import com.gitlab.ykrasik.gamedex.util.browseToUrl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 20/06/2018
 * Time: 19:47
 */
@Singleton
class ProviderSettingsPresenter @Inject constructor(
    private val settingsService: SettingsService,
    private val gameProviderService: GameProviderService,
    private val taskService: TaskService
) : Presenter<ProviderSettingsView> {
    override fun present(view: ProviderSettingsView) = object : ViewSession() {
        var lastVerifiedAccount = emptyMap<String, String>()
        var enabled by view.enabled
        var status by view.status
        var currentAccount by view.currentAccount
        var canVerifyAccount by view.canVerifyAccount

        init {
            // FIXME: This doesn't update when settings are updated outside of this scope, like the settings screen being closed with a cancel.
            enabled = settingsService.providers[view.provider.id]!!.enabled
            view.enabled.forEach { onEnabledChanged(it) }

            // This will not update if settings are reset to default - by design.
            val account = settingsService.providers[view.provider.id]!!.account
            currentAccount = account
            view.currentAccount.forEach { onCurrentAccountChanged(it) }

            lastVerifiedAccount = if (account.isNotEmpty()) account else emptyMap()

            status = when {
                view.provider.accountFeature == null -> ProviderAccountStatus.NotRequired
                accountHasEmptyFields -> ProviderAccountStatus.Empty
                account.isNotEmpty() -> ProviderAccountStatus.Valid
                else -> ProviderAccountStatus.Empty
            }
            setCanVerifyAccount()

            view.gotoAccountUrlActions.forEach { onGotoAccountUrl() }
            view.verifyAccountActions.forEach { verifyAccount() }
        }

        private val accountHasEmptyFields: Boolean
            get() = currentAccount.size != view.provider.accountFeature!!.fields ||
                currentAccount.any { (_, value) -> value.isBlank() }

        private suspend fun onEnabledChanged(enabled: Boolean) {
            if (enabled) {
                if (status == ProviderAccountStatus.Unverified) {
                    verifyAccount()
                }
                if (status == ProviderAccountStatus.Valid || status == ProviderAccountStatus.NotRequired) {
                    modifyProviderSettings { copy(enabled = true, account = currentAccount) }
                } else {
                    this.enabled = false
                }
            } else {
                modifyProviderSettings { copy(enabled = false) }
            }
        }

        private inline fun modifyProviderSettings(crossinline f: ProviderSettingsRepository.Data.() -> ProviderSettingsRepository.Data) {
            settingsService.providers[view.provider.id]!!.modify { f() }
        }

        private suspend fun verifyAccount() {
            check(canVerifyAccount.isSuccess) { "Verifying account not allowed!" }
            val valid = kotlin.runCatching {
                taskService.execute(gameProviderService.verifyAccount(view.provider.id, currentAccount))
            }
            if (valid.isSuccess) {
                status = ProviderAccountStatus.Valid
                lastVerifiedAccount = currentAccount
            } else {
                status = ProviderAccountStatus.Invalid
            }
        }

        private fun onCurrentAccountChanged(currentAccount: Map<String, String>) {
            status = when {
                view.provider.accountFeature == null -> ProviderAccountStatus.NotRequired
                accountHasEmptyFields -> ProviderAccountStatus.Empty
                currentAccount == lastVerifiedAccount -> ProviderAccountStatus.Valid
                else -> ProviderAccountStatus.Unverified
            }
            setCanVerifyAccount()
        }

        private fun setCanVerifyAccount() {
            canVerifyAccount = Try {
                check(status != ProviderAccountStatus.NotRequired) { "Provider does not require an account!" }
                check(status != ProviderAccountStatus.Empty) { "Empty account!" }
                check(status != ProviderAccountStatus.Valid) { "Account already verified!" }
            }
        }

        private fun onGotoAccountUrl() {
            view.provider.accountFeature!!.accountUrl.browseToUrl()
        }
    }
}