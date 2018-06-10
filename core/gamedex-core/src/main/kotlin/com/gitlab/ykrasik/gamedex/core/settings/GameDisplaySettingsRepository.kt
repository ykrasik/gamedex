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
import com.gitlab.ykrasik.gamedex.app.api.settings.ImageDisplayType
import com.gitlab.ykrasik.gamedex.util.Extractor
import com.gitlab.ykrasik.gamedex.util.NestedModifier

/**
 * User: ykrasik
 * Date: 09/06/2018
 * Time: 20:43
 */
class GameDisplaySettingsRepository : SettingsRepository<GameDisplaySettingsRepository.Data>("display", Data::class) {
    data class Data(
        val cell: CellDisplaySettings,
        val name: OverlayDisplaySettings,
        val metaTag: OverlayDisplaySettings,
        val version: OverlayDisplaySettings
    ) {
        fun withCell(f: CellDisplaySettings.() -> CellDisplaySettings) = copy(cell = f(cell))
        fun withName(f: OverlayDisplaySettings.() -> OverlayDisplaySettings) = copy(name = f(name))
        fun withMetaTag(f: OverlayDisplaySettings.() -> OverlayDisplaySettings) = copy(metaTag = f(metaTag))
        fun withVersion(f: OverlayDisplaySettings.() -> OverlayDisplaySettings) = copy(version = f(version))
    }

    data class CellDisplaySettings(
        val imageDisplayType: ImageDisplayType,
        val showBorder: Boolean,
        val width: Double,
        val height: Double,
        val horizontalSpacing: Double,
        val verticalSpacing: Double
    )

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

    override fun defaultSettings() = Data(
        cell = CellDisplaySettings(
            imageDisplayType = ImageDisplayType.Stretch,
            showBorder = true,
            width = 166.0,
            height = 166.0,
            horizontalSpacing = 3.0,
            verticalSpacing = 3.0
        ),
        name = OverlayDisplaySettings(
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
        ),
        metaTag = OverlayDisplaySettings(
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
        ),
        version = OverlayDisplaySettings(
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
    )

    val cell = CellSettingsAccessor()
    val nameOverlay = OverlaySettingsAccessor(Data::name, Data::withName)
    val metaTagOverlay = OverlaySettingsAccessor(Data::metaTag, Data::withMetaTag)
    val versionOverlay = OverlaySettingsAccessor(Data::version, Data::withVersion)

    inner class CellSettingsAccessor {
        val imageDisplayTypeChannel = map({ cell.imageDisplayType }) { withCell { copy(imageDisplayType = it) } }
        val showBorderChannel = map({ cell.showBorder }) { withCell { copy(showBorder = it) } }
        val widthChannel = map({ cell.width }) { withCell { copy(width = it) } }
        val heightChannel = map({ cell.height }) { withCell { copy(height = it) } }
        val horizontalSpacingChannel = map({ cell.horizontalSpacing }) { withCell { copy(horizontalSpacing = it) } }
        val verticalSpacingChannel = map({ cell.verticalSpacing }) { withCell { copy(verticalSpacing = it) } }
    }

    inner class OverlaySettingsAccessor(extractor: Extractor<Data, OverlayDisplaySettings>, modifier: NestedModifier<Data, OverlayDisplaySettings>) {
        val enabledChannel = map({ extractor().enabled }, { modifier { copy(enabled = it) } })
        val showOnlyWhenActiveChannel = map({ extractor().showOnlyWhenActive }, { modifier { copy(showOnlyWhenActive = it) } })
        val positionChannel = map({ extractor().position }, { modifier { copy(position = it) } })
        val fillWidthChannel = map({ extractor().fillWidth }, { modifier { copy(fillWidth = it) } })
        val fontSizeChannel = map({ extractor().fontSize }, { modifier { copy(fontSize = it) } })
        val boldFontChannel = map({ extractor().boldFont }, { modifier { copy(boldFont = it) } })
        val italicFontChannel = map({ extractor().italicFont }, { modifier { copy(italicFont = it) } })
        val textColorChannel = map({ extractor().textColor }, { modifier { copy(textColor = it) } })
        val backgroundColorChannel = map({ extractor().backgroundColor }, { modifier { copy(backgroundColor = it) } })
        val opacityChannel = map({ extractor().opacity }, { modifier { copy(opacity = it) } })
    }
}