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

package com.gitlab.ykrasik.gamedex.javafx.game.wall

import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfig
import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfigScope
import com.gitlab.ykrasik.gamedex.util.Extractor
import com.gitlab.ykrasik.gamedex.util.NestedModifier
import javafx.geometry.Pos
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 01/05/2017
 * Time: 19:05
 */
@Singleton
class GameWallUserConfig : UserConfig() {
    override val scope = UserConfigScope("wall") {
        Data(
            cell = CellSettings(
                imageDisplayType = ImageDisplayType.stretch,
                fixedSize = true,
                showBorder = true,
                width = 166.0,
                height = 166.0,
                horizontalSpacing = 3.0,
                verticalSpacing = 3.0
            ),
            name = OverlaySettings(
                show = true,
                position = Pos.TOP_CENTER,
                fillWidth = true,
                fontSize = 13,
                bold = true,
                italic = false,
                textColor = "#ffffff",
                backgroundColor = "#4d66cc",
                opacity = 0.85,
                showOnlyWhenActive = true
            ),
            metaTag = OverlaySettings(
                show = true,
                position = Pos.BOTTOM_CENTER,
                fillWidth = true,
                fontSize = 12,
                bold = false,
                italic = true,
                textColor = "#000000",
                backgroundColor = "#cce6ff",
                opacity = 0.85,
                showOnlyWhenActive = true
            ),
            version = OverlaySettings(
                show = true,
                position = Pos.BOTTOM_RIGHT,
                fillWidth = false,
                fontSize = 16,
                bold = false,
                italic = true,
                textColor = "#000000",
                backgroundColor = "#D3D3D3",
                opacity = 0.85,
                showOnlyWhenActive = true
            )
        )
    }

    val cell = CellSettingsAccessor()
    val nameOverlay = OverlaySettingsAccessor(Data::name, Data::withName)
    val metaTagOverlay = OverlaySettingsAccessor(Data::metaTag, Data::withMetaTag)
    val versionOverlay = OverlaySettingsAccessor(Data::version, Data::withVersion)

    inner class CellSettingsAccessor {
        val imageDisplayTypeSubject = scope.subject({ cell.imageDisplayType }) { withCell { copy(imageDisplayType = it) } }
        var imageDisplayType by imageDisplayTypeSubject

        val isFixedSizeSubject = scope.subject({ cell.fixedSize }) { withCell { copy(fixedSize = it) } }
        var isFixedSize by isFixedSizeSubject

        val isShowBorderSubject = scope.subject({ cell.showBorder }) { withCell { copy(showBorder = it) } }
        var isShowBorder by isShowBorderSubject

        val widthSubject = scope.subject({ cell.width }) { withCell { copy(width = it) } }
        var width by widthSubject

        val heightSubject = scope.subject({ cell.height }) { withCell { copy(height = it) } }
        var height by heightSubject

        val horizontalSpacingSubject = scope.subject({ cell.horizontalSpacing }) { withCell { copy(horizontalSpacing = it) } }
        var horizontalSpacing by horizontalSpacingSubject

        val verticalSpacingSubject = scope.subject({ cell.verticalSpacing }) { withCell { copy(verticalSpacing = it) } }
        var verticalSpacing by verticalSpacingSubject
    }

    inner class OverlaySettingsAccessor(extractor: Extractor<Data, OverlaySettings>, modifier: NestedModifier<Data, OverlaySettings>) {
        val isShowSubject = scope.subject({ extractor().show }, { modifier { copy(show = it) } })
        var isShow by isShowSubject

        val positionSubject = scope.subject({ extractor().position }, { modifier { copy(position = it) } })
        var position by positionSubject

        val fillWidthSubject = scope.subject({ extractor().fillWidth }, { modifier { copy(fillWidth = it) } })
        var fillWidth by fillWidthSubject
        
        val fontSizeSubject = scope.subject({ extractor().fontSize }, { modifier { copy(fontSize = it) } })
        var fontSize by fontSizeSubject

        val boldSubject = scope.subject({ extractor().bold }, { modifier { copy(bold = it) } })
        var bold by boldSubject

        val italicSubject = scope.subject({ extractor().italic }, { modifier { copy(italic = it) } })
        var italic by italicSubject
        
        val textColorSubject = scope.subject({ extractor().textColor }, { modifier { copy(textColor = it) } })
        var textColor by textColorSubject

        val backgroundColorSubject = scope.subject({ extractor().backgroundColor }, { modifier { copy(backgroundColor = it) } })
        var backgroundColor by backgroundColorSubject

        val opacitySubject = scope.subject({ extractor().opacity }, { modifier { copy(opacity = it) } })
        var opacity by opacitySubject

        val showOnlyWhenActiveSubject = scope.subject({ extractor().showOnlyWhenActive }, { modifier { copy(showOnlyWhenActive = it) } })
        var showOnlyWhenActive by showOnlyWhenActiveSubject
    }

    enum class ImageDisplayType { fit, stretch }

    data class CellSettings(
        val imageDisplayType: ImageDisplayType,
        val fixedSize: Boolean,
        val showBorder: Boolean,
        val width: Double,
        val height: Double,
        val horizontalSpacing: Double,
        val verticalSpacing: Double
    )

    data class OverlaySettings(
        val show: Boolean,
        val position: Pos,
        val fillWidth: Boolean,
        val fontSize: Int,
        val bold: Boolean,
        val italic: Boolean,
        val textColor: String,
        val backgroundColor: String,
        val opacity: Double,
        val showOnlyWhenActive: Boolean
    )

    data class Data(
        val cell: CellSettings,
        val name: OverlaySettings,
        val metaTag: OverlaySettings,
        val version: OverlaySettings
    ) {
        fun withCell(f: CellSettings.() -> CellSettings) = copy(cell = f(cell))
        fun withName(f: OverlaySettings.() -> OverlaySettings) = copy(name = f(name))
        fun withMetaTag(f: OverlaySettings.() -> OverlaySettings) = copy(metaTag = f(metaTag))
        fun withVersion(f: OverlaySettings.() -> OverlaySettings) = copy(version = f(version))
    }
}