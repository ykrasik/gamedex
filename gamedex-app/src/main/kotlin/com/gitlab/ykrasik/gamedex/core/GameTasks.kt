package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.repository.AddGameRequest
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.repository.GameRepository
import com.gitlab.ykrasik.gamedex.repository.LibraryRepository
import com.gitlab.ykrasik.gamedex.ui.Task
import kotlinx.coroutines.experimental.channels.produce
import org.joda.time.DateTime
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 02/05/2017
 * Time: 13:03
 */
@Singleton
class GameTasks @Inject constructor(
    private val newDirectoryDetector: NewDirectoryDetector,
    private val gameRepository: GameRepository,
    private val libraryRepository: LibraryRepository,
    private val providerRepository: GameProviderRepository,
    private val providerService: GameProviderService     // TODO: This class appears in too many places.
) {
    inner class ScanNewGamesTask : Task<Unit>("Scanning new games...") {
        private var numNewGames = 0

        override suspend fun doRun() {
            val requestsJob = scan()
            for (addGameRequest in requestsJob.channel) {
                gameRepository.add(addGameRequest)
                numNewGames += 1
            }
        }

        private fun scan() = produce<AddGameRequest>(context) {
            progress.message = "Scanning for new games..."

            val libraries = libraryRepository.libraries
            val games = gameRepository.games
            val excludedDirectories = libraries.map(Library::path).toSet() + games.map(Game::path).toSet()
            val newDirectories = libraries.flatMap { library ->
                val paths = if (library.platform != Platform.excluded) {
                    log.debug("Scanning library: $library")
                    newDirectoryDetector.detectNewDirectories(library.path, excludedDirectories - library.path).apply {
                        log.debug("Found ${this.size} new games.")
                    }
                } else {
                    emptyList()
                }
                paths.map { library to it }
            }

            progress.message = "Scanning for new games: ${newDirectories.size} new games."

            newDirectories.forEachIndexed { i, (library, directory) ->
                val addGameRequest = processDirectory(directory, library)
                progress.progress(i, newDirectories.size)
                addGameRequest?.let { send(it) }
            }
            channel.close()
        }

        private suspend fun processDirectory(directory: File, library: Library): AddGameRequest? {
            val providerData = providerService.search(this, directory.name, library.platform, directory, isSearchAgain = false) ?: return null
            val relativePath = library.path.toPath().relativize(directory.toPath()).toFile()
            return AddGameRequest(
                metaData = MetaData(library.id, relativePath, lastModified = DateTime.now()),
                providerData = providerData,
                userData = null
            )
        }

        override fun doneMessage() = "Done: Added $numNewGames new games."
    }

    inner class CleanupTask : Task<Unit>("Cleaning up...") {
        private var staleGames = 0
        private var staleLibraries = 0

        suspend override fun doRun() {
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

        override fun doneMessage() = "Done cleaning up: Removed $staleGames stale games and $staleLibraries stale libraries."
    }

    // TODO: Consider renaming 'refresh' to 'redownload'
    inner class RefreshAllGamesTask : Task<Unit>("Refreshing all games...") {
        private var numRefreshed = 0

        override suspend fun doRun() {
            // Operate on a copy of the games to avoid concurrent modifications
            gameRepository.games.sortedBy { it.name }.forEachIndexed { i, game ->
                progress.progress(i, gameRepository.games.size - 1)

                doRefreshGame(game)
                numRefreshed += 1
            }
        }

        override fun doneMessage() = "Done: Refreshed $numRefreshed games."
    }

    inner class RefreshGameTask(private val game: Game) : Task<Game>("Refreshing '${game.name}'...") {
        override suspend fun doRun() = doRefreshGame(game)
        override fun doneMessage() = "Done refreshing: '${game.name}'."
    }

    private suspend fun Task<*>.doRefreshGame(game: Game): Game {
        val newRawGameData = providerService.download(this, game.name, game.platform, game.providerHeaders)
        return updateGame(game, newRawGameData)
    }

    inner class RediscoverAllGamesTask : Task<Unit>("Rediscovering all games...") {
        private var numRetried = 0
        private var numSucceeded = 0

        override suspend fun doRun() {
            // Operate on a copy of the games to avoid concurrent modifications
            val gamesToRetry = gameRepository.games.filter { it.rawGame.providerData.size < providerRepository.providers.size }
            gamesToRetry.sortedBy { it.name }.forEachIndexed { i, game ->
                progress.progress(i, gamesToRetry.size - 1)

                if (doSearch(game) != null) {
                    numSucceeded += 1
                }
                numRetried += 1
            }
        }

        override fun doneMessage() = "Done: Rediscovered $numSucceeded/$numRetried games."
    }

    inner class SearchGameTask(private val game: Game) : Task<Game>("Searching '${game.name}'...") {
        suspend override fun doRun() = doSearch(game) ?: game
        override fun doneMessage() = "Done searching: '${game.name}'."
    }

    private suspend fun Task<*>.doSearch(game: Game): Game? {
        val newRawGameData = providerService.search(this, game.name, game.platform, game.path, isSearchAgain = true) ?: return null
        return updateGame(game, newRawGameData)
    }

    private suspend fun updateGame(game: Game, providerData: List<ProviderData>): Game {
        val newRawGame = game.rawGame.copy(providerData = providerData)
        return gameRepository.update(newRawGame)
    }
}