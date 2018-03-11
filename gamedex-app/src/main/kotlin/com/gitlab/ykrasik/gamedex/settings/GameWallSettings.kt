package com.gitlab.ykrasik.gamedex.settings

import com.gitlab.ykrasik.gamedex.util.Extractor
import com.gitlab.ykrasik.gamedex.util.NestedModifier
import javafx.geometry.Pos
import tornadofx.getValue
import tornadofx.setValue
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 01/05/2017
 * Time: 19:05
 */
@Singleton
class GameWallSettings : UserSettings() {
    override val repo = SettingsRepo("wall") {
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
                fillWidth = true
            )
        )
    }

    val cell = CellSettingsAccessor()
    val metaTagOverlay = OverlaySettingsAccessor(Data::metaTag, Data::withMetaTag)
    val versionOverlay = OverlaySettingsAccessor(Data::version, Data::withVersion)

    inner class CellSettingsAccessor {
        val imageDisplayTypeProperty = repo.property({ cell.imageDisplayType }) { withCell { copy(imageDisplayType = it) } }
        var imageDisplayType by imageDisplayTypeProperty

        val isFixedSizeProperty = repo.booleanProperty({ cell.fixedSize }) { withCell { copy(fixedSize = it) } }
        var isFixedSize by isFixedSizeProperty

        val isShowBorderProperty = repo.booleanProperty({ cell.showBorder }) { withCell { copy(showBorder = it) } }
        var isShowBorder by isShowBorderProperty

        val widthProperty = repo.doubleProperty({ cell.width }) { withCell { copy(width = it) } }
        var width by widthProperty

        val heightProperty = repo.doubleProperty({ cell.height }) { withCell { copy(height = it) } }
        var height by heightProperty

        val horizontalSpacingProperty = repo.doubleProperty({ cell.horizontalSpacing }) { withCell { copy(horizontalSpacing = it) } }
        var horizontalSpacing by horizontalSpacingProperty

        val verticalSpacingProperty = repo.doubleProperty({ cell.verticalSpacing }) { withCell { copy(verticalSpacing = it) } }
        var verticalSpacing by verticalSpacingProperty
    }

    inner class OverlaySettingsAccessor(extractor: Extractor<Data, OverlaySettings>, modifier: NestedModifier<Data, OverlaySettings>) {
        val isShowProperty = repo.booleanProperty({ extractor().show }, { modifier { copy(show = it) } })
        var isShow by isShowProperty

        val positionProperty = repo.property({ extractor().position }, { modifier { copy(position = it) } })
        var position by positionProperty

        val fillWidthProperty = repo.booleanProperty({ extractor().fillWidth }, { modifier { copy(fillWidth = it) } })
        var fillWidth by fillWidthProperty
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