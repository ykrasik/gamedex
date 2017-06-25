package com.gitlab.ykrasik.gamedex.controller

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.repository.GameRepository
import com.gitlab.ykrasik.gamedex.repository.LibraryRepository
import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.ui.filtering
import com.gitlab.ykrasik.gamedex.ui.fitAtMost
import com.gitlab.ykrasik.gamedex.ui.sortedFiltered
import com.gitlab.ykrasik.gamedex.ui.toPredicateF
import com.gitlab.ykrasik.gamedex.ui.view.dialog.areYouSureDialog
import com.gitlab.ykrasik.gamedex.ui.view.library.LibraryFragment
import com.gitlab.ykrasik.gamedex.ui.view.main.MainView
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import tornadofx.Controller
import tornadofx.label
import tornadofx.listview
import tornadofx.observable
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
    private val gameRepository: GameRepository,
    settings: GameSettings
) : Controller() {

    val allLibraries = libraryRepository.libraries.sortedFiltered()
    val realLibraries = allLibraries.filtered { it.platform != Platform.excluded }
    val platformLibraries = realLibraries.filtering(settings.platformProperty.toPredicateF { platform, library: Library ->
        library.platform == platform
    })

    fun addLibrary(): Boolean {
        var added = false
        val choice = LibraryFragment(libraryRepository.libraries, library = null).show()
        if (choice is LibraryFragment.Choice.AddNewSource) {
            launch(JavaFx) { libraryRepository.add(choice.request) }
            added = true
        }
        return added
    }

    fun edit(library: Library): Boolean {
        var added = false
        val choice = LibraryFragment(libraryRepository.libraries, library).show()
        if (choice is LibraryFragment.Choice.EditSource) {
            launch(JavaFx) {
                libraryRepository.update(choice.library)
                gameRepository.softInvalidate()
            }
            added = true
        }
        return added
    }

    fun delete(library: Library): Boolean {
        var deleted = false
        launch(JavaFx) {
            if (!confirmDelete(library)) return@launch

            libraryRepository.delete(library)
            gameRepository.hardInvalidate()
            deleted = true

            MainView.showFlashInfoNotification("Deleted library: '${library.name}'.")
        }
        return deleted
    }

    private fun confirmDelete(library: Library): Boolean {
        val gamesToBeDeleted = gameRepository.games
            .asSequence()
            .filter { it.library.id == library.id }
            .map { it.name }
            .toList()
        return areYouSureDialog("Delete library '${library.name}'?") {
            if (gamesToBeDeleted.isNotEmpty()) {
                label("The following ${gamesToBeDeleted.size} games will also be deleted:")
                listview(gamesToBeDeleted.observable()) { fitAtMost(10) }
            }
        }
    }

    fun getBy(platform: Platform, name: String) = libraryRepository.getBy(platform, name)
}