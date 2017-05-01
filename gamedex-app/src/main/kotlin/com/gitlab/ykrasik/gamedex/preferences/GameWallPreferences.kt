package com.gitlab.ykrasik.gamedex.preferences

import tornadofx.getValue
import tornadofx.setValue

/**
 * User: ykrasik
 * Date: 01/05/2017
 * Time: 19:05
 */
class GameWallPreferences private constructor() : UserPreferencesSet("gameWall") {
    companion object {
        operator fun invoke(): GameWallPreferences = readOrUse(GameWallPreferences())
    }

    @Transient
    val imageDisplayTypeProperty = preferenceProperty(ImageDisplayType.fit)
    var imageDisplayType by imageDisplayTypeProperty

    @Transient
    val cellWidthProperty = preferenceProperty(169.0)
    var cellWidth by cellWidthProperty

    @Transient
    val cellHeightProperty = preferenceProperty(169.0)
    var cellHeight by cellHeightProperty

    @Transient
    val cellHorizontalSpacingProperty = preferenceProperty(2.0)
    var cellHorizontalSpacing by cellHorizontalSpacingProperty

    @Transient
    val cellVerticalSpacingProperty = preferenceProperty(3.0)
    var cellVerticalSpacing by cellVerticalSpacingProperty

    @Transient
    val sortProperty = preferenceProperty(GameSort.criticScoreDesc)
    var sort by sortProperty
}

enum class ImageDisplayType { fit, stretch }