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

import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.app.api.settings.SettingsView
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import com.gitlab.ykrasik.gamedex.util.setAll
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 24/06/2018
 * Time: 09:39
 */
@Singleton
class SettingsPresenter @Inject constructor(
    private val settingsService: SettingsService,
    private val gameProviderService: GameProviderService,
    private val viewManager: ViewManager
) : Presenter<SettingsView> {
    override fun present(view: SettingsView) = object : ViewSession() {
        init {
            view.providers.setAll(gameProviderService.allProviders)

            view.acceptActions.forEach { onAccept() }
            view.cancelActions.forEach { onCancel() }
            view.resetDefaultsActions.forEach { onResetDefaults() }
        }

        override fun onShow() {
            settingsService.saveSnapshot()
        }

        private fun onAccept() {
            settingsService.commitSnapshot()
            close()
        }

        private fun onCancel() {
            settingsService.revertSnapshot()
            close()
        }

        private fun close() {
            viewManager.closeSettingsView(view)
        }

        private fun onResetDefaults() {
            if (view.confirmResetDefaults()) {
                settingsService.resetDefaults()
            }
        }
    }
}