package com.gitlab.ykrasik.gamedex.controller

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.core.GameTasks
import com.gitlab.ykrasik.gamedex.core.SortedFilteredGames
import com.gitlab.ykrasik.gamedex.repository.GameRepository
import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.ui.areYouSureDialog
import com.gitlab.ykrasik.gamedex.ui.distincted
import com.gitlab.ykrasik.gamedex.ui.flatMapped
import com.gitlab.ykrasik.gamedex.ui.fragment.EditGameDataFragment
import com.gitlab.ykrasik.gamedex.ui.fragment.GameDetailsFragment
import com.gitlab.ykrasik.gamedex.ui.fragment.TagFragment
import com.gitlab.ykrasik.gamedex.ui.widgets.Notification
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
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
    settings: GameSettings
) : Controller() {

    val sortedFilteredGames = SortedFilteredGames(gameRepository.games)
    val genres = gameRepository.games.flatMapped(Game::genres).distincted()
    val tags = gameRepository.games.flatMapped(Game::tags).distincted()

    init {
        sortedFilteredGames.platformFilterProperty.bindBidirectional(settings.platformProperty)
        sortedFilteredGames.sortProperty.bindBidirectional(settings.sortProperty)
        sortedFilteredGames.sortOrderProperty.bindBidirectional(settings.sortOrderProperty)
    }

    fun viewDetails(game: Game) = GameDetailsFragment(game).show()
    fun editDetails(game: Game, initialTab: GameDataType = GameDataType.name_): Deferred<Game> = async(JavaFx) {
        val choice = EditGameDataFragment(game, initialTab).show()
        val overrides = when (choice) {
            is EditGameDataFragment.Choice.Override -> choice.overrides
            is EditGameDataFragment.Choice.Clear -> emptyMap()
            is EditGameDataFragment.Choice.Cancel -> return@async game
        }

        val newRawGame = game.rawGame.withDataOverrides(overrides)
        if (newRawGame.userData != game.rawGame.userData) {
            gameRepository.update(newRawGame)
        } else {
            game
        }
    }

    fun tag(game: Game): Deferred<Game> = async(JavaFx) {
        val choice = TagFragment(game).show()
        val tags = when (choice) {
            is TagFragment.Choice.Select -> choice.tags
            is TagFragment.Choice.Cancel -> return@async game
        }

        val newRawGame = game.rawGame.withTags(tags)
        if (newRawGame.userData != game.rawGame.userData) {
            gameRepository.update(newRawGame)
        } else {
            game
        }
    }

    // TODO: Since these are called from multiple places, consider placing the ui icons in one central place for consistency.
    fun scanNewGames() = gameTasks.ScanNewGamesTask().apply { start() }
    fun cleanup() = gameTasks.CleanupTask().apply { start() }

    fun refreshAllGames() = gameTasks.RefreshAllGamesTask().apply { start() }
    fun refreshGame(game: Game) = gameTasks.RefreshGameTask(game).apply { start() }

    fun rediscoverAllGames() = gameTasks.RediscoverAllGamesTask().apply { start() }
    fun rediscoverGame(game: Game) = gameTasks.RediscoverGameTask(game).apply { start() }

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

    private fun RawGame.withDataOverrides(overrides: Map<GameDataType, GameDataOverride>): RawGame {
        // If new overrides are empty and userData is null, or userData has empty overrides -> nothing to do
        // If new overrides are not empty and userData is not null, but has the same overrides -> nothing to do
        if (overrides == userData?.overrides ?: emptyMap()) return this

        val userData = this.userData ?: UserData()
        return copy(userData = userData.copy(overrides = overrides))
    }

    private fun RawGame.withTags(tags: List<String>): RawGame {
        // If new tags are empty and userData is null, or userData has empty tags -> nothing to do
        // If new tags are not empty and userData is not null, but has the same tags -> nothing to do
        if (tags == userData?.tags ?: emptyList()) return this

        val userData = this.userData ?: UserData()
        return copy(userData = userData.copy(tags = tags))
    }
}