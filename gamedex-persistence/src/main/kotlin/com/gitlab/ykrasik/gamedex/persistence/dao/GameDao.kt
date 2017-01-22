package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.common.TimeProvider
import com.github.ykrasik.gamedex.common.jackson.fromJson
import com.github.ykrasik.gamedex.common.jackson.toJsonStr
import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.common.toFile
import com.github.ykrasik.gamedex.datamodel.Game
import com.github.ykrasik.gamedex.datamodel.GameImageData
import com.gitlab.ykrasik.gamedex.persistence.AddGameRequest
import com.gitlab.ykrasik.gamedex.persistence.entity.Games
import com.gitlab.ykrasik.gamedex.persistence.entity.Images
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

/**
 * User: ykrasik
 * Date: 02/10/2016
 * Time: 15:06
 */
interface GameDao {
    val all: List<Game>

    fun add(request: AddGameRequest): Game

    fun delete(game: Game)
    fun deleteByLibrary(libraryId: Int)
}

class GameDaoImpl(private val timeProvider: TimeProvider) : GameDao {
    private val log by logger()

    override val all: List<Game> get() {
        log.info { "Fetching all..." }
        val games = transaction {
            Games.selectAll().map {
                Game(
                    id = it[Games.id],
                    path = it[Games.path].toFile(),
                    lastModified = it[Games.lastModified],
                    libraryId = it[Games.libraryId],
                    data = it[Games.data].fromJson()
                )
            }
        }
        log.info { "Result: ${games.size} games." }
        return games
    }

    override fun add(request: AddGameRequest): Game = transaction {
        log.info { "Inserting: $request..." }

        val lastModified = timeProvider.now()
        val id = insertGame(request, lastModified)
        insertImage(id, request.imageData)

        val game = Game(
            id = id,
            path = request.path,
            lastModified = lastModified,
            libraryId = request.libraryId,
            data = request.gameData
        )

        log.info { "Result: $game." }
        game
    }

    private fun insertGame(request: AddGameRequest, lastModified: DateTime): Int = Games.insert {
        it[Games.libraryId] = request.libraryId
        it[Games.path] = request.path.path
        it[Games.lastModified] = lastModified
        it[Games.data] = request.gameData.toJsonStr()
    } get Games.id

    private fun insertImage(id: Int, imageData: GameImageData) = Images.insert {
        it[gameId] = id
        it[thumbnailUrl] = imageData.thumbnailUrl
        it[posterUrl] = imageData.posterUrl
        // FIXME: Add screenshots
    }

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
}