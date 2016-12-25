package com.gitlab.ykrasik.gamedex.core.ui.model

import com.github.ykrasik.gamedex.datamodel.Library
import com.github.ykrasik.gamedex.datamodel.LibraryData
import com.gitlab.ykrasik.gamedex.persistence.dao.LibraryDao
import javafx.beans.property.SimpleListProperty
import javafx.collections.ObservableList
import tornadofx.getValue
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
class LibrariesModel @Inject constructor(
    private val libraryDao: LibraryDao
) {
    val allProperty = SimpleListProperty(libraryDao.all.observable())
    val all: ObservableList<Library> by allProperty

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