package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.ui.areYouSureDialog
import com.gitlab.ykrasik.gamedex.ui.model.GameRepository
import com.gitlab.ykrasik.gamedex.ui.model.LibraryRepository
import com.gitlab.ykrasik.gamedex.ui.view.fragment.AddLibraryFragment
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 22:17
 */
class LibraryView : View("Libraries") {
    private val libraryRepository: LibraryRepository by di()
    private val gameRepository: GameRepository by di()

    override val root = tableview<Library> {
        itemsProperty().bind(libraryRepository.librariesProperty)

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
        launch(JavaFx) {
            val request = AddLibraryFragment().show() ?: return@launch
            libraryRepository.add(request)
            root.resizeColumnsToFitContent()
        }
    }

    fun deleteLibrary(library: Library) {
        if (confirmDelete(library)) {
            launch(JavaFx) {
                libraryRepository.delete(library)
            }
        }
    }

    private fun confirmDelete(library: Library): Boolean {
        val baseMessage = "Delete library '${library.name}'?"
        val gamesToBeDeleted = gameRepository.games.filtered { it.libraryId == library.id }
        return areYouSureDialog {
            if (gamesToBeDeleted.size > 0) {
                dialogPane.content = vbox {
                    text("$baseMessage The following ${gamesToBeDeleted.size} games will also be deleted!")
                    listview(gamesToBeDeleted) {
                        maxHeight = 400.0
                    }
                }
            } else {
                contentText = baseMessage
            }
        }
    }
}