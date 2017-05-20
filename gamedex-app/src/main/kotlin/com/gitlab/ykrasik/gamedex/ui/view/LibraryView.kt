package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.controller.LibraryController
import com.gitlab.ykrasik.gamedex.ui.*
import javafx.scene.control.ToolBar
import org.controlsfx.glyphfont.FontAwesome
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 22:17
 */
// TODO: This screen needs some work
class LibraryView : GamedexScreen("Libraries") {
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
        deleteButton {
            disableWhen { root.selectionModel.selectedItemProperty().isNull }
            setOnAction { deleteLibrary() }
        }
    }

    override val root = tableview(libraryController.libraries) {
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
            menuitem("Add", graphic = FontAwesome.Glyph.PLUS.toGraphic()) { addLibrary() }
            separator()
            menuitem("Edit", graphic = FontAwesome.Glyph.PENCIL.toGraphic()) { editLibrary() }.apply {
                disableWhen { this@tableview.selectionModel.selectedItemProperty().isNull }
            }
            separator()
            menuitem("Delete", graphic = FontAwesome.Glyph.TRASH.toGraphic()) { deleteLibrary() }.apply {
                disableWhen { this@tableview.selectionModel.selectedItemProperty().isNull }
            }

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