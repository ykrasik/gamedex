package com.gitlab.ykrasik.gamedex.repository

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.core.GameFactory
import com.gitlab.ykrasik.gamedex.core.NotificationManager
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.preferences.UserPreferences
import javafx.beans.property.ReadOnlyListProperty
import javafx.beans.property.SimpleListProperty
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.run
import tornadofx.getValue
import tornadofx.observable
import tornadofx.onChange
import tornadofx.setValue
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
    userPreferences: UserPreferences,
    private val notificationManager: NotificationManager
) {
    private val _gamesProperty = run {
        notificationManager.message("Fetching games...")
        val rawGames = persistenceService.fetchAllGames()
        val games = rawGames.map { it.toGame() }.observable()
        SimpleListProperty(games.observable())
    }
    private var _games: ObservableList<Game> by _gamesProperty

    val gamesProperty: ReadOnlyListProperty<Game> = _gamesProperty
    val games: ObservableList<Game> = _games

    val genresProperty: ReadOnlyListProperty<String> = SimpleListProperty(_games.flatMapTo(mutableSetOf<String>(), Game::genres).toList().observable())
    val genres: ObservableList<String> by genresProperty

    init {
        // TODO: Find a more intelligent way
        userPreferences.providerNamePriorityProperty.onChange { rebuildGames() }
        userPreferences.providerDescriptionPriorityProperty.onChange { rebuildGames() }
        userPreferences.providerReleaseDatePriorityProperty.onChange { rebuildGames() }
        userPreferences.providerCriticScorePriorityProperty.onChange { rebuildGames() }
        userPreferences.providerUserScorePriorityProperty.onChange { rebuildGames() }
        userPreferences.providerThumbnailPriorityProperty.onChange { rebuildGames() }
        userPreferences.providerPosterPriorityProperty.onChange { rebuildGames() }
        userPreferences.providerScreenshotPriorityProperty.onChange { rebuildGames() }
    }

    suspend fun add(request: AddGameRequest): Game = run(CommonPool) {
        val rawGame = persistenceService.insertGame(request.metaData, request.rawGameData)
        val game = rawGame.toGame()
        run(JavaFx) {
            _games.add(game)
        }
        game
    }

    suspend fun update(oldGame: Game, newRawGame: RawGame) = run(JavaFx) {
        run(CommonPool) {
            persistenceService.updateGame(newRawGame)
        }
        _games.remove(oldGame)
        _games.add(newRawGame.toGame())
    }

    suspend fun delete(game: Game) = run(JavaFx) {
        notificationManager.message("Deleting '${game.name}'...")
        run(CommonPool) {
            persistenceService.deleteGame(game.id)
        }
        check(_games.removeIf { it.id == game.id }) { "Error! Game doesn't exist: $game" }
        notificationManager.message("Deleted '${game.name}'.")
    }

    suspend fun deleteByLibrary(library: Library) = run(JavaFx) {
        // TODO: Instead of filtering in place, try re-setting to a filtered list (performance)
        _games.removeAll { it.libraryId == library.id }
    }

    private fun rebuildGames() {
        _games = _games.map { it.rawGame.toGame() }.observable()
    }

    fun gamesForLibrary(library: Library): FilteredList<Game> = games.filtered { it.libraryId == library.id }

    private fun RawGame.toGame(): Game = gameFactory.create(this)
}

data class AddGameRequest(
    val metaData: MetaData,
    val rawGameData: List<RawGameData>
)