package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.datamodel.persistence.ExcludedPath
import com.gitlab.ykrasik.gamedex.persistence.entity.ExcludedPaths
import com.gitlab.ykrasik.gamedex.persistence.entity.selectBy
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 02/10/2016
 * Time: 15:37
 */
interface ExcludedPathDao {
    val all: List<ExcludedPath>
    fun contains(path: Path): Boolean
    fun add(path: Path): ExcludedPath
    fun delete(id: Int)
}

@Singleton
class ExcludedPathDaoImpl @Inject constructor() : ExcludedPathDao {
    private val log by logger()

    override val all: List<ExcludedPath> get() {
        log.info { "Fetching all..." }
        val excludedPaths = transaction {
            ExcludedPaths.selectAll().map { Mapper(it) }
        }
        log.info { "Fetched ${excludedPaths.size}." }
        return excludedPaths
    }

    override fun contains(path: Path): Boolean {
        log.debug { "Checking if exists: '$path'..." }
        val contains = transaction {
            !ExcludedPaths.selectBy { it.path.eq(path.toString()) }.empty()
        }
        log.debug { "Exists: $contains."}
        return contains
    }

    override fun add(path: Path): ExcludedPath {
        log.info { "Inserting: '$path'..." }
        val id = transaction {
            ExcludedPaths.insert {
                it[ExcludedPaths.path] = path.toString()
            } get ExcludedPaths.id
        }
        val excludedPath = ExcludedPath(id, path)
        log.info { "Inserted: $excludedPath." }
        return excludedPath
    }

    override fun delete(id: Int) {
        log.info { "Deleting: id=$id..." }
        val amount = transaction {
            ExcludedPaths.deleteWhere { ExcludedPaths.id.eq(id) }
        }
        require(amount == 1) { "ExcludedPath doesn't exist: $id" }
        log.info { "Deleted." }
    }

    private object Mapper {
        operator fun invoke(row: ResultRow) = ExcludedPath(
            id = row[ExcludedPaths.id],
            path = Paths.get(row[ExcludedPaths.path])
        )
    }
}