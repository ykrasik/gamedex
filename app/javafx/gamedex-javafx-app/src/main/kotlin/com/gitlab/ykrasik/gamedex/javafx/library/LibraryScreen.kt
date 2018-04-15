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
import com.gitlab.ykrasik.gamedex.core.api.library.*
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.dialog.areYouSureDialog
import com.gitlab.ykrasik.gamedex.javafx.screen.GamedexScreen
import javafx.scene.control.ToolBar
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.consumeEach
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
class LibraryScreen : GamedexScreen("Libraries", Theme.Icon.hdd()), LibraryView {
    private val libraryPresenter: LibraryPresenter by di()
    // TODO: Have this data as output of the present() method. Not the repo, but all the libraries.
    private val libraryRepository: LibraryRepository by di()

    override val outputEvents = Channel<LibraryViewEvent>(10)
    override val inputActions = Channel<LibraryViewAction>(10)

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

    override val root = tableview(libraryRepository.libraries.toObservableList()) {
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

    init {
        libraryPresenter.bindView(this)
        libraryRepository.libraries.changesChannel.subscribe(JavaFx) {
            root.resizeColumnsToFitContent()
        }
        javaFx {
            inputActions.consumeEach { action ->
                when (action) {
                    LibraryViewAction.ShowAddLibraryView -> {
                        val request = addOrEditLibrary<LibraryFragment.Choice.AddNewLibrary, AddLibraryRequest>(library = null) { it.request }
                        sendEvent(LibraryViewEvent.AddLibraryViewClosed(request))
                    }
                    is LibraryViewAction.ShowEditLibraryView -> {
                        val updatedLibrary = addOrEditLibrary<LibraryFragment.Choice.EditLibrary, Library>(action.library) { it.library }
                        sendEvent(LibraryViewEvent.EditLibraryViewClosed(action.library, updatedLibrary))
                    }
                    is LibraryViewAction.ShowDeleteLibraryConfirmDialog -> {
                        val confirm = areYouSureDialog("Delete library '${action.library.name}'?") {
                            val gamesToBeDeleted = action.gamesToBeDeleted
                            if (gamesToBeDeleted.isNotEmpty()) {
                                label("The following ${gamesToBeDeleted.size} games will also be deleted:")
                                listview(gamesToBeDeleted.map { it.name }.observable()) { fitAtMost(10) }
                            }
                        }
                        sendEvent(LibraryViewEvent.DeleteLibraryConfirmDialogClosed(action.library, confirm))
                    }
                }
            }
        }
    }

    private suspend inline fun <reified T : LibraryFragment.Choice, U> addOrEditLibrary(library: Library?,
                                                                                        noinline f: suspend (T) -> U): U? {
        val choice = LibraryFragment(library).show()
        if (choice === LibraryFragment.Choice.Cancel) return null
        return f(choice as T)
    }

    private fun addLibrary() = launch { sendEvent(LibraryViewEvent.AddLibraryClicked) }
    private fun editLibrary() = launch { sendEvent(LibraryViewEvent.EditLibraryClicked(selectedLibrary)) }
    private fun deleteLibrary() = launch { sendEvent(LibraryViewEvent.DeleteLibraryClicked(selectedLibrary)) }

    private val selectedLibrary: Library get() = root.selectedItem!!

    private suspend fun sendEvent(event: LibraryViewEvent) = outputEvents.send(event)
}