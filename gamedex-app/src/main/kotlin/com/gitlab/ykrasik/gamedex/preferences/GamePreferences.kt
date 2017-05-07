package com.gitlab.ykrasik.gamedex.preferences

import com.gitlab.ykrasik.gamedex.Platform
import tornadofx.getValue
import tornadofx.setValue

/**
 * User: ykrasik
 * Date: 01/05/2017
 * Time: 19:08
 */
class GamePreferences private constructor() : UserPreferencesSet("game") {
    companion object {
        operator fun invoke(): GamePreferences = readOrUse(GamePreferences())
    }

    @Transient
    val displayTypeProperty = preferenceProperty(GameDisplayType.wall)
    var displayType by displayTypeProperty

    @Transient
    val platformProperty = preferenceProperty(Platform.pc)
    var platform by platformProperty

    @Transient
    val handsFreeModeProperty = preferenceProperty(false)
    var handsFreeMode by handsFreeModeProperty
}

enum class GameDisplayType { wall, list }