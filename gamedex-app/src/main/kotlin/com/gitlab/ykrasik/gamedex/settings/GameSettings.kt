package com.gitlab.ykrasik.gamedex.settings

import com.gitlab.ykrasik.gamedex.Platform
import javafx.scene.control.TableColumn
import tornadofx.getValue
import tornadofx.setValue

/**
 * User: ykrasik
 * Date: 01/05/2017
 * Time: 19:08
 */
class GameSettings private constructor() : AbstractSettings("game") {
    companion object {
        operator fun invoke(): GameSettings = readOrUse(GameSettings())
    }

    @Transient
    val displayTypeProperty = preferenceProperty(DisplayType.wall)
    var displayType by displayTypeProperty

    @Transient
    val platformProperty = preferenceProperty(Platform.pc)
    var platform by platformProperty

    @Transient
    val sourceIdsPerPlatformProperty = preferenceProperty(emptyMap<Platform, List<Int>>())
    var sourceIdsPerPlatform by sourceIdsPerPlatformProperty

    @Transient
    val sortProperty = preferenceProperty(Sort.criticScore)
    var sort by sortProperty

    @Transient
    val sortOrderProperty = preferenceProperty(TableColumn.SortType.DESCENDING)
    var sortOrder by sortOrderProperty

    enum class Sort(val key: String) {
        name_("Name"),
        criticScore("Critic Score"),
        userScore("User Score"),
        minScore("Min Score"),
        avgScore("Average Score"),
        releaseDate("Release Date"),
        lastModified("Last Modified");

        override fun toString() = key
    }

    enum class DisplayType { wall, list }
}