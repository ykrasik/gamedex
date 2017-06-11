package com.gitlab.ykrasik.gamedex.task

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.ProviderData
import com.gitlab.ykrasik.gamedex.ProviderHeader
import com.gitlab.ykrasik.gamedex.core.GameProviderService
import com.gitlab.ykrasik.gamedex.repository.GameRepository
import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.ui.Task
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 04/06/2017
 * Time: 09:43
 */
// TODO: Tasks should contain minimal logic and act as glue between the logic & display
@Singleton
class RefreshTasks @Inject constructor(
    private val gameRepository: GameRepository,
    private val providerService: GameProviderService,
    private val settings: GameSettings
) {
    // TODO: Consider renaming 'refresh' to 'redownload'
    // TODO: Allow refreshing with a user-specified excluded provider.
    inner class RefreshGamesTask(private val games: List<Game>) : Task<Unit>("Refreshing ${games.size} games...") {
        private var numRefreshed = 0

        override suspend fun doRun() {
            var remaining = games.size

            // Operate on a copy of the games to avoid concurrent modifications
            games.sortedBy { it.name }.forEachIndexed { i, game ->
                progress.progress(i, games.size - 1)
                titleProperty.set("Refreshing $remaining games...")

                val providersToDownload = game.providerHeaders.filter { header ->
                    header.updateDate.plus(settings.stalePeriod).isBeforeNow
                }
                if (providersToDownload.isNotEmpty()) {
                    doRefreshGame(game, providersToDownload)
                    numRefreshed += 1
                }
                remaining -= 1
            }
        }

        override fun doneMessage() = "Done: Refreshed $numRefreshed games."
    }

    inner class RefreshGameTask(private val game: Game) : Task<Game>("Refreshing '${game.name}'...") {
        override suspend fun doRun() = doRefreshGame(game)
        override fun doneMessage() = "Done refreshing: '${game.name}'."
    }

    private suspend fun Task<*>.doRefreshGame(game: Game, providersToDownload: List<ProviderHeader> = game.providerHeaders): Game {
        val taskData = GameProviderService.ProviderTaskData(this, game.name, game.platform, game.path)
        val downloadedProviderData = providerService.download(taskData, providersToDownload)
        val newProviderData = if (providersToDownload == game.providerHeaders) {
            downloadedProviderData
        } else {
            game.rawGame.providerData.filterNot { d -> providersToDownload.any { it.id == d.header.id } } + downloadedProviderData
        }
        return updateGame(game, newProviderData)
    }

    private suspend fun updateGame(game: Game, newProviderData: List<ProviderData>): Game {
        val newRawGame = game.rawGame.copy(providerData = newProviderData)
        return gameRepository.update(newRawGame)
    }
}