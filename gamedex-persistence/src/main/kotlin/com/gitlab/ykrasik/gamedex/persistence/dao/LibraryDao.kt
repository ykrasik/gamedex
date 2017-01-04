package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.common.toPath
import com.github.ykrasik.gamedex.datamodel.Library
import com.github.ykrasik.gamedex.datamodel.LibraryData
import com.gitlab.ykrasik.gamedex.persistence.entity.Libraries
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 02/10/2016
 * Time: 15:33
 */
interface LibraryDao {
    val all: List<Library>

    fun add(data: LibraryData): Library

    fun delete(library: Library)
}

@Singleton
class LibraryDaoImpl @Inject constructor() : LibraryDao {
    private val log by logger()

    override val all: List<Library> get() {
        log.info { "Fetching all..." }
        val libraries = transaction {
            Libraries.selectAll().map { it.toLibrary() }
        }
        log.info { "Result: ${libraries.size} libraries." }
        return libraries
    }

    override fun add(data: LibraryData): Library {
        log.info { "Inserting: $data..." }
        val id = transaction {
            Libraries.insert {
                it[Libraries.path] = data.path.toString()
                it[Libraries.name] = data.name
                it[Libraries.platform] = data.platform
            } get Libraries.id
        }
        val library = Library(id, data)
        log.info { "Result: $library." }
        return library
    }

    override fun delete(library: Library) {
        log.info { "Deleting library: $library..." }
        val amount = transaction {
            Libraries.deleteWhere { Libraries.id.eq(library.id) }
        }
        require(amount == 1) { "Library doesn't exist: $library" }
        log.info { "Done." }
    }
}

// FIXME: Hide this somehow.
inline fun ResultRow.toLibrary(): Library = Library(
    id = this[Libraries.id],
    data = LibraryData(
        path = this[Libraries.path].toPath(),
        name = this[Libraries.name],
        platform = this[Libraries.platform]
    )
)