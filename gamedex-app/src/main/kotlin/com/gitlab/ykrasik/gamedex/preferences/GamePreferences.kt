package com.gitlab.ykrasik.gamedex.preferences

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
    val sortProperty = preferenceProperty(GameSort.criticScoreDesc)
    var sort by sortProperty

    @Transient
    val handsFreeModeProperty = preferenceProperty(false)
    var handsFreeMode by handsFreeModeProperty
}

enum class GameDisplayType { wall, list }

// TODO: I don't like displayName on the enum. Also - use FontAwesome.
enum class GameSort constructor(val displayName: String) {
    nameAsc("Name \u2191"),
    nameDesc("Name \u2193"),
    criticScoreAsc("Critic Score \u2191"),
    criticScoreDesc("Critic Score \u2193"),
    userScoreAsc("User Score \u2191"),
    userScoreDesc("User Score \u2193"),
    minScoreAsc("Min Score \u2191"),
    minScoreDesc("Min Score \u2193"),
    avgScoreAsc("Average Score \u2191"),
    avgScoreDesc("Average Score \u2193"),
    releaseDateAsc("Release Date \u2191"),
    releaseDateDesc("Release Date \u2193"),
    dateAddedAsc("Date Added \u2191"),
    dateAddedDesc("Date Added \u2193");

    override fun toString() = displayName
}