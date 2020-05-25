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

import com.gitlab.ykrasik.gamedex.app.api.settings.ProviderOrderSettingsView
import com.gitlab.ykrasik.gamedex.core.CommonData
import com.gitlab.ykrasik.gamedex.core.settings.ProviderOrderSettingsRepository
import com.gitlab.ykrasik.gamedex.core.view.Presenter
import com.gitlab.ykrasik.gamedex.core.view.ViewSession
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 20/06/2018
 * Time: 09:26
 */
@Singleton
class ProviderOrderSettingsPresenter @Inject constructor(
    private val commonData: CommonData,
    private val settingsRepo: ProviderOrderSettingsRepository
) : Presenter<ProviderOrderSettingsView> {
    override fun present(view: ProviderOrderSettingsView) = object : ViewSession() {
        init {
            view::canChangeProviderOrder *= commonData.disableWhenGameSyncIsRunning

            view::search.bindBidirectional(settingsRepo.search)
            view::name.bindBidirectional(settingsRepo.name)
            view::description.bindBidirectional(settingsRepo.description)
            view::releaseDate.bindBidirectional(settingsRepo.releaseDate)
            view::thumbnail.bindBidirectional(settingsRepo.thumbnail)
            view::poster.bindBidirectional(settingsRepo.poster)
            view::screenshot.bindBidirectional(settingsRepo.screenshot)
        }

        private fun verifyCanChange() = view.canChangeProviderOrder.assert()
    }
}