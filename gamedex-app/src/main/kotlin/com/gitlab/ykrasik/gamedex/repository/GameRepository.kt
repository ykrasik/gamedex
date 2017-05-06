package com.gitlab.ykrasik.gamedex.repository

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.core.GameFactory
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.preferences.ProviderPreferences
import com.gitlab.ykrasik.gamedex.ui.distincted
import com.gitlab.ykrasik.gamedex.ui.flatMapped
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
    preferences: ProviderPreferences
) {
    private val log = logger()

    val games: ObservableList<Game> = run {
        log.info("Fetching games...")
        val games = persistenceService.fetchAllGames().map { it.toGame() }.observable()
        log.info("Fetched ${games.size} games.")
        games
    }

    val genres = games.flatMapped { it.genres }.distincted()

    init {
        // TODO: Find a more intelligent way
        preferences.nameOrderProperty.onChange { rebuildGames() }
        preferences.descriptionOrderProperty.onChange { rebuildGames() }
        preferences.releaseDateOrderProperty.onChange { rebuildGames() }
        preferences.criticScoreOrderProperty.onChange { rebuildGames() }
        preferences.userScoreOrderProperty.onChange { rebuildGames() }
        preferences.thumbnailOrderProperty.onChange { rebuildGames() }
        preferences.posterOrderProperty.onChange { rebuildGames() }
        preferences.screenshotOrderProperty.onChange { rebuildGames() }
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
        log.info("Deleting '${game.name}'...")
        run(CommonPool) {
            persistenceService.deleteGame(game.id)
        }
        removeGameById(game.id)
        log.info("Deleting '${game.name}': Done.")
    }

    suspend fun deleteByLibrary(library: Library) = run(JavaFx) {
        // TODO: Instead of filtering in place, try re-setting to a filtered list (performance)
        this.games.removeAll { it.libraryId == library.id }
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
    val rawGameData: List<RawGameData>
)