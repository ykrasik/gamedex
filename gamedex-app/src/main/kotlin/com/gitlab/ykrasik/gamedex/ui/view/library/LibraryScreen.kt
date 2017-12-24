package com.gitlab.ykrasik.gamedex.ui.view.library

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.controller.LibraryController
import com.gitlab.ykrasik.gamedex.ui.allowDeselection
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import com.gitlab.ykrasik.gamedex.ui.theme.addButton
import com.gitlab.ykrasik.gamedex.ui.theme.deleteButton
import com.gitlab.ykrasik.gamedex.ui.theme.editButton
import com.gitlab.ykrasik.gamedex.ui.verticalSeparator
import com.gitlab.ykrasik.gamedex.ui.view.GamedexScreen
import javafx.scene.control.ToolBar
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 22:17
 */
// TODO: This screen needs some work
// TODO: Show total amount of games and total game size.
class LibraryScreen : GamedexScreen("Libraries", Theme.Icon.hdd()) {
    private val libraryController: LibraryController by di()

    override fun ToolBar.constructToolbar() {
        addButton { setOnAction { addLibrary() } }
        verticalSeparator()
        editButton {
            disableWhen { root.selectionModel.selectedItemProperty().isNull }
            setOnAction { editLibrary() }
        }
        verticalSeparator()
        spacer()
        verticalSeparator()
        deleteButton("Delete") {
            disableWhen { root.selectionModel.selectedItemProperty().isNull }
            setOnAction { deleteLibrary() }
        }
    }

    override val root = tableview(libraryController.allLibraries) {
        isEditable = false
        columnResizePolicy = SmartResize.POLICY

        column("Name", Library::name) {
            isSortable = false
            contentWidth(padding = 10.0, useAsMin = true)
        }
        column("Platform", Library::platform) {
            isSortable = false
            contentWidth(padding = 10.0, useAsMin = true)
        }
        column("Path", Library::path) {
            isSortable = false
            contentWidth(padding = 100.0, useAsMin = true)
            remainingWidth()
        }

        contextmenu {
            item("Add", graphic = Theme.Icon.plus(20.0)).action { addLibrary() }
            separator()
            item("Edit", graphic = Theme.Icon.edit(20.0)) {
                disableWhen { this@tableview.selectionModel.selectedItemProperty().isNull }
            }.action { editLibrary() }
            separator()
            item("Delete", graphic = Theme.Icon.delete(20.0)) {
                disableWhen { this@tableview.selectionModel.selectedItemProperty().isNull }
            }.action { deleteLibrary() }

        }

        allowDeselection(onClickAgain = false)
    }

    private fun addLibrary() {
        if (libraryController.addLibrary()) {
            root.resizeColumnsToFitContent()
        }
    }

    private fun editLibrary() {
        if (libraryController.edit(selectedLibrary))
            root.resizeColumnsToFitContent()
    }

    private fun deleteLibrary() {
        if (libraryController.delete(selectedLibrary)) {
            root.resizeColumnsToFitContent()
        }
    }

    private val selectedLibrary get() = root.selectedItem!!
}