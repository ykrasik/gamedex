package com.gitlab.ykrasik.gamedex.ui.controller

import com.github.ykrasik.gamedex.common.datamodel.GamePlatform
import com.github.ykrasik.gamedex.common.datamodel.Library
import com.gitlab.ykrasik.gamedex.core.LibraryScanner
import com.gitlab.ykrasik.gamedex.ui.areYouSureDialog
import com.gitlab.ykrasik.gamedex.ui.model.GameRepository
import com.gitlab.ykrasik.gamedex.ui.model.LibraryRepository
import com.gitlab.ykrasik.gamedex.ui.view.fragment.AddLibraryFragment
import tornadofx.Controller
import tornadofx.listview
import tornadofx.text
import tornadofx.vbox

/**
 * User: ykrasik
 * Date: 10/10/2016
 * Time: 13:25
 */
// TODO: This class is redundant, the logic can sit in the view.
class LibraryController : Controller() {
    private val libraryScanner: LibraryScanner by di()

    private val libraryRepository: LibraryRepository by di()
    private val gameRepository: GameRepository by di()

    fun add(): Boolean {
        val request = AddLibraryFragment().show() ?: return false
        libraryRepository.add(request)
        return true
    }

    fun delete(library: Library) {
        if (confirmDelete(library)) {
            libraryRepository.delete(library)
        }
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
        libraryRepository.forEach { library ->
            if (library.platform != GamePlatform.excluded) {
                val task = libraryScanner.refresh(library)
                task.run()
            }
        }
    }
}