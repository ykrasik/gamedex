package com.gitlab.ykrasik.gamedex.controller

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.core.GameProviderService
import com.gitlab.ykrasik.gamedex.core.GameTasks
import com.gitlab.ykrasik.gamedex.core.SortedFilteredGames
import com.gitlab.ykrasik.gamedex.repository.GameRepository
import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.ui.areYouSureDialog
import com.gitlab.ykrasik.gamedex.ui.distincting
import com.gitlab.ykrasik.gamedex.ui.flatMapping
import com.gitlab.ykrasik.gamedex.ui.fragment.EditGameDataFragment
import com.gitlab.ykrasik.gamedex.ui.fragment.TagFragment
import com.gitlab.ykrasik.gamedex.ui.view.MainView
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import tornadofx.Controller
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
    private val mainView: MainView by inject()

    val sortedFilteredGames = SortedFilteredGames(gameRepository.games)
    val genres = gameRepository.games.flatMapping(Game::genres).distincting()
    val tags = gameRepository.games.flatMapping(Game::tags).distincting()

    init {
        sortedFilteredGames.platformFilterProperty.bindBidirectional(settings.platformProperty)
        sortedFilteredGames.sourceIdsPerPlatformFilterProperty.bindBidirectional(settings.sourceIdsPerPlatformProperty)
        sortedFilteredGames.sortProperty.bindBidirectional(settings.sortProperty)
        sortedFilteredGames.sortOrderProperty.bindBidirectional(settings.sortOrderProperty)
    }

    fun viewDetails(game: Game) = mainView.showGameDetails(game)
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
    fun scanNewGames(chooseResults: GameProviderService.SearchConstraints.ChooseResults) = gameTasks.ScanNewGamesTask(chooseResults).apply { start() }
    fun cleanup(): GameTasks.CleanupTask {
        // TODO: Detect stale games, confirm, then delete.
        return gameTasks.CleanupTask().apply { start() }
    }

    fun refreshAllGames() = gameTasks.RefreshAllGamesTask().apply { start() }
    fun refreshGame(game: Game) = gameTasks.RefreshGameTask(game).apply { start() }

    fun rediscoverAllGames(chooseResults: GameProviderService.SearchConstraints.ChooseResults) = gameTasks.RediscoverAllGamesTask(chooseResults).apply { start() }
    fun searchGame(game: Game) = gameTasks.SearchGameTask(game).apply { start() }

    fun delete(game: Game): Boolean {
        if (!areYouSureDialog("Delete game '${game.name}'?")) return false

        launch(JavaFx) {
            gameRepository.delete(game)
            MainView.showFlashInfoNotification("Deleted game: '${game.name}'.")
        }
        return true
    }

    private fun RawGame.withDataOverrides(overrides: Map<GameDataType, GameDataOverride>): RawGame {
        // If new overrides are empty and userData is null, or userData has empty overrides -> nothing to do
        // If new overrides are not empty and userData is not null, but has the same overrides -> nothing to do
        if (overrides == userData?.overrides ?: emptyMap<GameDataType, GameDataOverride>()) return this

        val userData = this.userData ?: UserData()
        return copy(userData = userData.copy(overrides = overrides))
    }

    private fun RawGame.withTags(tags: List<String>): RawGame {
        // If new tags are empty and userData is null, or userData has empty tags -> nothing to do
        // If new tags are not empty and userData is not null, but has the same tags -> nothing to do
        if (tags == userData?.tags ?: emptyList<String>()) return this

        val userData = this.userData ?: UserData()
        return copy(userData = userData.copy(tags = tags))
    }
}