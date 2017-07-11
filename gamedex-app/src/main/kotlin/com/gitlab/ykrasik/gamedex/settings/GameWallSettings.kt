package com.gitlab.ykrasik.gamedex.settings

import javafx.geometry.Pos
import tornadofx.getValue
import tornadofx.setValue

/**
 * User: ykrasik
 * Date: 01/05/2017
 * Time: 19:05
 */
class GameWallSettings private constructor() : Settings("gameWall") {
    companion object {
        operator fun invoke(): GameWallSettings = readOrUse(GameWallSettings())
    }

    val cell = CellSettings()

    val metaTagOverlay = OverlaySettings().apply {
        location = Pos.BOTTOM_CENTER
        fillWidth = true
    }

    val versionOverlay = OverlaySettings().apply {
        location = Pos.TOP_RIGHT
        fillWidth = false
    }

    inner class CellSettings {
        @Transient
        val imageDisplayTypeProperty = preferenceProperty(ImageDisplayType.stretch)
        var imageDisplayType by imageDisplayTypeProperty

        @Transient
        val isFixedSizeProperty = preferenceProperty(true)
        var isFixedSize by isFixedSizeProperty

        @Transient
        val isShowBorderProperty = preferenceProperty(true)
        var isShowBorder by isShowBorderProperty

        @Transient
        val widthProperty = preferenceProperty(166)
        var width by widthProperty

        @Transient
        val heightProperty = preferenceProperty(166)
        var height by heightProperty

        @Transient
        val horizontalSpacingProperty = preferenceProperty(3.0)
        var horizontalSpacing by horizontalSpacingProperty

        @Transient
        val verticalSpacingProperty = preferenceProperty(3.0)
        var verticalSpacing by verticalSpacingProperty
    }

    inner class OverlaySettings {
        @Transient
        val isShowProperty = preferenceProperty(true)
        var isShow by isShowProperty

        @Transient
        val locationProperty = preferenceProperty(Pos.BOTTOM_CENTER)
        var location by locationProperty

        @Transient
        val fillWidthProperty = preferenceProperty(true)
        var fillWidth by fillWidthProperty
    }

    enum class ImageDisplayType { fit, stretch }
}