package com.gitlab.ykrasik.gamedex.ui.view.game.edit

import com.gitlab.ykrasik.gamedex.BaseFragmentTestApp
import com.gitlab.ykrasik.gamedex.GameDataType
import com.gitlab.ykrasik.gamedex.test.randomGame

/**
 * User: ykrasik
 * Date: 13/05/2017
 * Time: 12:12
 */
object EditGameDataFragmentTestApp : BaseFragmentTestApp() {
    override fun init() {
        println("Result: " + EditGameDataFragment(randomGame(), initialTab = GameDataType.name_).show())
    }

    @JvmStatic fun main(args: Array<String>) {}
}