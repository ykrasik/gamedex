package com.gitlab.ykrasik.gamedex.core.ui.model

import com.github.ykrasik.gamedex.datamodel.Library
import com.github.ykrasik.gamedex.datamodel.LibraryData
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import javafx.beans.property.ListProperty
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
    private val persistenceService: PersistenceService
) {
    val librariesProperty: ListProperty<Library> = SimpleListProperty(persistenceService.libraries.all.observable())
    val libraries: ObservableList<Library> by librariesProperty

    fun contains(path: File): Boolean = libraries.any { it.path == path }

    fun add(libraryData: LibraryData) {
        val library = persistenceService.libraries.add(libraryData)
        libraries += library
    }

    fun delete(library: Library) {
        persistenceService.libraries.delete(library)
        check(libraries.remove(library)) { "Error! Didn't contain library: $library" }
    }
}