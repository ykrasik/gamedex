package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.common.d
import com.github.ykrasik.gamedex.common.i
import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.datamodel.ImageData
import com.github.ykrasik.gamedex.datamodel.persistence.Game
import com.github.ykrasik.gamedex.datamodel.persistence.Library
import com.github.ykrasik.gamedex.datamodel.provider.GameData
import com.gitlab.ykrasik.gamedex.persistence.entity.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.joda.time.DateTime
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
    operator fun get(id: Int): Game
    operator fun contains(path: Path): Boolean
    fun add(gameData: GameData, path: Path, library: Library): Game
    fun delete(id: Int)

    fun getThumbnail(id: Int): ImageData?
    fun getPoster(id: Int): ImageData?

    fun getByLibrary(libraryId: Int): List<Int>
    fun deleteByLibrary(libraryId: Int)
}

@Singleton
class GameDaoImpl @Inject constructor(
    private val gameGenreDao: GameGenreDao,
    private val genreDao: GenreDao
) : GameDao {
    private val log by logger()

    private val baseQuery = (Games innerJoin Libraries).slice(Games.withoutBlobs + Libraries.columns)

    override val all: List<Game> get() {
        val genres = gameGenreDao.oneToManyContext()
        log.i { "Fetching all games..." }
        return baseQuery.selectAll().map {
            val library = it.toLibrary()
            it.toGame(genres, library)
        }
    }

    override fun get(id: Int): Game {
        val genres = gameGenreDao.oneToManyContext(id)
        log.i { "Fetching game: $id..." }
        val row = baseQuery.select { Games.id.eq(id) }.first()
        val library = row.toLibrary()
        return row.toGame(genres, library)
    }

    override fun contains(path: Path) = !Games.selectBy { it.path.eq(path.toString()) }.empty()

    override fun add(gameData: GameData, path: Path, library: Library): Game {
        log.d { "Inserting game: $gameData..." }
        val lastModified = DateTime.now()
        val id = Games.insert {
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
            it[Games.library] = library.id
        } get Games.id

        // Insert all new genres.
        // TODO: Room for optimization - do one inList bulk fetch for existing genres.
        val genres = gameData.genres.map { genreDao.getOrAdd(it) }

        // Link genres to game.
        gameGenreDao.add(id, genres)

        return Game(
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
    }

    override fun delete(id: Int) {
        log.i { "Deleting gameId=$id..." }
        require(Games.deleteWhere { Games.id.eq(id) } == 1) { "Failed to delete gameId=$id!" }

        val genres = gameGenreDao.oneToManyContext(id)[id]
        gameGenreDao.deleteByGame(id)

        // Delete any genres which were only linked to this game.
        val genresToDelete = genres.mapNotNull { genre ->
            if (gameGenreDao.contains(genre.id)) {
                null
            } else {
                log.d { "GenreId=${genre.id} was only linked to gameId=$id, deleting..." }
                genre.id
            }
        }
        if (!genresToDelete.isEmpty()) {
            genreDao.deleteByIds(genresToDelete)
        }
    }

    override fun getThumbnail(id: Int): ImageData? {
        val result = Games.slice(Games.thumbnail).select { Games.id.eq(id) }.first()
        return result[Games.thumbnail]?.let { ImageData(it.bytes) }
    }

    override fun getPoster(id: Int): ImageData? {
        val result = Games.slice(Games.poster).select { Games.id.eq(id) }.first()
        return result[Games.poster]?.let { ImageData(it.bytes) }
    }

    override fun getByLibrary(libraryId: Int): List<Int> {
        log.i { "Fetching games of library: $libraryId" }
        val ids = Games.select { Games.library.eq(libraryId) }.map {
            it[Games.id]
        }
        log.i { "Fetched ${ids.size} games." }
        return ids
    }

    override fun deleteByLibrary(libraryId: Int) {
        log.i { "Deleting games by library: $libraryId" }
        val amount = Games.deleteWhere { Games.library.eq(libraryId) }
        log.i { "Deleted $amount games." }
    }
}