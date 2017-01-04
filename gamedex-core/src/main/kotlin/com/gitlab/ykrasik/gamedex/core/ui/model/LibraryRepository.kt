package com.gitlab.ykrasik.gamedex.core.ui.model

import com.github.ykrasik.gamedex.datamodel.Library
import com.github.ykrasik.gamedex.datamodel.LibraryData
import com.gitlab.ykrasik.gamedex.persistence.dao.LibraryDao
import javafx.beans.property.ListProperty
import javafx.beans.property.SimpleListProperty
import javafx.collections.ObservableList
import tornadofx.observable
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 25/12/2016
 * Time: 19:47
 */
@Singleton
class LibraryRepository @Inject constructor(
    private val libraryDao: LibraryDao
) {
    val all: ObservableList<Library> = libraryDao.all.observable()
    val allProperty: ListProperty<Library> = SimpleListProperty(libraryDao.all.observable())

    fun contains(path: Path): Boolean = all.any { it.path == path }

    fun add(libraryData: LibraryData) {
        val library = libraryDao.add(libraryData)
        all += library
    }

    fun delete(library: Library) {
        libraryDao.delete(library)
        check(all.remove(library)) { "Error! Didn't contain library: $library" }
    }
}