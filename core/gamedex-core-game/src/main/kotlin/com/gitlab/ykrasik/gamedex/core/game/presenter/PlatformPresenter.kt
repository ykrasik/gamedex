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

package com.gitlab.ykrasik.gamedex.core.game.presenter

import com.gitlab.ykrasik.gamedex.app.api.game.AvailablePlatform
import com.gitlab.ykrasik.gamedex.app.api.game.ViewWithPlatform
import com.gitlab.ykrasik.gamedex.core.CommonData
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.settings.GameSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/06/2018
 * Time: 09:45
 */
@Singleton
class PlatformPresenter @Inject constructor(
    private val settingsRepo: GameSettingsRepository,
    private val commonData: CommonData
) : Presenter<ViewWithPlatform> {
    override fun present(view: ViewWithPlatform) = object : ViewSession() {
        init {
            commonData.platformsWithLibraries.itemsChannel.forEach { platforms ->
                val availablePlatforms = (if (platforms.size > 1) listOf(AvailablePlatform.All) else emptyList()) +
                    platforms.map(AvailablePlatform::SinglePlatform)
                view.availablePlatforms.setAll(availablePlatforms)

                if (availablePlatforms.size == 1) {
                    val platform = availablePlatforms.first()
                    view.currentPlatform *= platform
                    settingsRepo.platform = platform
                }
            }
            settingsRepo.platformChannel.bind(view.currentPlatform)
        }
    }
}