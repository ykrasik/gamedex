package com.gitlab.ykrasik.gamedex.settings

import javafx.geometry.Pos
import tornadofx.getValue
import tornadofx.setValue

/**
 * User: ykrasik
 * Date: 01/05/2017
 * Time: 19:05
 */
class GameWallSettings : SettingsScope() {
    val cell = GameWallCellSettings()
    val metaTagOverlay = GameWallOverlaySettings().apply {
        location = Pos.BOTTOM_CENTER
        fillWidth = true
    }
    val versionOverlay = GameWallOverlaySettings().apply {
        location = Pos.TOP_RIGHT
        fillWidth = false
    }

    inner class GameWallCellSettings : SubSettings() {
        @Transient
        val imageDisplayTypeProperty = preferenceProperty(ImageDisplayType.fit)
        var imageDisplayType by imageDisplayTypeProperty

        @Transient
        val isFixedSizeProperty = preferenceProperty(false)
        var isFixedSize by isFixedSizeProperty

        @Transient
        val isShowBorderProperty = preferenceProperty(true)
        var isShowBorder by isShowBorderProperty

        @Transient
        val widthProperty = preferenceProperty(168.9)
        var width by widthProperty

        @Transient
        val heightProperty = preferenceProperty(168.9)
        var height by heightProperty

        @Transient
        val horizontalSpacingProperty = preferenceProperty(2.0)
        var horizontalSpacing by horizontalSpacingProperty

        @Transient
        val verticalSpacingProperty = preferenceProperty(3.0)
        var verticalSpacing by verticalSpacingProperty
    }

    inner class GameWallOverlaySettings : SubSettings() {
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