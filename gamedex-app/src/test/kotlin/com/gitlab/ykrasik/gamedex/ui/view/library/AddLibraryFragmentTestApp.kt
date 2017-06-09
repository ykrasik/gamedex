package com.gitlab.ykrasik.gamedex.ui.view.library

import com.gitlab.ykrasik.gamedex.BaseFragmentTestApp

/**
 * User: ykrasik
 * Date: 18/03/2017
 * Time: 17:11
 */
object AddLibraryFragmentTestApp : BaseFragmentTestApp() {
    override fun init() {
        println("Result: " + LibraryFragment(emptyList(), library = null).show())
    }

    @JvmStatic fun main(args: Array<String>) {  }
}