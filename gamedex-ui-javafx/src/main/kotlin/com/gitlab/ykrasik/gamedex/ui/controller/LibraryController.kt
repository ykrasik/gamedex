package com.gitlab.ykrasik.gamedex.ui.controller

import com.github.ykrasik.gamedex.datamodel.Game
import com.github.ykrasik.gamedex.datamodel.Library
import com.gitlab.ykrasik.gamedex.core.scan.LibraryScanner
import com.gitlab.ykrasik.gamedex.core.ui.LibraryUIManager
import com.gitlab.ykrasik.gamedex.persistence.dao.LibraryDao
import com.gitlab.ykrasik.gamedex.ui.util.areYouSureDialog
import com.gitlab.ykrasik.gamedex.ui.view.fragment.AddLibraryFragment
import javafx.beans.property.SimpleListProperty
import tornadofx.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 10/10/2016
 * Time: 13:25
 */
@Singleton
class LibraryController @Inject constructor(
    private val libraryDao: LibraryDao,
    private val gameController: GameController,
    private val libraryScanner: LibraryScanner
) : LibraryUIManager {

    val librariesProperty = SimpleListProperty(libraryDao.all.observable())
    override val all by librariesProperty

    fun add(): Boolean {
        val libraryData = AddLibraryFragment().show() ?: return false
        val library = libraryDao.add(libraryData)
        all += library
        return true
    }

    fun delete(library: Library) {
        if (!confirmDelete(library)) return

        gameController.deleteByLibrary(library)

        libraryDao.delete(library)
        check(all.remove(library)) { "Error! Didn't contain library: $library" }
    }

    private fun confirmDelete(library: Library): Boolean {
        val baseMessage = "Delete library '${library.name}'?"
        val gamesToBeDeleted = gameController.all.filtered { it.library == library }
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

    fun refreshLibraries() {
        all.forEach { refreshLibrary(it) }
    }

    private fun refreshLibrary(library: Library) {
        // FIXME: Run in a different thread.
        val token = libraryScanner.refresh(library)
        token.run()
    }
}