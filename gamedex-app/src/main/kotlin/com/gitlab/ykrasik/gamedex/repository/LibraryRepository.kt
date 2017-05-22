package com.gitlab.ykrasik.gamedex.repository

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.LibraryData
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.ui.Task
import com.gitlab.ykrasik.gamedex.util.logger
import javafx.collections.ObservableList
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.run
import tornadofx.observable
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 25/12/2016
 * Time: 19:47
 */
@Singleton
class LibraryRepository @Inject constructor(private val persistenceService: PersistenceService) {
    private val log = logger()

    val libraries: ObservableList<Library> = fetchAllLibraries()

    private fun fetchAllLibraries(): ObservableList<Library> {
        log.info("Fetching libraries...")
        val libraries = persistenceService.fetchAllLibraries()
        log.info("Fetched ${libraries.size} libraries.")
        return libraries.observable()
    }

    suspend fun add(request: AddLibraryRequest) = run(CommonPool) {
        val library = persistenceService.insertLibrary(request.path, request.data)
        run(JavaFx) {
            libraries += library
        }
        library
    }

    suspend fun addAll(requests: List<AddLibraryRequest>): List<Library> = run(CommonPool) {
        val libraries = requests.map { request ->
            persistenceService.insertLibrary(request.path, request.data)
        }
        run(JavaFx) {
            this.libraries += libraries
        }
        libraries
    }

    suspend fun update(library: Library): Library = run(JavaFx) {
        run(CommonPool) {
            persistenceService.updateLibrary(library)
        }

        removeById(library.id)
        libraries += library
        library
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

    suspend fun deleteAll(libraries: List<Library>, progress: Task.Progress) = run(CommonPool) {
        val deleted = AtomicInteger(0)

        progress.message = "Deleting ${libraries.size} libraries..."
        libraries.map { library ->
            async(CommonPool) {
                persistenceService.deleteLibrary(library.id)
                progress.progress(deleted.incrementAndGet(), libraries.size)
            }
        }.forEach { it.await() }
        progress.message = "Deleted ${libraries.size} libraries."

        run(JavaFx) {
            progress.message = "Updating UI..."
            this.libraries.setAll(this.libraries.filterNot { library -> libraries.any { it.id == library.id } }.observable())
        }
    }

    suspend fun invalidate() = run(JavaFx) {
        // Re-fetch all libraries from persistence
        libraries.setAll(fetchAllLibraries())
    }

    operator fun get(id: Int): Library = libraries.find { it.id == id } ?: throw IllegalStateException("No library found for id: $id!")

    private fun removeById(id: Int) {
        check(libraries.removeIf { it.id == id }) { "Error! Doesn't exist: Library($id)" }
    }
}

data class AddLibraryRequest(
    val path: File,
    val data: LibraryData
)