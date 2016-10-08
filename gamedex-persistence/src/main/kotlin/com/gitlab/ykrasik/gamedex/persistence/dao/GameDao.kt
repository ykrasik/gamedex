package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.common.Id
import com.github.ykrasik.gamedex.common.TimeProvider
import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.common.toId
import com.github.ykrasik.gamedex.datamodel.persistence.Game
import com.github.ykrasik.gamedex.datamodel.persistence.Library
import com.github.ykrasik.gamedex.datamodel.provider.GameData
import com.gitlab.ykrasik.gamedex.persistence.entity.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
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
    fun contains(path: Path): Boolean
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
        val genres = genreDao.linkedGames.all
        log.info { "Fetching all..." }
        val games = transaction {
            baseQuery.selectAll().map {
                val library = it.toLibrary()
                it.toGame(genres, library)
            }
        }
        log.info { "Fetched ${games.size}." }
        return games
    }

    override fun get(id: Id<Game>): Game {
        val genres = genreDao.linkedGames.getByGame(id)
        log.info { "Fetching game: $id..." }
        val row = baseQuery.select { Games.id.eq(id.id) }.first()
        val library = row.toLibrary()
        return GamesMapper.map(row, genres, library)
    }

    override fun contains(path: Path) = !Games.selectBy { it.path.eq(path.toString()) }.empty()

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
        log.info { "Inserted: $game." }
        return game
    }

    override fun delete(game: Game) {
        log.info { "Deleting: $game..." }
        val amount = transaction {
            Games.deleteWhere { Games.id.eq(game.id.id) }
        }
        require(amount == 1) { "Game doesn't exist: $game" }

        val genres = genreDao.linkedGames.getByGame(game.id)
        genreDao.linkedGames.unlinkAll(game)

        // Delete any genres which were only linked to this game.
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