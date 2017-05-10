package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.ProviderData
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.repository.GameRepository
import com.gitlab.ykrasik.gamedex.repository.LibraryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.experimental.CoroutineContext

/**
 * User: ykrasik
 * Date: 02/05/2017
 * Time: 13:03
 */
@Singleton
class GameTasks @Inject constructor(
    private val libraryScanner: LibraryScanner,
    private val gameRepository: GameRepository,
    private val libraryRepository: LibraryRepository,
    private val providerRepository: GameProviderRepository,
    private val providerService: GameProviderService     // TODO: This class appears in too many places.
) {
    inner class RefreshGamesTask : GamedexTask("Refreshing games...") {
        private var numNewGames = 0

        override suspend fun doRun(context: CoroutineContext) {
            val requestsJob = libraryScanner.scan(context, libraryRepository.libraries, gameRepository.games, progress)
            for (addGameRequest in requestsJob.channel) {
                gameRepository.add(addGameRequest)
                numNewGames += 1
            }
        }

        override fun finally() {
            progress.message = "Done refreshing games: Added $numNewGames new games."
        }
    }

    inner class RefetchAllGamesTask : GamedexTask("Re-fetching all games...") {
        private var numRefetched = 0

        override suspend fun doRun(context: CoroutineContext) {
            // Operate on a copy of the games to avoid concurrent modifications
            gameRepository.games.sortedBy { it.name }.forEachIndexed { i, game ->
                progress.progress(i, gameRepository.games.size - 1)

                doRefetchGame(game, progress)
                numRefetched += 1
            }
        }

        override fun finally() {
            progress.message = "Done: Re-fetched $numRefetched games."
        }
    }

    inner class RefetchGameTask(private val game: Game) : GamedexTask("Re-fetching '${game.name}'...") {
        override suspend fun doRun(context: CoroutineContext) {
            doRefetchGame(game, progress)
        }

        override fun finally() {
            progress.message = "Done."
        }
    }

    private suspend fun doRefetchGame(game: Game, progress: TaskProgress) {
        val newRawGameData = providerService.fetch(game.name, game.platform, game.providerHeaders, progress)
        updateGame(game, newRawGameData)
    }

    inner class RetryAllGamesTask : GamedexTask("Re-trying all games...") {
        private var numRetried = 0
        private var numSucceeded = 0

        override suspend fun doRun(context: CoroutineContext) {
            // Operate on a copy of the games to avoid concurrent modifications
            val gamesToRetry = gameRepository.games.filter { it.rawGame.providerData.size < providerRepository.providers.size }
            gamesToRetry.sortedBy { it.name }.forEachIndexed { i, game ->
                progress.progress(i, gamesToRetry.size - 1)

                if (doSearchAgain(game, progress)) {
                    numSucceeded += 1
                }
                numRetried += 1
            }
        }

        override fun finally() {
            progress.message = "Done: Successfully retried $numSucceeded/$numRetried games."
        }
    }

    inner class CleanupTask : GamedexTask("Cleaning up...") {
        private var staleGames = 0
        private var staleLibraries = 0

        suspend override fun doRun(context: CoroutineContext) {
            cleanupStaleGames()
            cleanupStaleLibraries()
        }

        private suspend fun cleanupStaleGames() {
            progress.message = "Detecting stales games..."
            val staleGamesDetected = gameRepository.games.filterIndexed { i, game ->
                progress.progress(i, gameRepository.games.size - 1)
                !game.path.isDirectory
            }
            progress.message = "Detected ${staleGamesDetected.size} stales games."

            staleGamesDetected.forEachIndexed { i, game ->
                progress.message = "Cleaning up stale game: '${game.name}'..."
                progress.progress(i, gameRepository.games.size - 1)

                gameRepository.delete(game)
                staleGames += 1
            }
        }

        private suspend fun cleanupStaleLibraries() {
            progress.message = "Detecting stales libraries..."
            val staleLibrariesDetected = libraryRepository.libraries.filterIndexed { i, library ->
                progress.progress(i, libraryRepository.libraries.size - 1)
                !library.path.isDirectory
            }
            progress.message = "Detected ${staleLibrariesDetected.size} stales libraries."

            staleLibrariesDetected.forEachIndexed { i, library ->
                progress.message = "Deleting stale library: '${library.name}'..."
                progress.progress(i, libraryRepository.libraries.size - 1)

                libraryRepository.delete(library)
                gameRepository.invalidate()  // TODO: This logic is duplicated from LibraryController.
                staleLibraries += 1
            }
        }

        override fun finally() {
            progress.message = "Done cleaning up: Removed $staleGames stale games and $staleLibraries stale libraries."
        }
    }

    inner class SearchAgainTask(private val game: Game) : GamedexTask("Searching '${game.name}'...") {
        suspend override fun doRun(context: CoroutineContext) {
            doSearchAgain(game, progress)
        }

        override fun finally() {
            progress.message = "Done."
        }
    }

    private suspend fun doSearchAgain(game: Game, progress: TaskProgress): Boolean {
        val newRawGameData = providerService.search(game.name, game.platform, game.path, progress, isSearchAgain = true) ?: return false
        updateGame(game, newRawGameData)
        return true
    }

    private suspend fun updateGame(game: Game, providerData: List<ProviderData>) {
        val newRawGame = game.rawGame.copy(providerData = providerData)
        gameRepository.update(newRawGame)
    }
}