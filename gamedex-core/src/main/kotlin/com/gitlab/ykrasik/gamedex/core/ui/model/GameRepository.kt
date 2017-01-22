package com.gitlab.ykrasik.gamedex.core.ui.model

import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.datamodel.Game
import com.github.ykrasik.gamedex.datamodel.Library
import com.gitlab.ykrasik.gamedex.persistence.AddGameRequest
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import javafx.beans.property.ReadOnlyListProperty
import javafx.beans.property.SimpleListProperty
import javafx.collections.ObservableList
import tornadofx.getValue
import tornadofx.observable
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 25/12/2016
 * Time: 19:18
 */
@Singleton
class GameRepository @Inject constructor(
    private val persistenceService: PersistenceService
) : Iterable<Game> {
    private val log by logger()

    val gamesProperty: ReadOnlyListProperty<Game> = run {
        log.info { "Fetching all games..." }
        val games = persistenceService.fetchAllGames()
        log.info { "Result: ${games.size} games." }
        SimpleListProperty(games.observable())
    }
    private val games: ObservableList<Game> by gamesProperty

    override fun iterator() = games.iterator()

    fun add(request: AddGameRequest): Game {
        log.info { "$request..." }
        val id = persistenceService.insert(request)
        val game = Game(id, request.path, request.lastModified, request.libraryId, request.gameData)
        games += game       // FIXME: Should this be runLater?
        log.info { "Result: $game." }
        return game
    }

    fun delete(game: Game) {
        log.info { "Deleting $game..." }
        persistenceService.deleteGame(game.id)
        check(games.remove(game)) { "Error! Game doesn't exist: $game" }    // FIXME: Should this be runLater?
        log.info { "Done." }
    }

    fun deleteByLibrary(library: Library) {
        log.debug { "Deleting all games by library: $library" }
        val sizeBefore = games.size
        games.removeAll { it.libraryId == library.id }  // FIXME: Should this be runLater?
        val removed = games.size - sizeBefore
        log.debug { "Done. Removed $removed games." }
    }

    fun getByLibrary(libraryId: Int): ObservableList<Game> = games.filtered { it.libraryId == libraryId }
    fun getByPath(path: File): Game? = games.find { it.path == path }
}