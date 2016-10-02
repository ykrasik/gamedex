package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.common.d
import com.github.ykrasik.gamedex.common.i
import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.datamodel.persistence.Genre
import com.gitlab.ykrasik.gamedex.persistence.entity.GameGenres
import com.gitlab.ykrasik.gamedex.persistence.entity.Genres
import com.gitlab.ykrasik.gamedex.persistence.entity.OneToManyContext
import com.gitlab.ykrasik.gamedex.persistence.entity.toGenre
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 02/10/2016
 * Time: 15:13
 */
interface GameGenreDao {
    fun add(gameId: Int, genres: List<Genre>)
    fun deleteByGame(gameId: Int)
    operator fun contains(genreId: Int): Boolean
    fun oneToManyContext(id: Int? = null): OneToManyContext<Genre>
}

@Singleton
class GameGenreDaoImpl @Inject constructor() : GameGenreDao {
    private val log by logger()

    override fun add(gameId: Int, genres: List<Genre>) {
        GameGenres.batchInsert(genres, false) { genre ->
            log.d { "Linking gameId=$gameId with genreId=${genre.id}..." }
            set(GameGenres.game, gameId)
            set(GameGenres.genre, genre.id)
        }
    }

    override fun deleteByGame(gameId: Int) {
        log.d { "Un-linking all genres from gameId=$gameId..." }
        val amount = GameGenres.deleteWhere { GameGenres.game.eq(gameId) }
        log.i { "Un-linked $amount genres." }
    }

    override fun contains(genreId: Int) = !GameGenres.select { GameGenres.genre.eq(genreId) }.empty()

    override fun oneToManyContext(id: Int?): OneToManyContext<Genre> {
        val join = GameGenres innerJoin Genres
        val query = if (id == null) {
            log.i { "Fetching all gameGenres..." }
            join.selectAll()
        } else {
            log.i { "Fetching gameGenres for gameId=$id..." }
            join.select { GameGenres.game.eq(id) }
        }
        val mapping = query.groupBy(keySelector = { it[GameGenres.game] }, valueTransform = { it.toGenre() })
        return OneToManyContext(mapping)
    }
}