package com.gitlab.ykrasik.gamedex.ui.controller

import com.github.ykrasik.gamedex.datamodel.Game
import com.github.ykrasik.gamedex.datamodel.Library
import com.gitlab.ykrasik.gamedex.persistence.dao.LibraryDao
import com.gitlab.ykrasik.gamedex.ui.util.areYouSureDialog
import com.gitlab.ykrasik.gamedex.ui.view.fragment.AddLibraryFragment
import javafx.beans.property.SimpleListProperty
import tornadofx.*

/**
 * User: ykrasik
 * Date: 10/10/2016
 * Time: 13:25
 */
class LibraryController : Controller() {
    private val gameController: GameController by inject()

    private val libraryDao: LibraryDao by di()

    val librariesProperty = SimpleListProperty(libraryDao.all.observable())
    val libraries by librariesProperty

    fun add(): Boolean {
        val libraryData = AddLibraryFragment().show() ?: return false
        val library = libraryDao.add(libraryData)
        libraries += library
        return true
    }

    fun delete(library: Library) {
        if (!confirmDelete(library)) return

        gameController.deleteByLibrary(library)

        libraryDao.delete(library)
        libraries -= library
    }

    private fun confirmDelete(library: Library): Boolean {
        val baseMessage = "Delete library '${library.name}'?"
        val gamesToBeDeleted = gameController.games.filter { it.library == library }.observable()
        return areYouSureDialog {
            if (gamesToBeDeleted.size > 0) {
                dialogPane.content = vbox {
                    text("?$baseMessage The following ${gamesToBeDeleted.size} games will also be deleted!")
                    listview<Game>(gamesToBeDeleted) {
                        maxHeight = 400.0
                    }
                }
            } else {
                contentText = baseMessage
            }
        }
    }
}