package com.gitlab.ykrasik.gamedex.repository

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.core.GameFactory
import com.gitlab.ykrasik.gamedex.core.NotificationManager
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.preferences.UserPreferences
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.run
import org.joda.time.DateTime
import tornadofx.observable
import tornadofx.onChange
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
    val games: ObservableList<Game> = run {
        notificationManager.message("Fetching games...")
        persistenceService.fetchAllGames().map { it.toGame() }.observable()
    }

    // TODO: Genres need to constantly flatMap from games, not just once.
//    val genresProperty: ReadOnlyListProperty<String> = SimpleListProperty(this.games.flatMapTo(mutableSetOf<String>(), Game::genres).toList().observable())
//    val genres: ObservableList<String> by genresProperty

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
        val rawGame = persistenceService.insertGame(request.metaData.updatedNow(), request.rawGameData)
        val game = rawGame.toGame()
        run(JavaFx) {
            games += game
        }
        game
    }

    suspend fun update(newRawGame: RawGame) = run(JavaFx) {
        run(CommonPool) {
            persistenceService.updateGame(newRawGame.copy(metaData = newRawGame.metaData.updatedNow()))
        }
        removeGameById(newRawGame.id)
        games += newRawGame.toGame()
    }

    suspend fun delete(game: Game) = run(JavaFx) {
        notificationManager.message("Deleting '${game.name}'...")
        run(CommonPool) {
            persistenceService.deleteGame(game.id)
        }
        removeGameById(game.id)
        notificationManager.message("Deleted '${game.name}'.")
    }

    suspend fun deleteByLibrary(library: Library) = run(JavaFx) {
        // TODO: Instead of filtering in place, try re-setting to a filtered list (performance)
        this.games.removeAll { it.libraryId == library.id }
    }

    private fun rebuildGames() {
        this.games.setAll(this.games.map { it.rawGame.toGame() }.observable())
    }

    fun gamesForLibrary(library: Library): FilteredList<Game> = this.games.filtered { it.libraryId == library.id }

    private fun RawGame.toGame(): Game = gameFactory.create(this)

    private fun removeGameById(id: Int) {
        check(games.removeIf { it.id == id }) { "Error! Doesn't exist: Game($id)" }
    }

    private fun MetaData.updatedNow(): MetaData = copy(lastModified = DateTime.now())
}

data class AddGameRequest(
    val metaData: MetaData,
    val rawGameData: List<RawGameData>
)