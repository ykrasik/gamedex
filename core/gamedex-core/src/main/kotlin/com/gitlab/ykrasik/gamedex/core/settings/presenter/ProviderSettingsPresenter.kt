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

package com.gitlab.ykrasik.gamedex.core.settings.presenter

import com.gitlab.ykrasik.gamedex.app.api.settings.ProviderAccountStatus
import com.gitlab.ykrasik.gamedex.app.api.settings.ProviderSettingsView
import com.gitlab.ykrasik.gamedex.core.CommonData
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.core.settings.ProviderSettingsRepository
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.util.Try
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 20/06/2018
 * Time: 19:47
 */
@Singleton
class ProviderSettingsPresenter @Inject constructor(
    private val settingsRepo: ProviderSettingsRepository,
    private val gameProviderService: GameProviderService,
    private val commonData: CommonData,
    private val taskService: TaskService
) : Presenter<ProviderSettingsView> {
    override fun present(view: ProviderSettingsView) = object : ViewSession() {
        val settings get() = settingsRepo.providers.getValue(view.provider.id)
        var lastVerifiedAccount = emptyMap<String, String>()
        var status by view.status
        var currentAccount by view.currentAccount

        init {
            commonData.isGameSyncRunning.disableWhenTrue(view.canChangeProviderSettings) { "Game sync in progress!" }
            view.enabled.forEach { onEnabledChanged(it) }
            view.currentAccount.forEach { onCurrentAccountChanged(it) }
            view.verifyAccountActions.forEach { verifyAccount() }
        }

        override suspend fun onShown() {
            view.enabled *= settings.enabled

            // This will not update if settings are reset to default - by design.
            val account = settings.account
            currentAccount = account
            lastVerifiedAccount = if (account.isNotEmpty()) account else emptyMap()
            status = when {
                view.provider.accountFeature == null -> ProviderAccountStatus.NotRequired
                accountHasEmptyFields -> ProviderAccountStatus.Empty
                account.isNotEmpty() -> ProviderAccountStatus.Valid
                else -> ProviderAccountStatus.Empty
            }
            setCanVerifyAccount()
        }

        private val accountHasEmptyFields: Boolean
            get() = currentAccount.size != view.provider.accountFeature!!.fields ||
                currentAccount.any { (_, value) -> value.isBlank() }

        private suspend fun onEnabledChanged(enabled: Boolean) {
            verifyCanChange()
            if (enabled) {
                if (status == ProviderAccountStatus.Unverified) {
                    verifyAccount()
                }
                if (status == ProviderAccountStatus.Valid || status == ProviderAccountStatus.NotRequired) {
                    settings.modify { copy(enabled = true, account = currentAccount) }
                } else {
                    view.enabled *= false
                }
            } else {
                settings.enabled = false
            }
        }

        private suspend fun verifyAccount() {
            verifyCanChange()
            view.canVerifyAccount.assert()
            val valid = runCatching {
                taskService.execute(gameProviderService.verifyAccount(view.provider.id, currentAccount))
            }
            if (valid.isSuccess) {
                status = ProviderAccountStatus.Valid
                lastVerifiedAccount = currentAccount
                view.enabled *= true
                onEnabledChanged(enabled = true)
            } else {
                status = ProviderAccountStatus.Invalid
            }
            setCanVerifyAccount()
        }

        private fun onCurrentAccountChanged(currentAccount: Map<String, String>) {
            verifyCanChange()
            status = when {
                view.provider.accountFeature == null -> ProviderAccountStatus.NotRequired
                accountHasEmptyFields -> ProviderAccountStatus.Empty
                currentAccount == lastVerifiedAccount -> ProviderAccountStatus.Valid
                else -> ProviderAccountStatus.Unverified
            }
            setCanVerifyAccount()
        }

        private fun setCanVerifyAccount() {
            view.canVerifyAccount *= Try {
                check(status != ProviderAccountStatus.NotRequired) { "Provider does not require an account!" }
                check(status != ProviderAccountStatus.Empty) { "Empty account!" }
                check(status != ProviderAccountStatus.Valid) { "Account already verified!" }
            }
        }

        private fun verifyCanChange() = view.canChangeProviderSettings.assert()
    }
}