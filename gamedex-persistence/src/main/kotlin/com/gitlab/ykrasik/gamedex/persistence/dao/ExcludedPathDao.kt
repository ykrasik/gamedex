package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.common.i
import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.datamodel.persistence.ExcludedPath
import com.gitlab.ykrasik.gamedex.persistence.entity.ExcludedPaths
import com.gitlab.ykrasik.gamedex.persistence.entity.selectBy
import com.gitlab.ykrasik.gamedex.persistence.entity.toExcludedPath
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 02/10/2016
 * Time: 15:37
 */
interface ExcludedPathDao {
    val all: List<ExcludedPath>
    operator fun contains(path: Path): Boolean
    fun add(path: Path): ExcludedPath
    fun delete(id: Int)
}

@Singleton
class ExcludedPathDaoImpl @Inject constructor() : ExcludedPathDao {
    private val log by logger()

    override val all: List<ExcludedPath> get() {
        log.i { "Fetching all excludedPaths..." }
        return ExcludedPaths.selectAll().map { it.toExcludedPath() }
    }

    override fun contains(path: Path) = !ExcludedPaths.selectBy { it.path.eq(path.toString()) }.empty()

    override fun add(path: Path): ExcludedPath {
        log.i { "Inserting excludedPath: path=$path" }
        val id = ExcludedPaths.insert {
            it[ExcludedPaths.path] = path.toString()
        } get ExcludedPaths.id
        val excludedPath = ExcludedPath(id, path)
        log.i { "Inserted: $excludedPath" }
        return excludedPath
    }

    override fun delete(id: Int) {
        log.i { "Deleting excludedPath: id=$id..." }
        require(ExcludedPaths.deleteWhere { ExcludedPaths.id.eq(id) } == 1) { "ExcludedPath doesn't exist: $id" }
        log.i { "Deleted." }
    }
}