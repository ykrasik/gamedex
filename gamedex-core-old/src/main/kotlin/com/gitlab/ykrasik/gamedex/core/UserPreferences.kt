package com.gitlab.ykrasik.gamedex.core

import com.github.ykrasik.gamedex.core.manager.game.GameSort
import com.github.ykrasik.gamedex.core.ui.gridview.GameWallImageDisplay
import java.nio.file.Path

/**
 * User: ykrasik
 * Date: 11/10/2016
 * Time: 10:34
 */
data class UserPreferences(
    val isAutoSkip: Boolean = false,
    val isShowLog: Boolean = true,
    val logDividerPosition: Double = 0.98,
    val gameWallImageDisplay: GameWallImageDisplay = GameWallImageDisplay.FIT,
    val gameSort: GameSort = GameSort.NAME_ASC,
    val pevDirectory: Path? = null
)

interface UserPreferencesService {
    var preferences: UserPreferences
}

class UserPreferencesServiceImpl : UserPreferencesService {

}