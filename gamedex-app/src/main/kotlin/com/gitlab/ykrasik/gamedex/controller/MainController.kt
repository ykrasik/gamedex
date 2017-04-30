package com.gitlab.ykrasik.gamedex.controller

import com.gitlab.ykrasik.gamedex.ui.fragment.SettingsFragment
import tornadofx.Controller
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 23:03
 */
@Singleton
class MainController : Controller() {
    fun showSettings() {
        SettingsFragment().show()
    }
}