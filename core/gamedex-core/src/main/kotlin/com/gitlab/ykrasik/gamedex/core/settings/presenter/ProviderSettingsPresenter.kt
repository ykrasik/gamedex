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

package com.gitlab.ykrasik.gamedex.core.settings.presenter

import com.gitlab.ykrasik.gamedex.app.api.settings.ProviderAccountStatus
import com.gitlab.ykrasik.gamedex.app.api.settings.ProviderSettingsView
import com.gitlab.ykrasik.gamedex.core.CommonData
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.core.settings.ProviderSettingsRepository
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.gitlab.ykrasik.gamedex.util.Try
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
        val settings = settingsRepo.providers.getValue(view.provider.id)
        val lastInvalidAccount = MutableStateFlow(emptyMap<String, String>())

        init {
            isShowing.forEach(debugName = "onShow") {
                if (it) {
                    view.enabled *= settings.enabled.value
                    view.currentAccount *= settings.account.value
                }
            }

            view.canChangeProviderSettings *= commonData.disableWhenGameSyncIsRunning withDebugName "canChangeProviderSettings"

            view.status *= combine(view.currentAccount.allValues(), lastInvalidAccount, settings.account) { currentAccount, lastInvalidAccount, lastValidAccount ->
                when {
                    view.provider.accountFeature == null -> ProviderAccountStatus.NotRequired
                    accountHasEmptyFields(currentAccount) -> ProviderAccountStatus.Empty
                    currentAccount == lastValidAccount -> ProviderAccountStatus.Valid
                    currentAccount == lastInvalidAccount -> ProviderAccountStatus.Invalid
                    else -> ProviderAccountStatus.Unverified
                }
            } withDebugName "status"
            view.canVerifyAccount *= view.status.map { status ->
                IsValid {
                    check(status != ProviderAccountStatus.NotRequired) { "Provider does not require an account!" }
                    check(status != ProviderAccountStatus.Empty) { "Empty account!" }
                    check(status != ProviderAccountStatus.Valid) { "Account already verified!" }
                }
            } withDebugName "canVerifyAccount"

            view.enabled.onlyChangesFromView().forEach(debugName = "onEnabledChanged") { enabled ->
                verifyCanChange()
                if (enabled) {
                    when (view.status.value) {
                        ProviderAccountStatus.Unverified -> verifyAccount()
                        ProviderAccountStatus.Valid, ProviderAccountStatus.NotRequired -> settings.enabled *= true
                        else -> view.enabled *= false
                    }
                } else {
                    settings.enabled *= false
                }
            }
            view.verifyAccountActions.forEach(debugName = "onVerifyAccount") { verifyAccount() }
        }

        private fun accountHasEmptyFields(account: Map<String, String>) =
            account.size != view.provider.accountFeature!!.fields || account.any { (_, value) -> value.isBlank() }

        private suspend fun verifyAccount() {
            verifyCanChange()
            view.canVerifyAccount.assert()

            val account = view.currentAccount.v
            val valid = Try {
                taskService.execute(gameProviderService.verifyAccount(view.provider.id, account))
            }
            if (valid.isSuccess) {
                settings.account *= account
                settings.enabled *= true
            } else {
                lastInvalidAccount *= account
            }
        }

        private fun verifyCanChange() = view.canChangeProviderSettings.assert()
    }
}