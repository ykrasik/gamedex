package com.gitlab.ykrasik.gamedex.ui.view.library

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
            println("Result: " + LibraryFragment(emptyList(), library = null).show())
            System.exit(0)
        }
    }

    @JvmStatic fun main(args: Array<String>) {  }
}