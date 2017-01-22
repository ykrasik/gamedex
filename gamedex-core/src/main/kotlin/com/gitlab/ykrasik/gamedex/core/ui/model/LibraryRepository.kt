package com.gitlab.ykrasik.gamedex.core.ui.model

import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.datamodel.Library
import com.gitlab.ykrasik.gamedex.persistence.AddLibraryRequest
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import javafx.beans.property.ReadOnlyListProperty
import javafx.beans.property.SimpleListProperty
import javafx.collections.ObservableList
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
    private val gameRepository: GameRepository
) : Iterable<Library> {
    private val log by logger()

    val librariesProperty: ReadOnlyListProperty<Library> = run {
        log.info { "Fetching all libraries..." }
        val libraries = persistenceService.fetchAllLibraries()
        log.info { "Result: ${libraries.size} libraries." }
        SimpleListProperty(libraries.observable())
    }
    private val libraries: ObservableList<Library> by librariesProperty

    override fun iterator() = libraries.iterator()

    fun add(request: AddLibraryRequest): Library {
        log.info { "$request..."}
        val id = persistenceService.insert(request)
        val library = Library(id, request.path, request.data)
        libraries += library        // FIXME: Should this be runLater?
        log.info { "Result: $library." }
        return library
    }

    fun delete(library: Library) {
        log.info { "Deleting $library..." }
        persistenceService.deleteLibrary(library.id)
        // TODO: Instead of calling gameRepository.deleteByLibrary(library), consider just re-fetching all games & libraries.
        gameRepository.deleteByLibrary(library)
        check(libraries.remove(library)) { "Error! Library doesn't exist: $library" }      // FIXME: Should this be runLater?
        log.info { "Done" }
    }

    fun getByPath(path: File): Library? = libraries.find { it.path == path }
}