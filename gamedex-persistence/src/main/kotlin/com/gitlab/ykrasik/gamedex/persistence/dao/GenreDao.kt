package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.common.i
import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.datamodel.persistence.Genre
import com.gitlab.ykrasik.gamedex.persistence.entity.Genres
import com.gitlab.ykrasik.gamedex.persistence.entity.selectBy
import com.gitlab.ykrasik.gamedex.persistence.entity.toGenre
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
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
    fun getOrAdd(name: String): Genre = getByName(name) ?: add(name)
    fun deleteByIds(ids: List<Int>)
}

@Singleton
class GenreDaoImpl @Inject constructor() : GenreDao {
    private val log by logger()

    override val all: List<Genre> get() = Genres.selectAll().map { it.toGenre() }

    override fun add(name: String): Genre {
        log.i { "Inserting Genre: $name" }
        val id = Genres.insert {
            it[Genres.name] = name
        } get Genres.id
        val genre = Genre(id, name)
        log.i { "Inserted: $genre" }
        return genre
    }

    override fun getByName(name: String): Genre? = Genres.selectBy { it.name.eq(name) }.firstOrNull()?.toGenre()

    override fun deleteByIds(ids: List<Int>) {
        log.i { "Deleting genres: $ids" }
        val amount = Genres.deleteWhere { Genres.id.inList(ids) }
        log.i { "Deleted $amount genres." }
    }
}