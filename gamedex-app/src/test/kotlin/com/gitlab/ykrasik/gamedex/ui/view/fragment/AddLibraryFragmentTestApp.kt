package com.gitlab.ykrasik.gamedex.ui.view.fragment

import com.gitlab.ykrasik.gamedex.ui.view.BaseTestApp
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch

/**
 * User: ykrasik
 * Date: 18/03/2017
 * Time: 17:11
 */
class AddLibraryFragmentTestApp : BaseTestApp() {
    override fun init() {
        launch(CommonPool) {
            println("Result: " + AddLibraryFragment().show())
            System.exit(0)
        }
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) { AddLibraryFragmentTestApp() }
    }
}