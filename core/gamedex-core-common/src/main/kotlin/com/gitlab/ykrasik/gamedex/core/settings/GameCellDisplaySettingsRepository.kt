/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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

import com.gitlab.ykrasik.gamedex.app.api.settings.ImageDisplayType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 16/06/2018
 * Time: 17:07
 */
@Singleton
class GameCellDisplaySettingsRepository @Inject constructor(settingsService: SettingsService) {
    data class Data(
        val imageDisplayType: ImageDisplayType,
        val showBorder: Boolean,
        val width: Double,
        val height: Double,
        val horizontalSpacing: Double,
        val verticalSpacing: Double
    )

    private val storage = settingsService.storage(basePath = "display", name = "cell") {
        Data(
            imageDisplayType = ImageDisplayType.Fixed,
            showBorder = true,
            width = 163.0,
            height = 163.0,
            horizontalSpacing = 3.0,
            verticalSpacing = 3.0
        )
    }

    val imageDisplayTypeChannel = storage.biChannel(Data::imageDisplayType) { copy(imageDisplayType = it) }
    var imageDisplayType by imageDisplayTypeChannel

    val showBorderChannel = storage.biChannel(Data::showBorder) { copy(showBorder = it) }
    var showBorder by showBorderChannel

    val widthChannel = storage.biChannel(Data::width) { copy(width = it) }
    var width by widthChannel

    val heightChannel = storage.biChannel(Data::height) { copy(height = it) }
    var height by heightChannel

    val horizontalSpacingChannel = storage.biChannel(Data::horizontalSpacing) { copy(horizontalSpacing = it) }
    var horizontalSpacing by horizontalSpacingChannel

    val verticalSpacingChannel = storage.biChannel(Data::verticalSpacing) { copy(verticalSpacing = it) }
    var verticalSpacing by verticalSpacingChannel

}