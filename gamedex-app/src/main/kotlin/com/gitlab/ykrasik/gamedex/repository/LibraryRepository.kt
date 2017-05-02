package com.gitlab.ykrasik.gamedex.repository

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.LibraryData
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.util.logger
import javafx.beans.property.SimpleListProperty
import javafx.collections.ObservableList
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.run
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
    private val persistenceService: PersistenceService
) {
    private val log = logger()

    val libraries: ObservableList<Library> = run {
        log.info("Fetching libraries...")
        val libraries = SimpleListProperty(persistenceService.fetchAllLibraries().observable())
        log.info("Fetched ${libraries.size} libraries.")
        libraries
    }

    suspend fun add(request: AddLibraryRequest): Library {
        val library = run(CommonPool) {
            persistenceService.insertLibrary(request.path, request.data)
        }
        run(JavaFx) {
            libraries += library
        }
        return library
    }

    suspend fun delete(library: Library) {
        log.info("Deleting '${library.name}'...")
        run(CommonPool) {
            persistenceService.deleteLibrary(library.id)
        }
        run(JavaFx) {
            check(libraries.remove(library)) { "Error! Library doesn't exist: $library" }
        }
        log.info("Deleting '${library.name}': Done.")
    }

    operator fun get(id: Int): Library = libraries.find { it.id == id } ?: throw IllegalStateException("No library found for id: $id!")
}

data class AddLibraryRequest(
    val path: File,
    val data: LibraryData
)