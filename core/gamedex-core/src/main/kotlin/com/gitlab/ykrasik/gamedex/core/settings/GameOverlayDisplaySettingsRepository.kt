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

import com.gitlab.ykrasik.gamedex.app.api.settings.DisplayPosition

/**
 * User: ykrasik
 * Date: 09/06/2018
 * Time: 20:43
 */
data class OverlayDisplaySettings(
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

abstract class AbstractGameOverlayDisplaySettingsRepository(name: String) : SettingsRepository<OverlayDisplaySettings>(name, OverlayDisplaySettings::class) {
    val enabledChannel = channel(OverlayDisplaySettings::enabled)
    val showOnlyWhenActiveChannel = channel(OverlayDisplaySettings::showOnlyWhenActive)
    val positionChannel = channel(OverlayDisplaySettings::position)
    val fillWidthChannel = channel(OverlayDisplaySettings::fillWidth)
    val fontSizeChannel = channel(OverlayDisplaySettings::fontSize)
    val boldFontChannel = channel(OverlayDisplaySettings::boldFont)
    val italicFontChannel = channel(OverlayDisplaySettings::italicFont)
    val textColorChannel = channel(OverlayDisplaySettings::textColor)
    val backgroundColorChannel = channel(OverlayDisplaySettings::backgroundColor)
    val opacityChannel = channel(OverlayDisplaySettings::opacity)
}

class GameNameDisplaySettingsRepository : AbstractGameOverlayDisplaySettingsRepository("display_name") {
    override fun defaultSettings() = OverlayDisplaySettings(
        enabled = true,
        showOnlyWhenActive = true,
        position = DisplayPosition.TopCenter,
        fillWidth = true,
        fontSize = 13,
        boldFont = true,
        italicFont = false,
        textColor = "#ffffff",
        backgroundColor = "#4d66cc",
        opacity = 0.85
    )
}

class GameMetaTagDisplaySettingsRepository : AbstractGameOverlayDisplaySettingsRepository("display_metatag") {
    override fun defaultSettings() = OverlayDisplaySettings(
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

class GameVersionDisplaySettingsRepository : AbstractGameOverlayDisplaySettingsRepository("display_version") {
    override fun defaultSettings() = OverlayDisplaySettings(
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