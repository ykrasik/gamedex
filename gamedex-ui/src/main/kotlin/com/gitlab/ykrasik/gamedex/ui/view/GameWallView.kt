package com.gitlab.ykrasik.gamedex.ui.view

import com.github.ykrasik.gamedex.datamodel.Game
import com.gitlab.ykrasik.gamedex.ui.util.gridView
import tornadofx.View

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 15:03
 */
class GameWallView : View("Games Wall") {
    override val root = gridView<Game> {
        cellHeight = 192.0
        cellWidth = 136.0
        horizontalCellSpacing = 3.0
        verticalCellSpacing = 3.0
    }
}
