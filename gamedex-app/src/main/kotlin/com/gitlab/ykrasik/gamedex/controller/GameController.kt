package com.gitlab.ykrasik.gamedex.controller

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.ProviderPriorityOverride
import com.gitlab.ykrasik.gamedex.RawGame
import com.gitlab.ykrasik.gamedex.core.GameTasks
import com.gitlab.ykrasik.gamedex.core.SortedFilteredGames
import com.gitlab.ykrasik.gamedex.preferences.AllPreferences
import com.gitlab.ykrasik.gamedex.repository.GameRepository
import com.gitlab.ykrasik.gamedex.ui.areYouSureDialog
import com.gitlab.ykrasik.gamedex.ui.fragment.ChangeThumbnailFragment
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

    fun changeThumbnail(game: Game) = launch(JavaFx) {
        val (thumbnailOverride, newThumbnailUrl) = ChangeThumbnailFragment(game).show() ?: return@launch
        if (newThumbnailUrl != game.thumbnailUrl) {
            val newRawGame = game.rawGame.withPriorityOverride { it.copy(thumbnail = thumbnailOverride) }
            gameRepository.update(newRawGame)
        }
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

    private fun RawGame.withPriorityOverride(f: (ProviderPriorityOverride) -> ProviderPriorityOverride): RawGame = copy(
        priorityOverride = f(this.priorityOverride ?: ProviderPriorityOverride())
    )
}