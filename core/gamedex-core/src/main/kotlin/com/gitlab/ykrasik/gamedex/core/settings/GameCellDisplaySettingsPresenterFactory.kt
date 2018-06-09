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

import com.gitlab.ykrasik.gamedex.app.api.settings.ViewCanChangeGameCellDisplaySettings
import com.gitlab.ykrasik.gamedex.app.api.settings.ViewWithGameCellDisplaySettings
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.PresenterFactory
import com.gitlab.ykrasik.gamedex.core.userconfig.SettingsService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 10/06/2018
 * Time: 12:30
 */
@Singleton
class ChangeGameCellDisplaySettingsPresenterFactory @Inject constructor(
    private val settingsService: SettingsService
) : PresenterFactory<ViewCanChangeGameCellDisplaySettings> {
    override fun present(view: ViewCanChangeGameCellDisplaySettings) = object : Presenter() {
        init {
            val settings = settingsService.gameDisplay.cell
            with(view.mutableCellDisplaySettings) {
                settings.imageDisplayTypeChannel.bind(::imageDisplayType, imageDisplayTypeChanges)
                settings.showBorderChannel.bind(::showBorder, showBorderChanges)
                settings.widthChannel.bind(::width, widthChanges)
                settings.heightChannel.bind(::height, heightChanges)
                settings.horizontalSpacingChannel.bind(::horizontalSpacing, horizontalSpacingChanges)
                settings.verticalSpacingChannel.bind(::verticalSpacing, verticalSpacingChanges)
            }
        }
    }
}

@Singleton
class GameCellDisplaySettingsPresenterFactory @Inject constructor(
    private val settingsService: SettingsService
) : PresenterFactory<ViewWithGameCellDisplaySettings> {
    override fun present(view: ViewWithGameCellDisplaySettings) = object : Presenter() {
        init {
            val settings = settingsService.gameDisplay.cell
            with(view.cellDisplaySettings) {
                settings.imageDisplayTypeChannel.reportChangesTo(::imageDisplayType)
                settings.showBorderChannel.reportChangesTo(::showBorder)
                settings.widthChannel.reportChangesTo(::width)
                settings.heightChannel.reportChangesTo(::height)
                settings.horizontalSpacingChannel.reportChangesTo(::horizontalSpacing)
                settings.verticalSpacingChannel.reportChangesTo(::verticalSpacing)
            }
        }
    }
}