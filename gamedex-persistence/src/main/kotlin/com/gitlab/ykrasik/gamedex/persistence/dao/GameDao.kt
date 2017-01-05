package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.common.TimeProvider
import com.github.ykrasik.gamedex.common.jackson.objectMapper
import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.common.toFile
import com.github.ykrasik.gamedex.datamodel.Game
import com.github.ykrasik.gamedex.datamodel.GameData
import com.github.ykrasik.gamedex.datamodel.GameImageData
import com.gitlab.ykrasik.gamedex.persistence.entity.Games
import com.gitlab.ykrasik.gamedex.persistence.entity.Images
import com.gitlab.ykrasik.gamedex.persistence.entity.Libraries
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.io.File

/**
 * User: ykrasik
 * Date: 02/10/2016
 * Time: 15:06
 */
interface GameDao {
    val all: List<Game>

    fun add(gameData: GameData, imageData: GameImageData, path: File, libraryId: Int): Game

    fun delete(game: Game)
    fun deleteByLibrary(libraryId: Int)
}

class GameDaoImpl(private val timeProvider: TimeProvider) : GameDao {
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

    override fun add(gameData: GameData, imageData: GameImageData, path: File, libraryId: Int): Game = transaction {
        val lastModified = timeProvider.now()
        log.info { "Inserting game: $gameData..." }
        val id = insertGame(path, lastModified, libraryId, gameData)
        insertImage(imageData, id)
        val game = gameData.toGame(id, path, lastModified, libraryId)
        log.info { "Result: $game." }
        game
    }

    private fun insertGame(path: File, lastModified: DateTime, libraryId: Int, gameData: GameData): Int = Games.insert {
        it[Games.path] = path.path
        it[Games.lastModified] = lastModified
        it[Games.libraryId] = libraryId
        it[Games.data] = objectMapper.writeValueAsString(gameData)
    } get Games.id

    private fun insertImage(imageData: GameImageData, id: Int) = Images.insert {
        it[gameId] = id
        it[thumbnailUrl] = imageData.thumbnailUrl
        it[posterUrl] = imageData.posterUrl
        // FIXME: Add screenshots
    }

    private fun GameData.toGame(id: Int, path: File, lastModified: DateTime, libraryId: Int) = Game(
        id = id,
        path = path,
        lastModified = lastModified,
        libraryId = libraryId,
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

    override fun deleteByLibrary(libraryId: Int) {
        TODO("deleteByLibrary")  // TODO: Implement
    }

    private fun ResultRow.toGame(): Game {
        return Game(
            id = this[Games.id],
            path = this[Games.path].toFile(),
            lastModified = this[Games.lastModified],
            libraryId = this[Games.libraryId],
            data = objectMapper.readValue(this[Games.data], GameData::class.java)
        )
    }
}