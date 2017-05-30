package com.gitlab.ykrasik.gamedex.settings

import tornadofx.getValue
import tornadofx.setValue

/**
 * User: ykrasik
 * Date: 01/05/2017
 * Time: 19:05
 */
class GameWallSettings private constructor() : AbstractSettings("gameWall") {
    companion object {
        operator fun invoke(): GameWallSettings = readOrUse(GameWallSettings())
    }

    @Transient
    val imageDisplayTypeProperty = preferenceProperty(ImageDisplayType.fit)
    var imageDisplayType by imageDisplayTypeProperty

    @Transient
    val cellWidthProperty = preferenceProperty(168.9)
    var cellWidth by cellWidthProperty

    @Transient
    val cellHeightProperty = preferenceProperty(168.9)
    var cellHeight by cellHeightProperty

    @Transient
    val cellHorizontalSpacingProperty = preferenceProperty(2.0)
    var cellHorizontalSpacing by cellHorizontalSpacingProperty

    @Transient
    val cellVerticalSpacingProperty = preferenceProperty(3.0)
    var cellVerticalSpacing by cellVerticalSpacingProperty

    enum class ImageDisplayType { fit, stretch }
}