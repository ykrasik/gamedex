package com.gitlab.ykrasik.gamedex.core.ui.controller

import com.github.ykrasik.gamedex.datamodel.Library
import com.gitlab.ykrasik.gamedex.core.scan.LibraryScanner
import com.gitlab.ykrasik.gamedex.core.ui.areYouSureDialog
import com.gitlab.ykrasik.gamedex.core.ui.model.GamesModel
import com.gitlab.ykrasik.gamedex.core.ui.model.LibrariesModel
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

    private val librariesModel: LibrariesModel by di()
    private val gamesModel: GamesModel by di()

    fun add(): Boolean {
        val libraryData = AddLibraryFragment().show() ?: return false
        librariesModel.add(libraryData)
        return true
    }

    fun delete(library: Library) {
        if (!confirmDelete(library)) return

        gamesModel.deleteByLibrary(library)
        librariesModel.delete(library)
    }

    private fun confirmDelete(library: Library): Boolean {
        val baseMessage = "Delete library '${library.name}'?"
        val gamesToBeDeleted = gamesModel.getByLibrary(library)
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
        librariesModel.all.forEach {
            val task = libraryScanner.refresh(it)
            task.run()
        }
    }
}