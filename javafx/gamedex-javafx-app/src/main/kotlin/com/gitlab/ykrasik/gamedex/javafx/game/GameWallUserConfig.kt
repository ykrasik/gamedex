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

package com.gitlab.ykrasik.gamedex.javafx.game

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
            metaTag = OverlaySettings(
                show = true,
                position = Pos.BOTTOM_CENTER,
                fillWidth = true
            ),
            version = OverlaySettings(
                show = true,
                position = Pos.TOP_RIGHT,
                fillWidth = false
            )
        )
    }

    val cell = CellSettingsAccessor()
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
        val fillWidth: Boolean
    )

    data class Data(
        val cell: CellSettings,
        val metaTag: OverlaySettings,
        val version: OverlaySettings
    ) {
        fun withCell(f: CellSettings.() -> CellSettings) = copy(cell = f(cell))
        fun withMetaTag(f: OverlaySettings.() -> OverlaySettings) = copy(metaTag = f(metaTag))
        fun withVersion(f: OverlaySettings.() -> OverlaySettings) = copy(version = f(version))
    }
}