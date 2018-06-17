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

package com.gitlab.ykrasik.gamedex.core.game.all

import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanSelectPlatform
import com.gitlab.ykrasik.gamedex.core.Presentation
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.common.CommonData
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/06/2018
 * Time: 09:45
 */
@Singleton
class SelectPlatformPresenter @Inject constructor(
    private val settingsService: SettingsService,
    private val commonData: CommonData
) : Presenter<ViewCanSelectPlatform> {
    override fun present(view: ViewCanSelectPlatform) = object : Presentation() {
        init {
            commonData.platformsWithLibraries.bindTo(view.availablePlatforms)
            settingsService.game.bind({ platformChannel }, view::currentPlatform, view.currentPlatformChanges) {
                copy(platform = it)
            }
        }
    }
}