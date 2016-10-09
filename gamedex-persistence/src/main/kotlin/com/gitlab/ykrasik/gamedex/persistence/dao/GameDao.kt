package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.common.*
import com.github.ykrasik.gamedex.datamodel.Game
import com.github.ykrasik.gamedex.datamodel.Genre
import com.github.ykrasik.gamedex.datamodel.Library
import com.github.ykrasik.gamedex.datamodel.provider.GameData
import com.gitlab.ykrasik.gamedex.persistence.entity.Games
import com.gitlab.ykrasik.gamedex.persistence.entity.Libraries
import com.gitlab.ykrasik.gamedex.persistence.entity.selectBy
import com.gitlab.ykrasik.gamedex.persistence.entity.toBlob
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 02/10/2016
 * Time: 15:06
 */
interface GameDao {
    val all: List<Game>
    fun get(id: Id<Game>): Game     // TODO: Under which circumstance is this needed?
    fun exists(path: Path): Boolean
    fun add(gameData: GameData, path: Path, library: Library): Game
    fun delete(game: Game)

//    fun getThumbnail(id: Int): ImageData?
//    fun getPoster(id: Int): ImageData?
}

@Singleton
class GameDaoImpl @Inject constructor(
    private val genreDao: GenreDao,
    private val timeProvider: TimeProvider
) : GameDao {
    private val log by logger()

    private val baseQuery = (Games innerJoin Libraries).slice(Games.withoutBlobs + Libraries.columns)

    override val all: List<Game> get() {
        log.info { "Fetching all..." }
        val games = transaction {
            val genres = genreDao.linkedGames.all
            baseQuery.selectAll().map { it.toGame(genres) }
        }
        log.info { "Result: ${games.size} games." }
        return games
    }

    override fun get(id: Id<Game>): Game {
        log.info { "Fetching: id=$id..." }
        val game = transaction {
            val genres = genreDao.linkedGames.getByGame(id)
            val row = baseQuery.select { Games.id.eq(id.id) }.firstOrNull()
            requireNotNull(row) { "Game doesn't exist: $id!" }.let { it.toGame(genres) }
        }
        log.info { "Result: $game." }
        return game
    }

    override fun exists(path: Path): Boolean {
        log.debug { "Checking if exists: '$path'..." }
        val exists = transaction {
            !Games.selectBy { it.path.eq(path.toString()) }.empty()
        }
        log.debug { "Result: $exists." }
        return exists
    }

    override fun add(gameData: GameData, path: Path, library: Library): Game {
        log.info { "Inserting game: $gameData..." }
        val lastModified = timeProvider.now()
        val id = transaction {
            Games.insert {
                it[Games.path] = path.toString()
                it[name] = gameData.name
                it[releaseDate] = gameData.releaseDate?.toDateTimeAtStartOfDay()
                it[description] = gameData.description
                it[criticScore] = gameData.criticScore?.let(::BigDecimal)
                it[userScore] = gameData.userScore?.let(::BigDecimal)
                it[metacriticUrl] = gameData.metacriticUrl
                it[giantBombUrl] = gameData.giantBombUrl
                it[thumbnail] = gameData.thumbnail?.rawData?.let { it.toBlob() }
                it[poster] = gameData.poster?.rawData?.let { it.toBlob() }
                it[Games.lastModified] = lastModified
                it[Games.library] = library.id.id
            } get Games.id
        }.toId<Game>()

        // Insert all new genres.
        // TODO: Room for optimization - do one inList bulk fetch for existing genres.
        // FIXME: Is adding genres part of the responsibilities of this DAO? that's probably more in the Manager domain.
        val genres = gameData.genres.map { genreDao.getOrAdd(it) }

        // Link genres to game.
        genreDao.linkedGames.link(id, genres)

        val game = Game(
            id = id,
            path = path,
            name = gameData.name,
            description = gameData.description,
            releaseDate = gameData.releaseDate,
            criticScore = gameData.criticScore,
            userScore = gameData.userScore,
            lastModified = lastModified,
            metacriticUrl = gameData.metacriticUrl,
            giantBombUrl = gameData.giantBombUrl,
            genres = genres,
            library = library
        )
        log.info { "Result: $game." }
        return game
    }

    override fun delete(game: Game) {
        log.info { "Deleting: $game..." }
        transaction {
            val genres = genreDao.linkedGames.getByGame(game.id)
            genreDao.linkedGames.unlinkAll(game)

            // Delete any genres which were only linked to this game.
            // FIXME: Why? Genres don't take up much space, and it would be easier to just periodically run cleaning.
            val genresToDelete = genres.mapNotNull { genre ->
                if (genreDao.linkedGames.isGenreLinkedToAnyGame(genre)) {
                    null
                } else {
                    log.debug { "$genre was only linked to $game, deleting..." }
                    genre
                }
            }
            if (!genresToDelete.isEmpty()) {
                genreDao.delete(genresToDelete)
            }

            val amount = Games.deleteWhere { Games.id.eq(game.id.id) }
            require(amount == 1) { "Game doesn't exist: $game" }

        }
        log.info { "Done." }
    }

    private fun ResultRow.toGame(genres: Map<Id<Game>, List<Genre>>): Game {
        val id = this[Games.id].toId<Game>()
        return toGame(genres[id] ?: emptyList())
    }

    private fun ResultRow.toGame( genres: List<Genre>): Game {
        val library = this.toLibrary()
        return Game(
            id = this[Games.id].toId(),
            path = this[Games.path].toPath(),
            name = this[Games.name],
            description = this[Games.description],
            releaseDate = this[Games.releaseDate]?.toLocalDate(),
            criticScore = this[Games.criticScore]?.toDouble(),
            userScore = this[Games.userScore]?.toDouble(),
            lastModified = this[Games.lastModified],
            metacriticUrl = this[Games.metacriticUrl],
            giantBombUrl = this[Games.giantBombUrl],
            genres = genres,
            library = library
        )
    }

//    override fun getThumbnail(id: Int): ImageData? {
//        val result = Games.slice(Games.thumbnail).select { Games.id.eq(id) }.first()
//        return result[Games.thumbnail]?.let { ImageData(it.bytes) }
//    }
//
//    override fun getPoster(id: Int): ImageData? {
//        val result = Games.slice(Games.poster).select { Games.id.eq(id) }.first()
//        return result[Games.poster]?.let { ImageData(it.bytes) }
//    }

//    override fun getByLibrary(libraryId: Int): List<Int> {
//        log.info { "Fetching games of library: $libraryId" }
//        val ids = Games.select { Games.library.eq(libraryId) }.map {
//            it[Games.id]
//        }
//        log.info { "Fetched ${ids.size} games." }
//        return ids
//    }
}