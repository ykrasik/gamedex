package com.gitlab.ykrasik.gamedex.repository

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.core.GameFactory
import com.gitlab.ykrasik.gamedex.core.TaskProgress
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.settings.ProviderSettings
import com.gitlab.ykrasik.gamedex.util.logger
import javafx.collections.ObservableList
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
    settings: ProviderSettings
) {
    private val log = logger()

    val games: ObservableList<Game> = fetchAllGames()

    init {
        // TODO: Find a more intelligent way
        settings.nameOrderProperty.onChange { rebuildGames() }
        settings.descriptionOrderProperty.onChange { rebuildGames() }
        settings.releaseDateOrderProperty.onChange { rebuildGames() }
        settings.criticScoreOrderProperty.onChange { rebuildGames() }
        settings.userScoreOrderProperty.onChange { rebuildGames() }
        settings.thumbnailOrderProperty.onChange { rebuildGames() }
        settings.posterOrderProperty.onChange { rebuildGames() }
        settings.screenshotOrderProperty.onChange { rebuildGames() }
    }

    private fun fetchAllGames(): ObservableList<Game> {
        log.info("Fetching games...")
        val games = persistenceService.fetchAllGames().map { it.toGame() }
        log.info("Fetched ${games.size} games.")
        return games.observable()
    }

    suspend fun add(request: AddGameRequest) = run(CommonPool) {
        val rawGame = persistenceService.insertGame(request.metaData.updatedNow(), request.providerData, request.userData)
        val game = rawGame.toGame()
        run(JavaFx) {
            games += game
        }
        game
    }

    suspend fun addAll(requests: List<AddGameRequest>, progress: TaskProgress) = run(CommonPool) {
        val games = requests.mapIndexed { i, request ->
            progress.progress(i, requests.size - 1)
            progress.message = "Writing '${request.metaData.path.name}..."
            val rawGame = persistenceService.insertGame(request.metaData.updatedNow(), request.providerData, request.userData)
            rawGame.toGame()
        }
        run(JavaFx) {
            progress.message = "Updating UI..."
            this.games += games
        }
        games
    }

    suspend fun update(newRawGame: RawGame): Game = run(JavaFx) {
        val updatedGame = newRawGame.copy(metaData = newRawGame.metaData.updatedNow())
        run(CommonPool) {
            persistenceService.updateGame(updatedGame)
        }
        removeGameById(updatedGame.id)
        val game = updatedGame.toGame()
        games += game
        game
    }

    suspend fun delete(game: Game) = run(JavaFx) {
        log.info("Deleting '${game.name}'...")
        run(CommonPool) {
            persistenceService.deleteGame(game.id)
        }
        removeGameById(game.id)
        log.info("Deleting '${game.name}': Done.")
    }

    suspend fun invalidate() = run(JavaFx) {
        // Re-fetch all games from persistence
        games.setAll(fetchAllGames())
    }

    private fun rebuildGames() {
        this.games.setAll(this.games.map { it.rawGame.toGame() }.observable())
    }

    private fun RawGame.toGame(): Game = gameFactory.create(this)

    private fun removeGameById(id: Int) {
        check(games.removeIf { it.id == id }) { "Error! Doesn't exist: Game($id)" }
    }

    private fun MetaData.updatedNow(): MetaData = copy(lastModified = DateTime.now())
}

data class AddGameRequest(
    val metaData: MetaData,
    val providerData: List<ProviderData>,
    val userData: UserData?
)