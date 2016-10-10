package com.gitlab.ykrasik.gamedex.ui.view

import com.github.ykrasik.gamedex.datamodel.ExcludedPath
import com.gitlab.ykrasik.gamedex.ui.controller.ExcludedPathController
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 22:52
 */
class ExcludedPathView : View("Excluded Paths") {
    private val controller: ExcludedPathController by inject()

    override val root = listview<ExcludedPath> {
        contextmenu {
            menuitem("Add") { controller.add() }
            separator()
            menuitem("Delete") { controller.delete() }
        }
    }
}