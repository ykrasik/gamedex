package com.gitlab.ykrasik.gamedex.ui.model

import com.gitlab.ykrasik.gamedex.common.datamodel.Game
import com.gitlab.ykrasik.gamedex.common.datamodel.Library
import com.gitlab.ykrasik.gamedex.common.util.logger
import com.gitlab.ykrasik.gamedex.persistence.AddGameRequest
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import javafx.beans.property.ReadOnlyListProperty
import javafx.beans.property.SimpleListProperty
import javafx.collections.ObservableList
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.run
import tornadofx.getValue
import tornadofx.observable
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
) {
    private val log by logger()

    val gamesProperty: ReadOnlyListProperty<Game> = SimpleListProperty(persistenceService.fetchAllGames().observable())
    val games: ObservableList<Game> by gamesProperty

    val genresProperty: ReadOnlyListProperty<String> = SimpleListProperty(games.flatMapTo(mutableSetOf<String>(), Game::genres).toList().observable())
    val genres: ObservableList<String> by genresProperty

    suspend fun add(request: AddGameRequest): Game {
        val game = run(CommonPool) {
            persistenceService.insert(request)
        }
        run(JavaFx) {
            games += game
        }
        return game
    }

    suspend fun delete(game: Game) {
        log.info { "Deleting $game..." }
        run(CommonPool) {
            persistenceService.deleteGame(game.id)
        }
        run(JavaFx) {
            check(games.removeIf { it.id == game.id }) { "Error! Game doesn't exist: $game" }
        }
        log.info { "Done." }
    }

    suspend fun deleteByLibrary(library: Library) {
        log.debug { "Deleting all games by library: $library" }
        val sizeBefore = games.size
        run(JavaFx) {
            games.removeAll { it.libraryId == library.id }
        }
        val removed = games.size - sizeBefore
        log.debug { "Done. Removed $removed games." }
    }
}