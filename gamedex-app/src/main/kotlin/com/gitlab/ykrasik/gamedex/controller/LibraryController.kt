package com.gitlab.ykrasik.gamedex.controller

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.repository.GameRepository
import com.gitlab.ykrasik.gamedex.repository.LibraryRepository
import com.gitlab.ykrasik.gamedex.ui.areYouSureDialog
import com.gitlab.ykrasik.gamedex.ui.fragment.AddLibraryFragment
import com.gitlab.ykrasik.gamedex.ui.widgets.Notification
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import tornadofx.*
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

    val libraries = SortedFilteredList(libraryRepository.libraries)

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

            libraryRepository.delete(library)
            gameRepository.invalidate()
            deleted = true

            Notification()
                .text("Deleted library: '${library.name}")
                .information()
                .automaticallyHideAfter(2.seconds)
                .show()
        }
        return deleted
    }

    private fun confirmDelete(library: Library): Boolean {
        val baseMessage = "Delete library '${library.name}'?"
        val gamesToBeDeleted = gameRepository.games.filter { it.library.id == library.id }
        return areYouSureDialog {
            if (gamesToBeDeleted.isNotEmpty()) {
                dialogPane.content = vbox {
                    text("$baseMessage The following games will also be deleted:")
                    val maxGamesToDisplay = 10
                    val messages = gamesToBeDeleted.asSequence().map { it.name }.take(maxGamesToDisplay).let {
                        if (gamesToBeDeleted.size > maxGamesToDisplay) {
                            it + "And ${gamesToBeDeleted.size - maxGamesToDisplay} other games..."
                        } else {
                            it
                        }
                    }.toList()
                    listview(messages.observable())
                }
            } else {
                contentText = baseMessage
            }
        }
    }
}