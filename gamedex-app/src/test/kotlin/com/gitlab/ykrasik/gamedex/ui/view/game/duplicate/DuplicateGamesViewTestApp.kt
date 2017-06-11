package com.gitlab.ykrasik.gamedex.ui.view.game.duplicate

import com.gitlab.ykrasik.gamedex.BaseTestApp
import com.gitlab.ykrasik.gamedex.controller.ReportsController
import com.gitlab.ykrasik.gamedex.core.GameDuplication
import com.gitlab.ykrasik.gamedex.randomGame
import com.gitlab.ykrasik.gamedex.randomProviderHeader
import com.gitlab.ykrasik.gamedex.test.randomElementExcluding
import com.gitlab.ykrasik.gamedex.test.rnd
import com.gitlab.ykrasik.gamedex.ui.view.report.DuplicateGamesView
import tornadofx.FX

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 12:41
 */
object DuplicateGamesViewTestApp : BaseTestApp<DuplicateGamesView>(DuplicateGamesView::class) {
    override fun init(view: DuplicateGamesView) {
        val games = List(10) { randomGame() }
        val duplicates = games.map { game ->
            game to List(rnd.nextInt(3) + 1) {
                GameDuplication(randomProviderHeader().id, games.randomElementExcluding(game))
            }
        }.toMap()

        FX.dicontainer!!.getInstance(ReportsController::class).duplications.resultsProperty.value = duplicates
    }

    @JvmStatic fun main(args: Array<String>) {}
}