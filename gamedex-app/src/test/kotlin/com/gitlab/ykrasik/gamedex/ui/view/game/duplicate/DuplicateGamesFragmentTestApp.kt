package com.gitlab.ykrasik.gamedex.ui.view.game.duplicate

import com.gitlab.ykrasik.gamedex.BaseFragmentTestApp
import com.gitlab.ykrasik.gamedex.randomGame
import com.gitlab.ykrasik.gamedex.randomProviderHeader
import com.gitlab.ykrasik.gamedex.task.GameTasks
import com.gitlab.ykrasik.gamedex.test.randomElementExcluding
import com.gitlab.ykrasik.gamedex.test.rnd

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 12:41
 */
object DuplicateGamesFragmentTestApp : BaseFragmentTestApp() {
    override fun init() {
        val games = List(10) { randomGame() }
        val duplicates = games.map { game ->
            game to List(rnd.nextInt(3) + 1) {
                GameTasks.GameDuplication(games.randomElementExcluding(game), randomProviderHeader())
            }
        }.toMap()
        DuplicateGamesFragment(duplicates).show()
    }

    @JvmStatic fun main(args: Array<String>) {}
}