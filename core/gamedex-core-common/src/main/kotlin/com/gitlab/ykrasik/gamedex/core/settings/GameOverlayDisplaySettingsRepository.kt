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

import com.gitlab.ykrasik.gamedex.app.api.settings.DisplayPosition

/**
 * User: ykrasik
 * Date: 09/06/2018
 * Time: 20:43
 */
class GameOverlayDisplaySettingsRepository(name: String, factory: SettingsStorageFactory, default: () -> Data) :
    SettingsRepository<GameOverlayDisplaySettingsRepository.Data>() {

    data class Data(
        val enabled: Boolean,
        val showOnlyWhenActive: Boolean,
        val position: DisplayPosition,
        val fillWidth: Boolean,
        val fontSize: Int,
        val boldFont: Boolean,
        val italicFont: Boolean,
        val textColor: String,
        val backgroundColor: String,
        val opacity: Double
    )

    override val storage = factory(name, Data::class, default)

    val enabledChannel = storage.channel(Data::enabled)
    val showOnlyWhenActiveChannel = storage.channel(Data::showOnlyWhenActive)
    val positionChannel = storage.channel(Data::position)
    val fillWidthChannel = storage.channel(Data::fillWidth)
    val fontSizeChannel = storage.channel(Data::fontSize)
    val boldFontChannel = storage.channel(Data::boldFont)
    val italicFontChannel = storage.channel(Data::italicFont)
    val textColorChannel = storage.channel(Data::textColor)
    val backgroundColorChannel = storage.channel(Data::backgroundColor)
    val opacityChannel = storage.channel(Data::opacity)

    companion object {
        fun name(factory: SettingsStorageFactory) = GameOverlayDisplaySettingsRepository("name", factory) {
            Data(
                enabled = true,
                showOnlyWhenActive = true,
                position = DisplayPosition.TopCenter,
                fillWidth = true,
                fontSize = 13,
                boldFont = true,
                italicFont = false,
                textColor = "#ffffff",
                backgroundColor = "#62728C",
                opacity = 0.85
            )
        }

        fun metaTag(factory: SettingsStorageFactory) = GameOverlayDisplaySettingsRepository("metatag", factory) {
            Data(
                enabled = true,
                showOnlyWhenActive = true,
                position = DisplayPosition.BottomCenter,
                fillWidth = true,
                fontSize = 12,
                boldFont = false,
                italicFont = true,
                textColor = "#000000",
                backgroundColor = "#cce6ff",
                opacity = 0.85
            )
        }

        fun version(factory: SettingsStorageFactory) = GameOverlayDisplaySettingsRepository("version", factory) {
            Data(
                enabled = true,
                showOnlyWhenActive = true,
                position = DisplayPosition.BottomRight,
                fillWidth = false,
                fontSize = 16,
                boldFont = false,
                italicFont = true,
                textColor = "#000000",
                backgroundColor = "#D3D3D3",
                opacity = 0.85
            )
        }
    }
}