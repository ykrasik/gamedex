package com.gitlab.ykrasik.gamedex.ui.view.settings

import com.gitlab.ykrasik.gamedex.BaseFragmentTestApp

/**
 * User: ykrasik
 * Date: 28/05/2017
 * Time: 12:55
 */
object SettingsFragmentTestApp : BaseFragmentTestApp() {
    override fun init() {
        println("Result: " + SettingsView().show())
    }

    @JvmStatic fun main(args: Array<String>) {}
}