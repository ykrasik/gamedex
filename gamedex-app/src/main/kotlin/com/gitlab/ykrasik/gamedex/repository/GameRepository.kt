package com.gitlab.ykrasik.gamedex.repository

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.core.GameFactory
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.ui.Task
import com.gitlab.ykrasik.gamedex.util.logger
import com.gitlab.ykrasik.gamedex.util.replaceFirst
import javafx.collections.ObservableList
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
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
    providerRepository: GameProviderRepository
) {
    private val log = logger()

    val games: ObservableList<Game> = fetchGames()

    init {
        providerRepository.enabledProviders.onChange {
            launch(CommonPool) {
                softInvalidate()
            }
        }
    }

    private fun fetchGames(): ObservableList<Game> {
        log.info("Fetching games...")
        val games = persistenceService.fetchGames().map { it.toGame() }
        log.info("Fetched ${games.size} games.")
        return games.observable()
    }

    suspend fun add(request: AddGameRequest): Game = withContext(CommonPool) {
        val rawGame = persistenceService.insertGame(request.metadata, request.providerData, request.userData)
        val game = rawGame.toGame()
        withContext(JavaFx) {
            games += game
        }
        game
    }

    suspend fun addAll(requests: List<AddGameRequest>, progress: Task.Progress): List<Game> = withContext(CommonPool) {
        val added = AtomicInteger(0)

        progress.message = "Writing Games..."
        val games = requests.map { request ->
            async(CommonPool) {
                val rawGame = persistenceService.insertGame(request.metadata, request.providerData, request.userData)
                progress.progress(added.incrementAndGet(), requests.size)
                rawGame.toGame()
            }
        }.map { it.await() }

        withContext(JavaFx) {
            progress.message = "Updating UI..."
            this.games += games
        }
        games
    }

    suspend fun update(newRawGame: RawGame): Game = withContext(JavaFx) {
        val updatedGame = withContext(CommonPool) {
            persistenceService.updateGame(newRawGame)
        }

        val game = updatedGame.toGame()
        require(games.replaceFirst(game) { it.id == game.id }) { "Game doesn't exist: $game" }
        game
    }

    suspend fun updateAll(newRawGames: List<RawGame>, progress: Task.Progress): List<Game> = withContext(CommonPool) {
        val updated = AtomicInteger(0)

        progress.message = "Writing DB..."
        val games = newRawGames.map { newRawGame ->
            async(CommonPool) {
                val updatedGame = persistenceService.updateGame(newRawGame)
                progress.progress(updated.incrementAndGet(), newRawGames.size)
                updatedGame
            }
        }.map { it.await().toGame() }

        withContext(JavaFx) {
            progress.message = "Updating UI..."
            this.games.removeIf { game -> newRawGames.any { it.id == game.id } }
            this.games += games
        }
        games
    }

    suspend fun delete(game: Game) = withContext(JavaFx) {
        log.info("Deleting '${game.name}'...")
        withContext(CommonPool) {
            persistenceService.deleteGame(game.id)
        }
        removeById(game.id)
        log.info("Deleting '${game.name}': Done.")
    }

    suspend fun deleteAll(games: List<Game>) = withContext(CommonPool) {
        if (games.isEmpty()) return@withContext

        persistenceService.deleteGames(games.map { it.id })

        replaceGames {
            it.filterNot { game -> games.any { it.id == game.id } }
        }
    }

    suspend fun clearUserData() = withContext(CommonPool) {
        persistenceService.clearUserData()
        modifyGames {
            it.rawGame.copy(userData = null).toGame()
        }
    }

    suspend fun hardInvalidate() = replaceGames {
        // Re-fetch all games from persistence
        fetchGames()
    }

    suspend fun softInvalidate() = modifyGames {
        it.rawGame.toGame()
    }

    private suspend fun modifyGames(f: (Game) -> Game) = withContext(CommonPool) {
        val newGames = games.map(f)
        withContext(JavaFx) {
            games.setAll(newGames)
        }
    }

    private suspend fun replaceGames(f: (List<Game>) -> List<Game>) = withContext(CommonPool) {
        val newGames = f(games)
        withContext(JavaFx) {
            games.setAll(newGames)
        }
    }

    private fun RawGame.toGame(): Game = gameFactory.create(this)

    private fun removeById(id: Int) {
        check(games.removeIf { it.id == id }) { "Error! Doesn't exist: Game($id)" }
    }

    operator fun get(id: Int): Game = games.find { it.id == id } ?: throw IllegalStateException("No Game found for id: $id!")
}

data class AddGameRequest(
    val metadata: Metadata,
    val providerData: List<ProviderData>,
    val userData: UserData?
)