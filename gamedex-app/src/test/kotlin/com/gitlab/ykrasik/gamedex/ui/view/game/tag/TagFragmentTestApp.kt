package com.gitlab.ykrasik.gamedex.ui.view.game.tag

import com.gitlab.ykrasik.gamedex.BaseFragmentTestApp
import com.gitlab.ykrasik.gamedex.test.randomGame

/**
 * User: ykrasik
 * Date: 13/05/2017
 * Time: 12:12
 */
object TagFragmentTestApp : BaseFragmentTestApp() {
    override fun init() {
        println("Result: " + TagFragment(randomGame()).show())
    }

    @JvmStatic fun main(args: Array<String>) {}
}