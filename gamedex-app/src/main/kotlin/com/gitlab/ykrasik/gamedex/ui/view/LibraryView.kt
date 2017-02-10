package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.common.datamodel.Library
import com.gitlab.ykrasik.gamedex.ui.controller.LibraryController
import com.gitlab.ykrasik.gamedex.ui.model.LibraryRepository
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 22:17
 */
class LibraryView : View("Libraries") {
    private val controller: LibraryController by di()
    private val repository: LibraryRepository by di()

    override val root = tableview<Library> {
        itemsProperty().bind(repository.librariesProperty)

        isEditable = false
        columnResizePolicy = SmartResize.POLICY

        column("Name", Library::name) {
            isSortable = false
            contentWidth(padding = 10.0, useAsMin = true)
        }
        column("Path", Library::path) {
            isSortable = false
            contentWidth(padding = 100.0, useAsMin = true)
            remainingWidth()
        }
        column("Platform", Library::platform) {
            isSortable = false
            contentWidth(padding = 10.0, useAsMin = true)
        }

        contextmenu {
            menuitem("Add") { addLibrary() }
            separator()
            menuitem("Delete") { selectedItem?.let { controller.delete(it) } }
        }
    }

    private fun addLibrary() {
        if (controller.add()) {
            root.resizeColumnsToFitContent()
        }
    }
}