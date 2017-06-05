package com.gitlab.ykrasik.gamedex.task

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.ProviderData
import com.gitlab.ykrasik.gamedex.ProviderId
import com.gitlab.ykrasik.gamedex.UserData
import com.gitlab.ykrasik.gamedex.core.GameProviderService
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.repository.GameRepository
import com.gitlab.ykrasik.gamedex.ui.Task
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 04/06/2017
 * Time: 09:30
 */
// TODO: Tasks should contain minimal logic and act as glue between the logic & display
@Singleton
class SearchTasks @Inject constructor(
    private val gameRepository: GameRepository,
    private val providerRepository: GameProviderRepository,
    private val providerService: GameProviderService
) {
    // TODO: This only rediscovers non-excluded providers - find a way to add to name
    // TODO: Merge with RediscoverGamesTask
    inner class RediscoverAllGamesTask : Task<Unit>("Rediscovering all games...") {
        private var numUpdated = 0

        override suspend fun doRun() {
            val gamesWithMissingProviders = gameRepository.games.filter { it.hasMissingProviders }
            doRediscover(gamesWithMissingProviders) { numUpdated += 1 }
        }

        // TODO: Can consider checking if the missing providers support the game's platform, to avoid an empty call.
        private val Game.hasMissingProviders: Boolean
            get() = rawGame.providerData.size + excludedProviders.size < providerRepository.providers.size

        override fun doneMessage() = "Done: Updated $numUpdated games."
    }

    inner class RediscoverGamesTask(private val games: List<Game>) : Task<Unit>("Rediscovering ${games.size} games...") {
        private var numUpdated = 0
        override suspend fun doRun() = doRediscover(games) { numUpdated += 1 }
        override fun doneMessage() = "Done: Updated $numUpdated games."
    }

    inner class SearchGameTask(private val game: Game) : Task<Game>("Searching '${game.name}'...") {
        override suspend fun doRun() = doSearchAgain(game, excludedProviders = emptyList()) ?: game
        override fun doneMessage() = "Done searching: '${game.name}'."
    }

    private suspend fun Task<*>.doRediscover(games: List<Game>,
                                             onSuccess: (Game) -> Unit) {
        // Operate on a copy of the games to avoid concurrent modifications
        games.sortedBy { it.name }.forEachIndexed { i, game ->
            if (!isActive) return@forEachIndexed
            progress.progress(i, games.size - 1)

            val excludedProviders = game.existingProviders + game.excludedProviders
            if (doSearchAgain(game, excludedProviders) != null) {
                onSuccess(game)
            }
        }
    }

    private val Game.existingProviders get() = rawGame.providerData.map { it.header.id }
    private val Game.excludedProviders get() = userData?.excludedProviders ?: emptyList()

    private suspend fun Task<*>.doSearchAgain(game: Game, excludedProviders: List<ProviderId>): Game? {
        val taskData = GameProviderService.ProviderTaskData(this, game.name, game.platform, game.path)
        val results = providerService.search(taskData, excludedProviders) ?: return null
        if (results.isEmpty()) return null

        val newProviderData = if (excludedProviders.isEmpty()) {
            results.providerData
        } else {
            game.rawGame.providerData + results.providerData
        }

        val newUserData = if (results.excludedProviders.isEmpty()) {
            game.userData
        } else {
            game.userData.merge(UserData(excludedProviders = results.excludedProviders))
        }

        return updateGame(game, newProviderData, newUserData)
    }

    private fun UserData?.merge(userData: UserData?): UserData? {
        if (userData == null) return this
        if (this == null) return userData
        return this.merge(userData)
    }

    private suspend fun updateGame(game: Game, newProviderData: List<ProviderData>, newUserData: UserData?): Game {
        val newRawGame = game.rawGame.copy(providerData = newProviderData, userData = newUserData)
        return gameRepository.update(newRawGame)
    }
}