package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.common.Id
import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.common.toId
import com.github.ykrasik.gamedex.common.toPath
import com.github.ykrasik.gamedex.datamodel.GamePlatform
import com.github.ykrasik.gamedex.datamodel.Library
import com.gitlab.ykrasik.gamedex.persistence.entity.Libraries
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 02/10/2016
 * Time: 15:33
 */
interface LibraryDao {
    val all: List<Library>
    fun get(id: Id<Library>): Library  // TODO: Under which circumstances is this call needed?

    fun exists(path: Path): Boolean   // TODO: Under which circumstances is this call needed?

    fun add(path: Path, platform: GamePlatform, name: String): Library

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

    override fun get(id: Id<Library>): Library {
        log.info { "Fetching: id=$id..." }
        val library = transaction {
            val library = Libraries.select { Libraries.id.eq(id.id) }.firstOrNull()
            requireNotNull(library) { "Library doesn't exist: $id!" }.toLibrary()
        }
        log.info { "Result: $library." }
        return library
    }

    override fun exists(path: Path): Boolean {
        log.debug { "Checking if exists: '$path'..." }
        val exists = transaction {
            !Libraries.select { Libraries.path.eq(path.toString()) }.empty()
        }
        log.debug { "Result: $exists." }
        return exists
    }

    override fun add(path: Path, platform: GamePlatform, name: String): Library {
        log.info { "Inserting: path=$path, platform=$platform, name=$name..." }
        val id = transaction {
            Libraries.insert {
                it[Libraries.path] = path.toString()
                it[Libraries.platform] = platform
                it[Libraries.name] = name
            } get Libraries.id
        }
        val library = Library(id.toId(), path, platform, name)
        log.info { "Result: $library." }
        return library
    }

    override fun delete(library: Library) {
        log.info { "Deleting library: $library..." }
        val amount = transaction {
            Libraries.deleteWhere { Libraries.id.eq(library.id.id) }
        }
        require(amount == 1) { "Library doesn't exist: $library" }
        log.info { "Done." }
    }
}

// FIXME: Hide this somehow.
inline fun ResultRow.toLibrary(): Library = Library(
    id = this[Libraries.id].toId(),
    path = this[Libraries.path].toPath(),
    platform = this[Libraries.platform],
    name = this[Libraries.name]
)