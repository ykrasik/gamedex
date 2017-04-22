package com.gitlab.ykrasik.gamedex.ui.model

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.MetaData
import com.gitlab.ykrasik.gamedex.ProviderFetchResult
import com.gitlab.ykrasik.gamedex.core.GameFactory
import com.gitlab.ykrasik.gamedex.core.NotificationManager
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.util.UserPreferences
import javafx.beans.property.ReadOnlyListProperty
import javafx.beans.property.SimpleListProperty
import javafx.collections.ObservableList
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
    private val userPreferences: UserPreferences,
    private val notificationManager: NotificationManager
) {
    private val _gamesProperty = run {
        notificationManager.message("Fetching games...")
        val games = fetchAllGames()
        SimpleListProperty(games.observable())
    }
    private var _games: ObservableList<Game> by _gamesProperty

    val gamesProperty: ReadOnlyListProperty<Game> = _gamesProperty
    val games: ObservableList<Game> = _games

    val genresProperty: ReadOnlyListProperty<String> = SimpleListProperty(_games.flatMapTo(mutableSetOf<String>(), Game::genres).toList().observable())
    val genres: ObservableList<String> by genresProperty

    init {
        userPreferences.providerNamePriorityProperty.onChange { _games = fetchAllGames() }
        userPreferences.providerDescriptionPriorityProperty.onChange { _games = fetchAllGames() }
        userPreferences.providerReleaseDatePriorityProperty.onChange { _games = fetchAllGames() }
        userPreferences.providerCriticScorePriorityProperty.onChange { _games = fetchAllGames() }
        userPreferences.providerUserScorePriorityProperty.onChange { _games = fetchAllGames() }
        userPreferences.providerThumbnailPriorityProperty.onChange { _games = fetchAllGames() }
        userPreferences.providerPosterPriorityProperty.onChange { _games = fetchAllGames() }
        userPreferences.providerScreenshotPriorityProperty.onChange { _games = fetchAllGames() }
    }

    suspend fun add(request: AddGameRequest): Game = run(CommonPool) {
        val rawGame = persistenceService.insertGame(request.metaData, request.providerData)
        val game = gameFactory.create(rawGame)
        run(JavaFx) {
            _games.add(game)
        }
        game
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

    private fun fetchAllGames(): ObservableList<Game> {
        val rawGames = persistenceService.fetchAllGames()
        return rawGames.map(gameFactory::create).observable()
    }
}

data class AddGameRequest(
    val metaData: MetaData,
    val providerData: List<ProviderFetchResult>
)