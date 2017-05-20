package com.gitlab.ykrasik.gamedex.repository

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.core.GameFactory
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.settings.ProviderSettings
import com.gitlab.ykrasik.gamedex.ui.Task
import com.gitlab.ykrasik.gamedex.util.logger
import javafx.collections.ObservableList
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.run
import tornadofx.observable
import tornadofx.onChange
import java.util.concurrent.atomic.AtomicInteger
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

    suspend fun add(request: AddGameRequest): Game = run(CommonPool) {
        val rawGame = persistenceService.insertGame(request.metaData.updatedNow(), request.providerData, request.userData)
        val game = rawGame.toGame()
        run(JavaFx) {
            games += game
        }
        game
    }

    suspend fun addAll(requests: List<AddGameRequest>, progress: Task.Progress): List<Game> = run(CommonPool) {
        val added = AtomicInteger(0)

        progress.message = "Writing Games..."
        val games = requests.map { request ->
            async(CommonPool) {
                val rawGame = persistenceService.insertGame(request.metaData.updatedNow(), request.providerData, request.userData)
                progress.progress(added.incrementAndGet(), requests.size)
                rawGame.toGame()
            }
        }.map { it.await() }

        run(JavaFx) {
            progress.message = "Updating UI..."
            this.games += games
        }
        games
    }

    suspend fun update(newRawGame: RawGame): Game = run(JavaFx) {
        val updatedGame = newRawGame.updatedNow()
        run(CommonPool) {
            persistenceService.updateGame(updatedGame)
        }

        removeById(updatedGame.id)
        val game = updatedGame.toGame()
        games += game
        game
    }

    suspend fun updateAll(newRawGames: List<RawGame>, progress: Task.Progress): List<Game> = run(CommonPool) {
        val updated = AtomicInteger(0)

        progress.message = "Writing DB..."
        val games = newRawGames.map { newRawGame ->
            async(CommonPool) {
                val updatedGame = newRawGame.updatedNow()
                persistenceService.updateGame(updatedGame)
                progress.progress(updated.incrementAndGet(), newRawGames.size)
                updatedGame
            }
        }.map { it.await().toGame() }

        run(JavaFx) {
            progress.message = "Updating UI..."
            this.games.removeIf { game -> newRawGames.any { it.id == game.id} }
            this.games += games
        }
        games
    }

    suspend fun delete(game: Game) = run(JavaFx) {
        log.info("Deleting '${game.name}'...")
        run(CommonPool) {
            persistenceService.deleteGame(game.id)
        }
        removeById(game.id)
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

    private fun removeById(id: Int) {
        check(games.removeIf { it.id == id }) { "Error! Doesn't exist: Game($id)" }
    }
}

data class AddGameRequest(
    val metaData: MetaData,
    val providerData: List<ProviderData>,
    val userData: UserData?
)