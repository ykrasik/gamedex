package com.gitlab.ykrasik.gamedex.ui.model

import com.gitlab.ykrasik.gamedex.common.util.logger
import com.gitlab.ykrasik.gamedex.core.NotificationManager
import com.gitlab.ykrasik.gamedex.datamodel.Game
import com.gitlab.ykrasik.gamedex.datamodel.Library
import com.gitlab.ykrasik.gamedex.persistence.AddLibraryRequest
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import javafx.beans.property.ReadOnlyListProperty
import javafx.beans.property.SimpleListProperty
import javafx.collections.ObservableList
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.run
import tornadofx.getValue
import tornadofx.observable
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

    suspend fun add(request: AddLibraryRequest): Library {
        val library = run(CommonPool) {
            persistenceService.insert(request)
        }
        run(JavaFx) {
            libraries += library
        }
        return library
    }

    suspend fun delete(library: Library) {
        log.info { "Deleting $library..." }
        run(CommonPool) {
            persistenceService.deleteLibrary(library.id)
        }
        gameRepository.deleteByLibrary(library)
        run(JavaFx) {
            check(libraries.remove(library)) { "Error! Library doesn't exist: $library" }      // FIXME: Should this be runLater?
        }
        log.info { "Done" }
    }

    fun libraryForGame(game: Game): Library = libraries.find { it.id == game.libraryId } ?: throw IllegalStateException("No library found for game: $game!")
}