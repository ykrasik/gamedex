package com.gitlab.ykrasik.gamedex.ui.view

import com.github.ykrasik.gamedex.datamodel.Library
import com.gitlab.ykrasik.gamedex.ui.controller.LibraryController
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 22:17
 */
class LibraryView : View("Libraries") {
    private val controller: LibraryController by inject()

    override val root = tableview<Library> {
        isEditable = false
        columnResizePolicy = SmartResize.POLICY

        column("Name", Library::name) {
            isSortable = false
            contentWidth(padding = 10.0, useAsMin = true)
        }
        column("Path", Library::path) {
            isSortable = false
            contentWidth(padding = 10.0, useAsMin = true)
            remainingWidth()
        }
        column("Platform", Library::platform) {
            isSortable = false
            contentWidth(padding = 10.0, useAsMin = true)
        }

        contextmenu {
            menuitem("Add") { controller.add() }
            separator()
            menuitem("Delete") { controller.delete() }
        }
    }
}