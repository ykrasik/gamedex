package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.controller.LibraryController
import javafx.scene.control.ToolBar
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 22:17
 */
// TODO: This screen needs some work
class SourceView : GamedexView("Sources") {
    private val controller: LibraryController by di()

    override fun ToolBar.constructToolbar() {
    }

    override val root = tableview(controller.libraries) {

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
            menuitem("Delete") { selectedItem?.let { deleteLibrary(it) } }
        }
    }

    private fun addLibrary() {
        if (controller.addLibrary()) {
            root.resizeColumnsToFitContent()
        }
    }

    fun deleteLibrary(library: Library) {
        if (controller.delete(library)) {
            root.resizeColumnsToFitContent()
        }
    }
}