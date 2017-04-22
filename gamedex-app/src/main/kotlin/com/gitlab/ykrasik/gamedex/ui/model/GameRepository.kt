package com.gitlab.ykrasik.gamedex.ui.model

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.MetaData
import com.gitlab.ykrasik.gamedex.ProviderFetchResult
import com.gitlab.ykrasik.gamedex.core.GameFactory
import com.gitlab.ykrasik.gamedex.core.NotificationManager
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
    private val persistenceService: PersistenceService,
    private val gameFactory: GameFactory,
    private val notificationManager: NotificationManager
) {
    val gamesProperty: ReadOnlyListProperty<Game> = run {
        notificationManager.message("Fetching games...")
        val rawGames = persistenceService.fetchAllGames()
        val games = rawGames.map(gameFactory::create)
        SimpleListProperty(games.observable())
    }
    val games: ObservableList<Game> by gamesProperty

    val genresProperty: ReadOnlyListProperty<String> = SimpleListProperty(games.flatMapTo(mutableSetOf<String>(), Game::genres).toList().observable())
    val genres: ObservableList<String> by genresProperty

    suspend fun add(request: AddGameRequest): Game = run(CommonPool) {
        val rawGame = persistenceService.insertGame(request.metaData, request.providerData)
        val game = gameFactory.create(rawGame)
        run(JavaFx) {
            games += game
        }
        game
    }

    suspend fun delete(game: Game) = run(JavaFx) {
        notificationManager.message("Deleting '${game.name}'...")
        run(CommonPool) {
            persistenceService.deleteGame(game.id)
        }
        check(games.removeIf { it.id == game.id }) { "Error! Game doesn't exist: $game" }
        notificationManager.message("Deleted '${game.name}'.")
    }

    suspend fun deleteByLibrary(library: Library) = run(JavaFx) {
        // TODO: Instead of filtering in place, try re-setting to a filtered list (performance)
        games.removeAll { it.libraryId == library.id }
    }
}

data class AddGameRequest(
    val metaData: MetaData,
    val providerData: List<ProviderFetchResult>
)