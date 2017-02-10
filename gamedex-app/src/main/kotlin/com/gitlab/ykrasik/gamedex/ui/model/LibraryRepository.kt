package com.gitlab.ykrasik.gamedex.ui.model

import com.gitlab.ykrasik.gamedex.common.datamodel.Library
import com.gitlab.ykrasik.gamedex.common.util.logger
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

    val librariesProperty: ReadOnlyListProperty<Library> = SimpleListProperty(persistenceService.fetchAllLibraries().observable())
    private val libraries: ObservableList<Library> by librariesProperty

    override fun iterator() = libraries.iterator()

    fun add(request: AddLibraryRequest): Library {
        val library = persistenceService.insert(request)
        libraries += library        // FIXME: Should this be runLater?
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