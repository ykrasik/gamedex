package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.common.Id
import com.github.ykrasik.gamedex.common.KLogger
import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.common.toId
import com.github.ykrasik.gamedex.datamodel.Game
import com.github.ykrasik.gamedex.datamodel.Genre
import com.gitlab.ykrasik.gamedex.persistence.entity.GameGenres
import com.gitlab.ykrasik.gamedex.persistence.entity.Genres
import com.gitlab.ykrasik.gamedex.persistence.entity.selectBy
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
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

    fun get(name: String): Genre?
    fun getOrAdd(name: String): Genre

    fun delete(genres: List<Genre>): Int

    val linkedGames: GameGenreDao

    // TODO: This looks like it should just be a part of GenreDao
    interface GameGenreDao {
        val all: Map<Id<Game>, List<Genre>>

        // TODO: This looks like it should belong to GenreDao
        fun getByGame(id: Id<Game>): List<Genre>

        fun isGenreLinkedToAnyGame(genre: Genre): Boolean

        fun link(id: Id<Game>, genres: List<Genre>)
        fun unlinkAll(game: Game): Int
    }
}

@Singleton
class GenreDaoImpl @Inject constructor() : GenreDao {
    private val log by logger()

    override val all: List<Genre> get() {
        log.info { "Fetching all..." }
        val genres = transaction {
            Genres.selectAll().map { it.toGenre() }
        }
        log.info { "Result: ${genres.size} genres." }
        return genres
    }

    override fun add(name: String): Genre {
        log.info { "Inserting: '$name'..." }
        val id = transaction {
            Genres.insert {
                it[Genres.name] = name
            } get Genres.id
        }
        val genre = Genre(id.toId(), name)
        log.info { "Result: $genre." }
        return genre
    }

    override fun get(name: String): Genre? {
        log.debug { "Fetching: '$name'..."}
        val genre = transaction {
            Genres.selectBy { it.name.eq(name) }.firstOrNull()?.toGenre()
        }
        log.debug { "Result: $genre." }
        return genre
    }

    override fun getOrAdd(name: String): Genre = transaction {
        get(name) ?: add(name)
    }

    override fun delete(genres: List<Genre>): Int {
        log.info { "Deleting: $genres..." }
        val idsToDelete = genres.map { it.id.id }
        val amount = transaction {
            Genres.deleteWhere { Genres.id.inList(idsToDelete) }
        }
        log.info { "Done: $amount deleted." }
        return amount
    }

    private fun ResultRow.toGenre() = Genre(
        id = this[Genres.id].toId(),
        name = this[Genres.name]
    )

    override val linkedGames = object : GenreDao.GameGenreDao {
        private val log = KLogger("com.gitlab.ykrasik.gamedex.persistence.dao.GameGenreDao")

        override val all: Map<Id<Game>, List<Genre>> get() {
            log.info { "Fetching all..." }
            val genres = transaction {
                (GameGenres innerJoin Genres).selectAll().groupBy(
                    keySelector = { it[GameGenres.game].toId<Game>() },
                    valueTransform = { it.toGenre() }
                )
            }
            log.info { "Result: ${genres.size} game-genre links." }
            return genres
        }

        override fun getByGame(id: Id<Game>): List<Genre> {
            log.info { "Fetching for gameId=$id..." }
            val genres = transaction {
                (GameGenres innerJoin Genres).select { GameGenres.game.eq(id.id) }.map { it.toGenre() }
            }
            log.info { "Result: $genres." }
            return genres
        }

        override fun isGenreLinkedToAnyGame(genre: Genre): Boolean {
            log.debug { "Checking if genre is linked to any game: $genre..." }
            val linked = transaction {
                !GameGenres.select { GameGenres.genre.eq(genre.id.id) }.empty()
            }
            log.debug { "Result: $linked." }
            return linked
        }

        override fun link(id: Id<Game>, genres: List<Genre>) {
            log.info { "Linking gameId=$id with genres: $genres" }
            transaction {
                for (genre in genres) {
                    log.debug { "Linking gameId=$id with genre=$genre..." }
                    GameGenres.insert {
                        it[GameGenres.game] = id.id
                        it[GameGenres.genre] = genre.id.id
                    }
                }
                // TODO: Batch insert seems to not work, or I just don't understand how to use it.
//                GameGenres.batchInsert(genres) { genre ->
//                    log.debug { "Linking gameId=$id with genre=$genre..." }
//                    set(GameGenres.game, id.id)
//                    set(GameGenres.genre, genre.id.id)
//                }
            }
            log.info { "Done." }
        }

        override fun unlinkAll(game: Game): Int {
            log.info { "Un-linking all genres from game=$game..." }
            val amount = transaction {
                GameGenres.deleteWhere { GameGenres.game.eq(game.id.id) }
            }
            log.info { "Result: $amount un-linked." }
            return amount
        }

    }
}