package com.gitlab.ykrasik.gamedex.controller

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.core.GameTasks
import com.gitlab.ykrasik.gamedex.core.SortedFilteredGames
import com.gitlab.ykrasik.gamedex.preferences.AllPreferences
import com.gitlab.ykrasik.gamedex.repository.GameRepository
import com.gitlab.ykrasik.gamedex.ui.areYouSureDialog
import com.gitlab.ykrasik.gamedex.ui.fragment.ChangeImageFragment
import com.gitlab.ykrasik.gamedex.ui.widgets.Notification
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import tornadofx.Controller
import tornadofx.seconds
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 14:39
 */
@Singleton
class GameController @Inject constructor(
    private val gameRepository: GameRepository,
    private val gameTasks: GameTasks,
    preferences: AllPreferences
) : Controller() {

    val sortedFilteredGames = SortedFilteredGames(gameRepository.games)
    val genres get() = gameRepository.genres

    init {
        sortedFilteredGames.platformFilterProperty.bindBidirectional(preferences.game.platformProperty)
        sortedFilteredGames.sortProperty.bindBidirectional(preferences.gameWall.sortProperty)
        sortedFilteredGames.sortOrderProperty.bindBidirectional(preferences.gameWall.sortOrderProperty)
    }

    fun refreshGames() = gameTasks.RefreshGamesTask().apply { start() }

    fun refetchAllGames(): GameTasks.RefetchAllGamesTask? {
        return if (areYouSureDialog("Re-fetch all games? This could take a while...")) {
            gameTasks.RefetchAllGamesTask().apply { start() }
        } else {
            null
        }
    }

    fun refetchGame(game: Game) = gameTasks.RefetchGameTask(game).apply { start() }

    fun cleanup() = gameTasks.CleanupTask().apply { start() }

    fun searchAgain(game: Game) = gameTasks.SearchAgainTask(game).apply { start() }

    fun changeThumbnail(game: Game) = changeImage(game, { g -> ChangeImageFragment.thumbnail(g) }, { o -> copy(thumbnail = o) })
    fun changePoster(game: Game) = changeImage(game, { g -> ChangeImageFragment.poster(g) }, { o -> copy(poster = o) })

    private fun changeImage(game: Game,
                            factory: (Game) -> ChangeImageFragment,
                            modifier: GameDataOverrides.(GameDataOverride?) -> GameDataOverrides) = launch(JavaFx) {
        val choice = factory(game).show()
        val override = when (choice) {
            is ChangeImageFragment.Choice.Select -> choice.override
            is ChangeImageFragment.Choice.Clear -> null
            is ChangeImageFragment.Choice.Cancel -> return@launch
        }
        val newRawGame = game.rawGame.withDataOverride { it.modifier(override) }
        gameRepository.update(newRawGame)
    }

    fun delete(game: Game): Boolean {
        if (!areYouSureDialog("Delete game '${game.name}'?")) return false

        launch(JavaFx) {
            gameRepository.delete(game)

            Notification()
                .text("Deleted game: '${game.name}")
                .information()
                .automaticallyHideAfter(2.seconds)
                .show()
        }
        return true
    }

    private fun RawGame.withDataOverride(f: (GameDataOverrides) -> GameDataOverrides): RawGame {
        val userData = this.userData ?: UserData(overrides = GameDataOverrides())
        return copy(userData = userData.copy(overrides = f(userData.overrides)))
    }
}