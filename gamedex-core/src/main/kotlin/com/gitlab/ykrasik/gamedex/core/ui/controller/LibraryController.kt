package com.gitlab.ykrasik.gamedex.core.ui.controller

import com.github.ykrasik.gamedex.datamodel.Library
import com.gitlab.ykrasik.gamedex.core.LibraryScanner
import com.gitlab.ykrasik.gamedex.core.ui.areYouSureDialog
import com.gitlab.ykrasik.gamedex.core.ui.model.GameRepository
import com.gitlab.ykrasik.gamedex.core.ui.model.LibraryRepository
import com.gitlab.ykrasik.gamedex.core.ui.view.fragment.AddLibraryFragment
import tornadofx.Controller
import tornadofx.listview
import tornadofx.text
import tornadofx.vbox

/**
 * User: ykrasik
 * Date: 10/10/2016
 * Time: 13:25
 */
class LibraryController : Controller() {
    private val libraryScanner: LibraryScanner by di()

    private val libraryRepository: LibraryRepository by di()
    private val gameRepository: GameRepository by di()

    fun add(): Boolean {
        val libraryData = AddLibraryFragment().show() ?: return false
        libraryRepository.add(libraryData)
        return true
    }

    fun delete(library: Library) {
        if (!confirmDelete(library)) return

        gameRepository.deleteByLibrary(library.id)
        libraryRepository.delete(library)
    }

    private fun confirmDelete(library: Library): Boolean {
        val baseMessage = "Delete library '${library.name}'?"
        val gamesToBeDeleted = gameRepository.getByLibrary(library.id)
        return areYouSureDialog {
            if (gamesToBeDeleted.size > 0) {
                dialogPane.content = vbox {
                    text("?$baseMessage The following ${gamesToBeDeleted.size} games will also be deleted!")
                    listview(gamesToBeDeleted) {
                        maxHeight = 400.0
                    }
                }
            } else {
                contentText = baseMessage
            }
        }
    }

    fun refreshLibraries() {
        // FIXME: Run in a different thread.
        libraryRepository.all.forEach {
            val task = libraryScanner.refresh(it)
            task.run()
        }
    }
}