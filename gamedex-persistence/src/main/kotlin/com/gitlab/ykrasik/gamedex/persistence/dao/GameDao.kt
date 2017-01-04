package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.common.TimeProvider
import com.github.ykrasik.gamedex.common.jackson.objectMapper
import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.common.toPath
import com.github.ykrasik.gamedex.datamodel.Game
import com.github.ykrasik.gamedex.datamodel.GameData
import com.github.ykrasik.gamedex.datamodel.GameImageData
import com.github.ykrasik.gamedex.datamodel.Library
import com.gitlab.ykrasik.gamedex.persistence.entity.Games
import com.gitlab.ykrasik.gamedex.persistence.entity.Images
import com.gitlab.ykrasik.gamedex.persistence.entity.Libraries
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
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

    fun add(gameData: GameData, imageData: GameImageData, path: Path, library: Library): Game

    fun delete(game: Game)
    fun deleteByLibrary(library: Library)
}

@Singleton
class GameDaoImpl @Inject constructor(
    private val timeProvider: TimeProvider
) : GameDao {
    private val log by logger()

    // TODO: Why fetch the library? For the platform? that can be saved in the data.
    private val baseQuery = (Games innerJoin Libraries).slice(Games.columns + Libraries.columns)

    override val all: List<Game> get() {
        log.info { "Fetching all..." }
        val games = transaction {
            baseQuery.selectAll().map { it.toGame() }
        }
        log.info { "Result: ${games.size} games." }
        return games
    }

    override fun add(gameData: GameData, imageData: GameImageData, path: Path, library: Library): Game = transaction {
        val lastModified = timeProvider.now()
        log.info { "Inserting game: $gameData..." }
        val id = insertGame(path, lastModified, library, gameData)
        insertImage(imageData, id)
        val game = gameData.toGame(id, path, lastModified, library)
        log.info { "Result: $game." }
        game
    }

    private fun insertGame(path: Path, lastModified: DateTime, library: Library, gameData: GameData): Int = Games.insert {
        it[Games.path] = path.toString()
        it[Games.lastModified] = lastModified
        it[Games.libraryId] = library.id
        it[Games.data] = objectMapper.writeValueAsString(gameData)
    } get Games.id

    private fun insertImage(imageData: GameImageData, id: Int) = Images.insert {
        it[gameId] = id
        it[thumbnailUrl] = imageData.thumbnailUrl
        it[posterUrl] = imageData.posterUrl
        // FIXME: Add screenshots
    }

    private fun GameData.toGame(id: Int, path: Path, lastModified: DateTime, library: Library) = Game(
        id = id,
        path = path,
        lastModified = lastModified,
        library = library,
        data = this
    )

    override fun delete(game: Game) {
        log.info { "Deleting: $game..." }
        transaction {
            val amount = Games.deleteWhere { Games.id.eq(game.id) }
            require(amount == 1) { "Game doesn't exist: $game" }
        }
        log.info { "Done." }
    }

    override fun deleteByLibrary(library: Library) {
        TODO("deleteByLibrary")  // TODO: Implement
    }

    private fun ResultRow.toGame(): Game {
        val library = this.toLibrary()
        return Game(
            id = this[Games.id],
            path = this[Games.path].toPath(),
            lastModified = this[Games.lastModified],
            library = library,
            data = objectMapper.readValue(this[Games.data], GameData::class.java)
        )
    }
}