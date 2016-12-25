package com.gitlab.ykrasik.gamedex.core.controller

import com.github.ykrasik.gamedex.datamodel.Game
import com.github.ykrasik.gamedex.datamodel.Library
import com.gitlab.ykrasik.gamedex.core.scan.LibraryScanner
import com.gitlab.ykrasik.gamedex.core.util.areYouSureDialog
import com.gitlab.ykrasik.gamedex.core.view.fragment.AddLibraryFragment
import com.gitlab.ykrasik.gamedex.persistence.dao.LibraryDao
import javafx.beans.property.SimpleListProperty
import javafx.collections.ObservableList
import tornadofx.*
import java.nio.file.Path
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
)  {
    private val librariesProperty = SimpleListProperty(libraryDao.all.observable())
    val all: ObservableList<Library> by librariesProperty

    fun contains(path: Path): Boolean = all.any { it.path == path }

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