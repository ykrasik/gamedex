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

package com.gitlab.ykrasik.gamedex.core.game.presenter.filter

import com.gitlab.ykrasik.gamedex.app.api.game.ViewWithCurrentPlatformFilter
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 11/12/2018
 * Time: 08:53
 */
@Singleton
class CurrentPlatformFilterPresenter @Inject constructor(
    private val settingsService: SettingsService
) : Presenter<ViewWithCurrentPlatformFilter> {
    override fun present(view: ViewWithCurrentPlatformFilter) = object : ViewSession() {
        init {
            view.currentPlatformFilter *= settingsService.currentPlatformSettings.filter
            settingsService.game.platformChannel.forEach {
                view.currentPlatformFilter *= settingsService.currentPlatformSettings.filter
            }

            view.currentPlatformFilter.forEach {
                settingsService.currentPlatformSettings.modify { copy(filter = it) }
            }
        }
    }
}