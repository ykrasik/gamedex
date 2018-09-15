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

import com.gitlab.ykrasik.gamedex.app.api.settings.ProviderOrderSettingsView
import com.gitlab.ykrasik.gamedex.core.Presentation
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.api.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 20/06/2018
 * Time: 09:26
 */
@Singleton
class ProviderOrderSettingsPresenter @Inject constructor(
    private val gameProviderService: GameProviderService,
    private val settingsService: SettingsService
) : Presenter<ProviderOrderSettingsView> {
    override fun present(view: ProviderOrderSettingsView) = object : Presentation() {
        init {
            view.providerLogos = gameProviderService.logos

            settingsService.providerOrder.bind({ searchChannel }, view::search, view.searchChanges) { copy(search = it) }
            settingsService.providerOrder.bind({ nameChannel }, view::name, view.nameChanges) { copy(name = it) }
            settingsService.providerOrder.bind({ descriptionChannel }, view::description, view.descriptionChanges) { copy(description = it) }
            settingsService.providerOrder.bind({ releaseDateChannel }, view::releaseDate, view.releaseDateChanges) { copy(releaseDate = it) }
            settingsService.providerOrder.bind({ criticScoreChannel }, view::criticScore, view.criticScoreChanges) { copy(criticScore = it) }
            settingsService.providerOrder.bind({ userScoreChannel }, view::userScore, view.userScoreChanges) { copy(userScore = it) }
            settingsService.providerOrder.bind({ thumbnailChannel }, view::thumbnail, view.thumbnailChanges) { copy(thumbnail = it) }
            settingsService.providerOrder.bind({ posterChannel }, view::poster, view.posterChanges) { copy(poster = it) }
            settingsService.providerOrder.bind({ screenshotChannel }, view::screenshot, view.screenshotChanges) { copy(screenshot = it) }
        }
    }
}