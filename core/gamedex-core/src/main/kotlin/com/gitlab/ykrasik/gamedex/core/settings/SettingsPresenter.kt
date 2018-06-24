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

package com.gitlab.ykrasik.gamedex.core.settings

import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.app.api.settings.SettingsView
import com.gitlab.ykrasik.gamedex.core.Presentation
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.api.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.util.logger
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
    private val log = logger()

    override fun present(view: SettingsView) = object : Presentation() {
        init {
            view.providers.clear()
            view.providers += gameProviderService.allProviders

            view.acceptActions.actionOnUi { onAccept() }
            view.cancelActions.actionOnUi { onCancel() }
            view.resetDefaultsActions.actionOnUi { onResetDefaults() }
        }

        override fun onShow() {
            settingsService.saveSnapshot()
        }

        private fun onAccept() = writeSettings { settingsService.commitSnapshot() }

        private fun onCancel() = writeSettings { settingsService.revertSnapshot() }

        private fun writeSettings(f: () -> Unit) = try {
            f()
            viewManager.closeSettingsView(view)
        } catch (e: Exception) {
            log.error("Error updating settings!", e)
            settingsService.revertSnapshot()
        }

        private fun onResetDefaults() {
            if (view.confirmResetDefaults()) {
                settingsService.resetDefaults()
            }
        }
    }
}