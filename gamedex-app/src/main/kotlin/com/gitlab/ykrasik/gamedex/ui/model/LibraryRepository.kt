package com.gitlab.ykrasik.gamedex.ui.model

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.LibraryData
import com.gitlab.ykrasik.gamedex.core.NotificationManager
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.util.logger
import javafx.beans.property.ReadOnlyListProperty
import javafx.beans.property.SimpleListProperty
import javafx.collections.ObservableList
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.run
import tornadofx.getValue
import tornadofx.observable
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 25/12/2016
 * Time: 19:47
 */
@Singleton
class LibraryRepository @Inject constructor(
    private val persistenceService: PersistenceService,
    private val gameRepository: GameRepository,
    private val notificationManager: NotificationManager
) {
    private val log by logger()

    val librariesProperty: ReadOnlyListProperty<Library> = run {
        notificationManager.message("Fetching libraries...")
        SimpleListProperty(persistenceService.fetchAllLibraries().observable())
    }
    val libraries: ObservableList<Library> by librariesProperty

    suspend fun add(request: AddLibraryRequest): Library = run(JavaFx) {
        val library = run(CommonPool) {
            persistenceService.insertLibrary(request.path, request.data)
        }
        libraries += library
        library
    }

    suspend fun delete(library: Library) = run(JavaFx) {
        log.info { "Deleting $library..." }
        run(CommonPool) {
            persistenceService.deleteLibrary(library.id)
        }
        gameRepository.deleteByLibrary(library)
        check(libraries.remove(library)) { "Error! Library doesn't exist: $library" }
        log.info { "Done" }
    }

    fun libraryForGame(game: Game): Library = libraries.find { it.id == game.libraryId } ?: throw IllegalStateException("No library found for game: $game!")
}

data class AddLibraryRequest(
    val path: File,
    val data: LibraryData
)