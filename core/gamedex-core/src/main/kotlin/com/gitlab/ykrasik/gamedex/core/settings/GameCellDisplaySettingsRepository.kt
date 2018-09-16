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

import com.gitlab.ykrasik.gamedex.app.api.settings.ImageDisplayType

/**
 * User: ykrasik
 * Date: 16/06/2018
 * Time: 17:07
 */
class GameCellDisplaySettingsRepository(factory: SettingsStorageFactory) : SettingsRepository<GameCellDisplaySettingsRepository.Data>() {
    data class Data(
        val imageDisplayType: ImageDisplayType,
        val showBorder: Boolean,
        val width: Double,
        val height: Double,
        val horizontalSpacing: Double,
        val verticalSpacing: Double
    )

    override val storage = factory("display_cell", Data::class) {
        Data(
            imageDisplayType = ImageDisplayType.Stretch,
            showBorder = true,
            width = 163.0,
            height = 163.0,
            horizontalSpacing = 3.0,
            verticalSpacing = 3.0
        )
    }

    val imageDisplayTypeChannel = storage.channel(Data::imageDisplayType)
    val showBorderChannel = storage.channel(Data::showBorder)
    val widthChannel = storage.channel(Data::width)
    val heightChannel = storage.channel(Data::height)
    val horizontalSpacingChannel = storage.channel(Data::horizontalSpacing)
    val verticalSpacingChannel = storage.channel(Data::verticalSpacing)
}