package com.gitlab.ykrasik.gamedex.ui.fragment

import com.gitlab.ykrasik.gamedex.BaseTestApp
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch

/**
 * User: ykrasik
 * Date: 18/03/2017
 * Time: 17:11
 */
object AddLibraryFragmentTestApp : BaseTestApp() {
    override fun init() {
        launch(CommonPool) {
            println("Result: " + AddLibraryFragment().show())
            System.exit(0)
        }
    }

    @JvmStatic fun main(args: Array<String>) {  }
}