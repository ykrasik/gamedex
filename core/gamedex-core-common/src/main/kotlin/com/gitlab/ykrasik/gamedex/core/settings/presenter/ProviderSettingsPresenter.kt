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
import com.gitlab.ykrasik.gamedex.app.api.util.ValueOrError
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.core.settings.ProviderSettingsRepository
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.util.browseToUrl
import com.gitlab.ykrasik.gamedex.util.logger
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
    private val log = logger()

    override fun present(view: ProviderSettingsView) = object : ViewSession() {
        init {
            // This will not update if settings are reset to default - by design.
            view.enabled = settingsService.providers[view.provider.id]!!.enabled
            view.currentAccount = settingsService.providers[view.provider.id]!!.account
            view.lastVerifiedAccount = if (view.currentAccount.isNotEmpty()) view.currentAccount else emptyMap()
            onCurrentAccountChanged(view.currentAccount)

            // FIXME: This doesn't update when settings are updated outside of this scope, like the settings screen being closed with a cancel.
            view.enabledChanges.forEach { onEnabledChanged(it) }
            view.accountUrlClicks.forEach { onAccountUrlClicked() }
            view.currentAccountChanges.forEach { onCurrentAccountChanged(it) }
            view.verifyAccountRequests.forEach { verifyAccount() }
        }

        private fun accountHasEmptyFields(): Boolean =
            view.currentAccount.size != view.provider.accountFeature!!.fields ||
                view.currentAccount.any { (_, value) -> value.isBlank() }

        private suspend fun onEnabledChanged(enabled: Boolean) {
            if (enabled) {
                if (view.status == ProviderAccountStatus.Unverified) {
                    verifyAccount()
                }
                if (view.status == ProviderAccountStatus.Valid || view.status == ProviderAccountStatus.NotRequired) {
                    modifyProviderSettings { copy(enabled = true, account = view.currentAccount) }
                } else {
                    view.enabled = false
                }
            } else {
                modifyProviderSettings { copy(enabled = false) }
            }
        }

        private inline fun modifyProviderSettings(crossinline f: ProviderSettingsRepository.Data.() -> ProviderSettingsRepository.Data) {
            settingsService.providers[view.provider.id]!!.modify { f() }
        }

        private suspend fun verifyAccount() {
            check(view.canVerifyAccount.isSuccess) { "Verifying account not allowed!" }
            val valid = taskService.execute(gameProviderService.verifyAccount(view.provider.id, view.currentAccount))
            if (valid) {
                log.info("[${view.provider.id}] Valid!")
                view.status = ProviderAccountStatus.Valid
                view.lastVerifiedAccount = view.currentAccount
            } else {
                log.info("[${view.provider.id}] Invalid!")
                view.status = ProviderAccountStatus.Invalid
            }
        }

        private fun onCurrentAccountChanged(currentAccount: Map<String, String>) {
            view.status = when {
                view.provider.accountFeature == null -> ProviderAccountStatus.NotRequired
                accountHasEmptyFields() -> ProviderAccountStatus.Empty
                currentAccount == view.lastVerifiedAccount -> ProviderAccountStatus.Valid
                else -> ProviderAccountStatus.Unverified
            }
            setCanVerifyAccount()
        }

        private fun setCanVerifyAccount() {
            view.canVerifyAccount = ValueOrError {
                check(view.status != ProviderAccountStatus.NotRequired) { "Provider does not require an account!" }
                check(view.status != ProviderAccountStatus.Empty) { "Empty account!" }
                check(view.status != ProviderAccountStatus.Valid) { "Account already verified!" }
            }
        }

        private fun onAccountUrlClicked() {
            view.provider.accountFeature!!.accountUrl.browseToUrl()
        }
    }
}