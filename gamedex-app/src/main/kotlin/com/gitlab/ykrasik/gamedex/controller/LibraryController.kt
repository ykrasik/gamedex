package com.gitlab.ykrasik.gamedex.controller

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.repository.GameRepository
import com.gitlab.ykrasik.gamedex.repository.LibraryRepository
import com.gitlab.ykrasik.gamedex.ui.areYouSureDialog
import com.gitlab.ykrasik.gamedex.ui.fragment.AddLibraryFragment
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import tornadofx.Controller
import tornadofx.listview
import tornadofx.text
import tornadofx.vbox
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 23/04/2017
 * Time: 11:05
 */
@Singleton
class LibraryController @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val gameRepository: GameRepository
): Controller() {

    val librariesProperty get() = libraryRepository.librariesProperty

    fun addLibrary(): Boolean {
        var added = false
        launch(JavaFx) {
            val request = AddLibraryFragment().show() ?: return@launch
            libraryRepository.add(request)
            added = true
        }
        return added
    }

    fun delete(library: Library): Boolean {
        var deleted = false
        launch(JavaFx) {
            if (!confirmDelete(library)) return@launch

            log.info { "Deleting $library..." }
            libraryRepository.delete(library)
            gameRepository.deleteByLibrary(library)
            log.info { "Done" }
            deleted = true
        }
        return deleted
    }

    private fun confirmDelete(library: Library): Boolean {
        val baseMessage = "Delete library '${library.name}'?"
        val gamesToBeDeleted = gameRepository.gamesForLibrary(library)
        return areYouSureDialog {
            if (gamesToBeDeleted.size > 0) {
                dialogPane.content = vbox {
                    text("$baseMessage The following ${gamesToBeDeleted.size} games will also be deleted!")
                    listview(gamesToBeDeleted) {
                        // TODO: Limit max amount of items.
                        maxHeight = 400.0
                    }
                }
            } else {
                contentText = baseMessage
            }
        }
    }
}