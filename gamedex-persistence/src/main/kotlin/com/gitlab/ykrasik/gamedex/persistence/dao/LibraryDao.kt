package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.datamodel.GamePlatform
import com.github.ykrasik.gamedex.datamodel.persistence.Library
import com.gitlab.ykrasik.gamedex.persistence.entity.Libraries
import com.gitlab.ykrasik.gamedex.persistence.entity.toLibrary
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
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
    operator fun get(id: Int): Library
    operator fun contains(path: Path): Boolean
    fun add(path: Path, platform: GamePlatform, name: String): Library
    fun delete(id: Int): List<Int>
}

@Singleton
class LibraryDaoImpl @Inject constructor(
    private val gameDao: GameDao
) : LibraryDao {
    private val log by logger()

    override val all: List<Library> get() = Libraries.selectAll().map { it.toLibrary() }

    override fun get(id: Int) = Libraries.select { Libraries.id.eq(id) }.first().toLibrary()

    override fun contains(path: Path) = !Libraries.select { Libraries.path.eq(path.toString()) }.empty()

    override fun add(path: Path, platform: GamePlatform, name: String): Library {
        log.info { "Inserting library: path=$path, platform=$platform, name=$name" }
        val id = Libraries.insert {
            it[Libraries.path] = path.toString()
            it[Libraries.platform] = platform
            it[Libraries.name] = name
        } get Libraries.id
        return Library(id, path, platform, name)
    }

    // Returns ids of games to be deleted.
    override fun delete(id: Int): List<Int> {
        val gameIds = gameDao.getByLibrary(id)

        // TODO: onDelete Cascade does the exact same thing
        gameDao.deleteByLibrary(id)

        log.info { "Deleting library: $id" }
        require(Libraries.deleteWhere { Libraries.id.eq(id) } == 1) { "Library doesn't exist: $id" }
        log.info { "Deleted." }
        return gameIds
    }
}