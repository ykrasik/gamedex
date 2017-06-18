package com.gitlab.ykrasik.gamedex.ui.view.game.details

import com.gitlab.ykrasik.gamedex.BaseTestApp
import com.gitlab.ykrasik.gamedex.test.randomGame

/**
 * User: ykrasik
 * Date: 01/04/2017
 * Time: 14:06
 */
object GameDetailsScreenTestApp : BaseTestApp<GameDetailsScreen>(GameDetailsScreen::class) {
    override fun init(view: GameDetailsScreen) {
        view.game = randomGame()
    }

    @JvmStatic fun main(args: Array<String>) {}
}