package com.gitlab.ykrasik.gamedex.controller

import com.gitlab.ykrasik.gamedex.ui.fragment.SettingsFragment
import tornadofx.Controller

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 23:03
 */
class MainController : Controller() {
    fun showSettings() {
        SettingsFragment().show()
    }

    fun cleanup() {
        TODO()  // TODO: Implement
    }
}