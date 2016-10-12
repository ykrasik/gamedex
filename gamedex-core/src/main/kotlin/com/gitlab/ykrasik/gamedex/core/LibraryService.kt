package com.gitlab.ykrasik.gamedex.core

import com.github.ykrasik.gamedex.datamodel.Library
import com.github.ykrasik.gamedex.datamodel.LibraryData
import com.gitlab.ykrasik.gamedex.persistence.dao.LibraryDao
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 11/10/2016
 * Time: 11:13
 */
// FIXME: Place this in the persistence module?
interface LibraryService {
    val all: List<Library>
    fun add(data: LibraryData): Library
    fun delete(library: Library)
}

@Singleton
class LibraryServiceImpl @Inject constructor(private val dao: LibraryDao): LibraryService {
    override val all: List<Library> get() = dao.all
    override fun add(data: LibraryData): Library = dao.add(data)
    override fun delete(library: Library) = dao.delete(library)
}