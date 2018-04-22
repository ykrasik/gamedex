/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 * http://www.apache.org/licenses/LICENSE-2.0                               *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ****************************************************************************/

package com.gitlab.ykrasik.gamedex.javafx.library

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.core.api.library.AddLibraryRequest
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryViewModel
import com.gitlab.ykrasik.gamedex.core.api.presenters
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.dialog.areYouSureDialog
import com.gitlab.ykrasik.gamedex.javafx.screen.PresentableGamedexScreen
import javafx.scene.control.ToolBar
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 22:17
 */
// TODO: This screen needs some work
// TODO: Show total amount of games and total game size.
class LibraryScreen : PresentableGamedexScreen<LibraryViewModel.Event, LibraryViewModel.Action, LibraryViewModel>(
    "Libraries", Theme.Icon.hdd(), presenters.libraryPresenter::present, skipFirst = true
) {      // This is first called when the mainView is loaded (even though this view isn't shown) - skip first time.

    private val editLibraryView: EditLibraryView by inject()

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

    override val root = tableview<Library> {
        isEditable = false
        columnResizePolicy = SmartResize.POLICY

        readonlyColumn("Name", Library::name) {
            isSortable = false
            contentWidth(padding = 10.0, useAsMin = true)
        }
        readonlyColumn("Platform", Library::platform) {
            isSortable = false
            contentWidth(padding = 10.0, useAsMin = true)
        }
        readonlyColumn("Path", Library::path) {
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

    override fun LibraryViewModel.onPresent() {
        root.items = toObservableList { libraries }
        libraries.changesChannel.subscribe(JavaFx) {
            root.resizeColumnsToFitContent()
        }
    }

    override suspend fun onAction(action: LibraryViewModel.Action) {
        when (action) {
            LibraryViewModel.Action.ShowAddLibraryView -> {
                val request = addOrEditLibrary<EditLibraryView.Choice.AddNewLibrary, AddLibraryRequest>(library = null) { it.request }
                sendEvent(LibraryViewModel.Event.AddLibraryViewClosed(request))
            }
            is LibraryViewModel.Action.ShowEditLibraryView -> {
                val updatedLibrary = addOrEditLibrary<EditLibraryView.Choice.EditLibrary, Library>(action.library) { it.library }
                sendEvent(LibraryViewModel.Event.EditLibraryViewClosed(action.library, updatedLibrary))
            }
            is LibraryViewModel.Action.ShowDeleteLibraryConfirmDialog -> {
                val confirm = areYouSureDialog("Delete library '${action.library.name}'?") {
                    val gamesToBeDeleted = action.gamesToBeDeleted
                    if (gamesToBeDeleted.isNotEmpty()) {
                        label("The following ${gamesToBeDeleted.size} games will also be deleted:")
                        listview(gamesToBeDeleted.map { it.name }.observable()) { fitAtMost(10) }
                    }
                }
                sendEvent(LibraryViewModel.Event.DeleteLibraryConfirmDialogClosed(action.library, confirm))
            }
        }
    }

    private inline fun <reified T : EditLibraryView.Choice, U> addOrEditLibrary(library: Library?,
                                                                                f: (T) -> U): U? {
        val choice = editLibraryView.show(library)
        if (choice === EditLibraryView.Choice.Cancel) return null
        return f(choice as T)
    }

    private fun addLibrary() = launch { sendEvent(LibraryViewModel.Event.AddLibraryClicked) }
    private fun editLibrary() = launch { sendEvent(LibraryViewModel.Event.EditLibraryClicked(selectedLibrary)) }
    private fun deleteLibrary() = launch { sendEvent(LibraryViewModel.Event.DeleteLibraryClicked(selectedLibrary)) }

    private val selectedLibrary: Library get() = root.selectedItem!!
}