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

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryView
import com.gitlab.ykrasik.gamedex.core.api.presenters
import com.gitlab.ykrasik.gamedex.core.api.util.ListObservable
import com.gitlab.ykrasik.gamedex.core.api.util.ListObservableImpl
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.dialog.areYouSureDialog
import com.gitlab.ykrasik.gamedex.javafx.screen.GamedexScreen
import javafx.scene.control.ToolBar
import kotlinx.coroutines.experimental.channels.Channel
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 22:17
 */
// TODO: This screen needs some work
// TODO: Show total amount of games and total game size.
class JavaFxLibraryScreen : GamedexScreen("Libraries", Theme.Icon.hdd()), LibraryView {
    private val editLibraryView: JavaFxEditLibraryView by inject()

    override val events = Channel<LibraryView.Event>(32)
    override var libraries: ListObservable<Library> = ListObservableImpl()
        set(value) {
            field = value
            value.toObservableList(observableLibraries)
        }
    private val observableLibraries = mutableListOf<Library>().observable()

    init {
        presenters.libraryPresenter.present(this)

        observableLibraries.onChange {
            root.resizeColumnsToFitContent()
        }
    }

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

    override val root = tableview(observableLibraries) {
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

    override fun showAddLibraryView() = editLibraryView.show(library = null)

    override fun showEditLibraryView(library: Library) = editLibraryView.show(library)

    override fun confirmDeleteLibrary(library: Library, gamesToBeDeleted: List<Game>) =
        areYouSureDialog("Delete library '${library.name}'?") {
            if (gamesToBeDeleted.isNotEmpty()) {
                label("The following ${gamesToBeDeleted.size} games will also be deleted:")
                listview(gamesToBeDeleted.map { it.name }.observable()) { fitAtMost(10) }
            }
        }

    private fun addLibrary() = sendEvent(LibraryView.Event.AddLibraryClicked)
    private fun editLibrary() = sendEvent(LibraryView.Event.EditLibraryClicked(selectedLibrary))
    private fun deleteLibrary() = sendEvent(LibraryView.Event.DeleteLibraryClicked(selectedLibrary))

    private val selectedLibrary: Library get() = root.selectedItem!!

    private fun sendEvent(event: LibraryView.Event) = events.offer(event)
}