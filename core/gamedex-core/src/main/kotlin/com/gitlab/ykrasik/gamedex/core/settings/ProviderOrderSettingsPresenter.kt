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

import com.gitlab.ykrasik.gamedex.app.api.settings.ProviderOrderSettingsView
import com.gitlab.ykrasik.gamedex.core.Presentation
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.api.provider.GameProviderService
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

            settingsService.provider.bind({ searchOrderChannel }, view::search, view.searchChanges) { modifyOrder { copy(search = it) } }
            settingsService.provider.bind({ nameOrderChannel }, view::name, view.nameChanges) { modifyOrder { copy(name = it) } }
            settingsService.provider.bind({ descriptionOrderChannel }, view::description, view.descriptionChanges) { modifyOrder { copy(description = it) } }
            settingsService.provider.bind({ releaseDateOrderChannel }, view::releaseDate, view.releaseDateChanges) { modifyOrder { copy(releaseDate = it) } }
            settingsService.provider.bind({ criticScoreOrderChannel }, view::criticScore, view.criticScoreChanges) { modifyOrder { copy(criticScore = it) } }
            settingsService.provider.bind({ userScoreOrderChannel }, view::userScore, view.userScoreChanges) { modifyOrder { copy(userScore = it) } }
            settingsService.provider.bind({ thumbnailOrderChannel }, view::thumbnail, view.thumbnailChanges) { modifyOrder { copy(thumbnail = it) } }
            settingsService.provider.bind({ posterOrderChannel }, view::poster, view.posterChanges) { modifyOrder { copy(poster = it) } }
            settingsService.provider.bind({ screenshotOrderChannel }, view::screenshot, view.screenshotChanges) { modifyOrder { copy(screenshot = it) } }
        }
    }
}