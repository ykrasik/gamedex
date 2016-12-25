package com.gitlab.ykrasik.gamedex.core.view

import com.github.ykrasik.gamedex.datamodel.Game
import com.gitlab.ykrasik.gamedex.core.controller.GameController
import com.gitlab.ykrasik.gamedex.core.util.gridView
import tornadofx.View

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 15:03
 */
class GameWallView : View("Games Wall") {
    private val controller: GameController by di()

    override val root = gridView<Game> {
        cellHeight = 192.0
        cellWidth = 136.0
        horizontalCellSpacing = 3.0
        verticalCellSpacing = 3.0

        itemsProperty().bind(controller.gamesProperty)
    }
}
