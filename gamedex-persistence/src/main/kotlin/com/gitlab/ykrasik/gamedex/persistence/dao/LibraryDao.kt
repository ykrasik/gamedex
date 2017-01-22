package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.common.jackson.fromJson
import com.github.ykrasik.gamedex.common.jackson.toJsonStr
import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.common.toFile
import com.github.ykrasik.gamedex.datamodel.Library
import com.gitlab.ykrasik.gamedex.persistence.AddLibraryRequest
import com.gitlab.ykrasik.gamedex.persistence.entity.Libraries
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * User: ykrasik
 * Date: 02/10/2016
 * Time: 15:33
 */
interface LibraryDao {
    val all: List<Library>

    fun add(request: AddLibraryRequest): Library

    fun delete(library: Library)
}

class LibraryDaoImpl : LibraryDao {
    private val log by logger()

    override val all: List<Library> get() {
        log.info { "Fetching all..." }
        val libraries = transaction {
            Libraries.selectAll().map {
                Library(
                    id = it[Libraries.id],
                    path = it[Libraries.path].toFile(),
                    data = it[Libraries.data].fromJson()
                )
            }
        }
        log.info { "Result: ${libraries.size} libraries." }
        return libraries
    }

    override fun add(request: AddLibraryRequest): Library {
        log.info { "Inserting: $request..." }
        val id = transaction {
            Libraries.insert {
                it[Libraries.path] = request.path.toString()
                it[Libraries.data] = request.data.toJsonStr()
            } get Libraries.id
        }
        val library = Library(id, request.path, request.data)
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

    private fun AddLibraryRequest.toLibrary(id: Int) = Library(id, path, data)
}