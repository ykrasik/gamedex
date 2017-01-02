package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.common.*
import com.github.ykrasik.gamedex.common.jackson.objectMapper
import com.github.ykrasik.gamedex.common.jackson.readList
import com.github.ykrasik.gamedex.datamodel.*
import com.gitlab.ykrasik.gamedex.persistence.entity.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.math.BigDecimal
import java.nio.file.Path
import java.sql.Blob
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

    fun add(gameData: GameDataDto, path: Path, library: Library): Game

    fun delete(game: Game)
    fun deleteByLibrary(library: Library)

//    fun getThumbnail(id: Int): ImageData?
//    fun getPoster(id: Int): ImageData?
}

@Singleton
class GameDaoImpl @Inject constructor(
    private val genreDao: GenreDao,
    private val timeProvider: TimeProvider
) : GameDao {
    private val log by logger()

    // TODO: Do not pre-fetch images, maybe only the thumbnail.
    private val baseQuery = (Games innerJoin Images innerJoin Libraries).slice(Games.columns + Libraries.columns + Images.columns)

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

    override fun add(gameData: GameDataDto, path: Path, library: Library): Game = transaction {
        val (id, lastModified) = insert(gameData, path, library)
        val genres = insertGenres(gameData.genresNames, id)
        val game = gameData.toGame(id, path, lastModified, library, genres)
        log.info { "Result: $game." }
        game
    }

    private fun insert(gameData: GameDataDto, path: Path, library: Library): Pair<Id<Game>, DateTime> {
        log.info { "Inserting game: $gameData..." }
        val lastModified = timeProvider.now()
        val id = insertGame(gameData, path, library, lastModified)
        insertImage(gameData.imageData, id)
        return Pair(id.toId<Game>(), lastModified)
    }

    private fun insertGame(gameData: GameDataDto, path: Path, library: Library, lastModified: DateTime): Int = Games.insert {
        it[Games.path] = path.toString()
        it[Games.lastModified] = lastModified
        it[Games.library] = library.id.id
        gameData.basicData.fillBasicData(it)
        gameData.scoreData.fillScoreData(it)
        it[providerData] = objectMapper.writeValueAsString(gameData.providerData)
    } get Games.id

    private fun GameBasicData.fillBasicData(table: InsertStatement<Number>) {
        table[Games.name] = name
        table[Games.releaseDate] = releaseDate?.toDateTimeAtStartOfDay()
        table[Games.description] = description
    }

    private fun GameScoreData.fillScoreData(table: InsertStatement<Number>) {
        table[Games.criticScore] = criticScore?.let(::BigDecimal)
        table[Games.userScore] = userScore?.let(::BigDecimal)
    }

    private fun insertImage(imageData: GameImageData, id: Int) = Images.insert {
        it[game] = id
        imageData.thumbnail.fillData(it, thumbnail, thumbnailUrl)
        imageData.poster.fillData(it, poster, posterUrl)
        // TODO: Probably don't need to pre-fetch poster. Add screenshots.
    }

    private fun ImageData?.fillData(table: InsertStatement<Number>, dataColumn: Column<Blob?>, urlColumn: Column<String?>) {
        table[dataColumn] = this?.rawData?.toBlob()
        table[urlColumn] = this?.url
    }

    private fun insertGenres(genreNames: List<String>, id: Id<Game>): List<Genre> {
        // Insert all new genres.
        // TODO: Room for optimization - do one inList bulk fetch for existing genres.
        // FIXME: Is adding genres part of the responsibilities of this DAO? that's probably more in the Manager domain.
        val genres = genreNames.map { genreDao.getOrAdd(it) }

        // Link genres to game.
        genreDao.linkedGames.link(id, genres)
        return genres
    }

    private fun GameDataDto.toGame(id: Id<Game>, path: Path, lastModified: DateTime, library: Library, genres: List<Genre>) = Game(
        id = id,
        path = path,
        lastModified = lastModified,
        library = library,
        data = GameData(
            basicData = basicData,
            scoreData = scoreData,
            imageData = imageData,
            genres = genres,
            providerData = providerData
        )
    )

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

    override fun deleteByLibrary(library: Library) {
        TODO("deleteByLibrary")  // TODO: Implement
    }

    private fun ResultRow.toGame(genres: Map<Id<Game>, List<Genre>>): Game {
        val id = this[Games.id].toId<Game>()
        return toGame(genres[id] ?: emptyList())
    }

    private fun ResultRow.toGame(genres: List<Genre>): Game {
        val library = this.toLibrary()
        return Game(
            id = this[Games.id].toId(),
            path = this[Games.path].toPath(),
            lastModified = this[Games.lastModified],
            library = library,
            data = GameData(
                basicData = this.toGameBasicData(),
                scoreData = this.toGameScoreData(),
                imageData = this.toGameImageData(),
                genres = genres,
                providerData = objectMapper.readList(this[Games.providerData])
            )
        )
    }

    private fun ResultRow.toGameBasicData(): GameBasicData = GameBasicData(
        name = this[Games.name],
        description = this[Games.description],
        releaseDate = this[Games.releaseDate]?.toLocalDate()
    )

    private fun ResultRow.toGameScoreData(): GameScoreData = GameScoreData(
        criticScore = this[Games.criticScore]?.toDouble(),
        userScore = this[Games.userScore]?.toDouble()
    )

    private fun ResultRow.toGameImageData(): GameImageData = GameImageData(
        thumbnail = toImageData(Images.thumbnail, Images.thumbnailUrl),
        poster = toImageData(Images.poster, Images.posterUrl)
    )

    private fun ResultRow.toImageData(dataColumn: Column<Blob?>, urlColumn: Column<String?>): ImageData = ImageData(
        rawData = this[dataColumn]?.bytes,
        url = this[urlColumn]
    )
}