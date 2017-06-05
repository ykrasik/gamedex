package com.gitlab.ykrasik.gamedex.task

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.core.GameProviderService
import com.gitlab.ykrasik.gamedex.core.NewDirectoryDetector
import com.gitlab.ykrasik.gamedex.repository.AddGameRequest
import com.gitlab.ykrasik.gamedex.repository.GameRepository
import com.gitlab.ykrasik.gamedex.repository.LibraryRepository
import com.gitlab.ykrasik.gamedex.ui.Task
import com.gitlab.ykrasik.gamedex.util.now
import kotlinx.coroutines.experimental.channels.produce
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 02/05/2017
 * Time: 13:03
 */
// TODO: Tasks should contain minimal logic and act as glue between the logic & display
@Singleton
class GameTasks @Inject constructor(
    private val newDirectoryDetector: NewDirectoryDetector,
    private val gameRepository: GameRepository,
    private val libraryRepository: LibraryRepository,
    private val providerService: GameProviderService
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
            val taskData = GameProviderService.ProviderTaskData(this, directory.name, library.platform, directory)
            val results = providerService.search(taskData, excludedProviders = emptyList()) ?: return null
            val relativePath = library.path.toPath().relativize(directory.toPath()).toFile()
            val metaData = MetaData(library.id, relativePath, updateDate = now)
            val userData = if (results.excludedProviders.isNotEmpty()) {
                UserData(excludedProviders = results.excludedProviders)
            } else {
                null
            }
            return AddGameRequest(
                metaData = metaData,
                providerData = results.providerData,
                userData = userData
            )
        }

        override fun doneMessage() = "Done: Added $numNewGames new games."
    }

    inner class CleanupTask : Task<Unit>("Cleaning up...") {
        private var staleGames = 0
        private var staleLibraries = 0

        override suspend fun doRun() {
            // TODO: First detect stale, then confirm, then delete.
            // TODO: Create backup before deleting
            staleGames = cleanupStaleGames()
            staleLibraries = cleanupStaleLibraries()
        }

        private suspend fun cleanupStaleGames(): Int {
            progress.message = "Detecting stales games..."
            val staleGames = gameRepository.games.filterIndexed { i, game ->
                (!game.path.isDirectory).apply {
                    progress.progress(i, gameRepository.games.size)
                }
            }

            progress.message = "Cleaning up ${staleGames.size} stales games..."
            gameRepository.deleteAll(staleGames, progress)

            return staleGames.size
        }

        private suspend fun cleanupStaleLibraries(): Int {
            progress.message = "Detecting stales libraries..."
            val staleLibraries = libraryRepository.libraries.filterIndexed { i, library ->
                (!library.path.isDirectory).apply {
                    progress.progress(i, libraryRepository.libraries.size)
                }
            }

            progress.message = "Cleaning up ${staleLibraries.size} stales libraries..."
            libraryRepository.deleteAll(staleLibraries, progress)
            return staleLibraries.size
        }

        override fun doneMessage() = "Done cleaning up: Removed $staleGames stale games and $staleLibraries stale libraries."
    }

    // TODO: Finish this.
    inner class DetectDuplicateGames : Task<List<Game>>("Detecting duplicate games...") {
        private var duplicates = 0

        override suspend fun doRun() = gameRepository.games.let { games ->
            games.filterIndexed { i, game ->
                (games.any { it != game && it.name == game.name }).apply {
                    if (this) duplicates += 1
                    progress.progress(i, games.size)
                }
            }
        }

        override fun doneMessage() = "Detected $duplicates duplicate games."
    }
}