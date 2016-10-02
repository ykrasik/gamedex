package com.github.ykrasik.gamedex.persistence

import com.github.ykrasik.gamedex.common.debug
import com.github.ykrasik.gamedex.common.info
import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.datamodel.GamePlatform
import com.github.ykrasik.gamedex.datamodel.ImageData
import com.github.ykrasik.gamedex.datamodel.persistence.ExcludedPath
import com.github.ykrasik.gamedex.datamodel.persistence.Game
import com.github.ykrasik.gamedex.datamodel.persistence.Genre
import com.github.ykrasik.gamedex.datamodel.persistence.Library
import com.github.ykrasik.gamedex.datamodel.provider.GameData
import com.github.ykrasik.gamedex.persistence.config.PersistenceConfig
import com.github.ykrasik.gamedex.persistence.entity.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.joda.time.DateTime
import java.math.BigDecimal
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Yevgeny Krasik
 */
@Singleton
class PersistenceServiceImpl @Inject constructor(private val config: PersistenceConfig) : PersistenceService {
    private val log by logger()

    init {
        log.debug { "Connection url: ${config.dbUrl}" }
        Database.connect(config.dbUrl, driver = "org.h2.Driver", user = "sa")

        // TODO: Use Db evolutions!!!
        log.debug("Creating db...")
        create(Libraries, Games, Genres, GameGenres, ExcludedPaths)
    }

    inner class GamesImpl : PersistenceService.Games {
        private val baseQuery = (Games innerJoin Libraries).slice(Games.withoutBlobs + Libraries.columns)

        override val all: List<Game> get() {
            val genres = gameGenres.oneToManyContext()
            log.info { "Fetching all games..." }
            return baseQuery.selectAll().map {
                val library = it.toLibrary()
                it.toGame(genres, library)
            }
        }

        override fun get(id: Int): Game {
            val genres = gameGenres.oneToManyContext(id)
            log.info { "Fetching game: $id..." }
            val row = baseQuery.select { Games.id.eq(id) }.first()
            val library = row.toLibrary()
            return row.toGame(genres, library)
        }

        override fun contains(path: Path): Boolean = !Games.selectBy { it.path.eq(path.toString()) }.empty()

        override fun add(gameData: GameData, path: Path, library: Library): Game {
            log.debug { "Inserting game: $gameData..." }
            val lastModified = DateTime.now()
            val id = Games.insert {
                it[Games.path] = path.toString()
                it[Games.name] = gameData.name
                it[Games.releaseDate] = gameData.releaseDate?.toDateTimeAtStartOfDay()
                it[Games.description] = gameData.description
                it[Games.criticScore] = gameData.criticScore?.let { BigDecimal(it) }
                it[Games.userScore] = gameData.userScore?.let { BigDecimal(it) }
                it[Games.metacriticUrl] = gameData.metacriticUrl
                it[Games.giantBombUrl] = gameData.giantBombUrl
                it[Games.thumbnail] = gameData.thumbnail?.rawData?.let { it.toBlob() }
                it[Games.poster] = gameData.poster?.rawData?.let { it.toBlob() }
                it[Games.lastModified] = lastModified
                it[Games.library] = library.id
            } get Games.id

            // Insert all new genres.
            // TODO: Room for optimization - do one inList bulk fetch for existing genres.
            val genres = gameData.genres.map { genres.getOrAdd(it) }

            // Link genres to game.
            gameGenres.add(id, genres)

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
            log.info { "Deleting gameId=$id..." }
            require(Games.deleteWhere { Games.id.eq(id) } == 1) { "Failed to delete gameId=$id!" }

            val genres = gameGenres.oneToManyContext(id)[id]
            gameGenres.deleteByGame(id)

            // Delete any genres which were only linked to this game.
            val genresToDelete = genres.mapNotNull { genre ->
                if (gameGenres.isGenreLinked(genre.id)) {
                    null
                } else {
                    log.debug { "GenreId=${genre.id} was only linked to gameId=$id, deleting..." }
                    genre.id
                }
            }
            if (!genresToDelete.isEmpty()) {
                this@PersistenceServiceImpl.genres.deleteByIds(genresToDelete)
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

        fun getByLibrary(libraryId: Int): List<Int> {
            log.info { "Fetching games of library: $libraryId" }
            val ids = Games.select { Games.library.eq(libraryId) }.map {
                it[Games.id]
            }
            log.info { "Fetched ${ids.size} games." }
            return ids
        }

        fun deleteByLibrary(libraryId: Int) {
            log.info { "Deleting games by library: $libraryId" }
            val amount = Games.deleteWhere { Games.library.eq(libraryId) }
            log.info { "Deleted $amount games." }
        }
    }

    inner class GenresImpl : PersistenceService.Genres {
        override val all: List<Genre> get() = Genres.selectAll().map { it.toGenre() }

        fun add(name: String): Genre {
            log.info { "Inserting Genre: $name" }
            val id = Genres.insert {
                it[Genres.name] = name
            } get Genres.id
            val genre = Genre(id, name)
            log.info { "Inserted: $genre" }
            return genre
        }

        fun getByName(name: String): Genre? = Genres.selectBy { it.name.eq(name) }.firstOrNull()?.toGenre()

        fun getOrAdd(name: String): Genre = getByName(name) ?: add(name)

        fun deleteByIds(ids: List<Int>) {
            log.info { "Deleting genres: $ids" }
            val amount = Genres.deleteWhere { Genres.id.inList(ids) }
            log.info { "Deleted $amount genres." }
        }
    }

    private val gameGenres = object {
        fun add(gameId: Int, genres: List<Genre>) {
            GameGenres.batchInsert(genres, false) { genre ->
                log.debug { "Linking gameId=$gameId with genreId=${genre.id}..." }
                set(GameGenres.game, gameId)
                set(GameGenres.genre, genre.id)
            }
        }
//
//        fun add(gameId: Int, genre: Genre) {
//            log.debug { "Linking gameId=$gameId with genreId=${genre.id}..." }
//            GameGenres.insert {
//                it[GameGenres.game] = EntityID(gameId, Games)
//                it[GameGenres.genre] = EntityID(genre.id, Genres)
//            }
//        }

        fun deleteByGame(gameId: Int) {
            log.debug { "Un-linking all genres from gameId=$gameId..." }
            val amount = GameGenres.deleteWhere { GameGenres.game.eq(gameId) }
            log.info { "Un-linked $amount genres." }
        }

        fun isGenreLinked(genreId: Int): Boolean = !GameGenres.select { GameGenres.genre.eq(genreId) }.empty()

        fun oneToManyContext(id: Int = 0): OneToManyContext<Genre> {
            val join = GameGenres innerJoin Genres
            val query = if (id == 0) {
                log.info { "Fetching all gameGenres..." }
                join.selectAll()
            } else {
                log.info { "Fetching gameGenres for gameId=$id..." }
                join.select { GameGenres.game.eq(id) }
            }
            val mapping = query.groupBy(keySelector = { it[GameGenres.game] }, valueTransform = { it.toGenre() })
            return OneToManyContext(mapping)
        }
    }

    inner class LibrariesImpl : PersistenceService.Libraries {
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
            val gameIds = games.getByLibrary(id)

            // TODO: onDelete Cascade does the exact same thing
            games.deleteByLibrary(id)

            log.info { "Deleting library: $id" }
            require(Libraries.deleteWhere { Libraries.id.eq(id) } == 1) { "Library doesn't exist: $id" }
            log.info { "Deleted." }
            return gameIds
        }
    }

    inner class ExcludedPathsImpl : PersistenceService.ExcludedPaths {
        override val all: List<ExcludedPath> get() {
            log.info { "Fetching all excludedPaths..." }
            return ExcludedPaths.selectAll().map { it.toExcludedPath() }
        }

        override fun contains(path: Path) = !ExcludedPaths.selectBy { it.path.eq(path.toString()) }.empty()

        override fun add(path: Path): ExcludedPath {
            log.info { "Inserting excludedPath: path=$path" }
            val id = ExcludedPaths.insert {
                it[ExcludedPaths.path] = path.toString()
            } get ExcludedPaths.id
            val excludedPath = ExcludedPath(id, path)
            log.info { "Inserted: $excludedPath" }
            return excludedPath
        }

        override fun delete(id: Int) {
            log.info { "Deleting excludedPath: id=$id..." }
            require(ExcludedPaths.deleteWhere { ExcludedPaths.id.eq(id) } == 1) { "ExcludedPath doesn't exist: $id" }
            log.info { "Deleted." }
        }
    }

    override val games = GamesImpl()
    override val genres = GenresImpl()
    override val libraries = LibrariesImpl()
    override val excludedPaths = ExcludedPathsImpl()
}
