package com.gitlab.ykrasik.gamedex.repository

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
            persistenceService.insertLibrary(request.path, request.data)
        }
        run(JavaFx) {
            libraries += library
        }
        return library
    }

    suspend fun delete(library: Library) {
        run(CommonPool) {
            persistenceService.deleteLibrary(library.id)
        }
        run(JavaFx) {
            check(libraries.remove(library)) { "Error! Library doesn't exist: $library" }
        }
        log.info("Done")
    }

    operator fun get(id: Int): Library = libraries.find { it.id == id } ?: throw IllegalStateException("No library found for id: $id!")
}

data class AddLibraryRequest(
    val path: File,
    val data: LibraryData
)