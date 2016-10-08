package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.datamodel.persistence.Genre
import com.gitlab.ykrasik.gamedex.persistence.entity.Genres
import com.gitlab.ykrasik.gamedex.persistence.entity.selectBy
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 02/10/2016
 * Time: 15:26
 */
interface GenreDao {
    val all: List<Genre>
    fun add(name: String): Genre
    fun getByName(name: String): Genre?
    fun getOrAdd(name: String): Genre
    fun deleteByIds(ids: List<Int>): Int
}

@Singleton
class GenreDaoImpl @Inject constructor() : GenreDao {
    private val log by logger()

    override val all: List<Genre> get() {
        log.info { "Fetching all..." }
        val genres = transaction {
            Genres.selectAll().map { it.toGenre() }
        }
        log.info { "Fetched ${genres.size}." }
        return genres
    }

    override fun add(name: String): Genre {
        log.info { "Inserting: '$name'..." }
        val id = transaction {
            Genres.insert {
                it[Genres.name] = name
            } get Genres.id
        }
        val genre = Genre(id, name)
        log.info { "Inserted: $genre." }
        return genre
    }

    override fun getByName(name: String): Genre? {
        log.debug { "Fetching: '$name'..."}
        val genre = transaction {
            Genres.selectBy { it.name.eq(name) }.firstOrNull()?.toGenre()
        }
        if (genre != null) {
            log.debug { "Found: $genre." }
        } else {
            log.debug { "Not found: '$name'." }
        }
        return genre
    }

    override fun getOrAdd(name: String): Genre = transaction {
        getByName(name) ?: add(name)
    }

    override fun deleteByIds(ids: List<Int>): Int {
        log.info { "Deleting: ids=$ids..." }
        val amount = transaction {
            Genres.deleteWhere { Genres.id.inList(ids) }
        }
        log.info { "Deleted $amount." }
        return amount
    }
}

object GenreMapper {
    operator fun invoke(row: ResultRow) = Genre(
        id = row[Genres.id],
        name = row[Genres.name]
    )
}
inline fun ResultRow.toGenre() = GenreMapper(this)