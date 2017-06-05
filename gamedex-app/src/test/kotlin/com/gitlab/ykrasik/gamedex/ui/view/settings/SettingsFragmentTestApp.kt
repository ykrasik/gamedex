package com.gitlab.ykrasik.gamedex.ui.view.settings

import com.gitlab.ykrasik.gamedex.BaseTestApp

/**
 * User: ykrasik
 * Date: 28/05/2017
 * Time: 12:55
 */
object SettingsFragmentTestApp : BaseTestApp() {
    override fun init() {
        println("Result: " + SettingsFragment().show())
        System.exit(0)
    }

    @JvmStatic fun main(args: Array<String>) {}
}